package com.simats.travelbuddy

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class JoinRequest(
    val id: Int,
    val name: String,
    val initials: String,
    val destination: String,
    val email: String
)

class JoinRequestsActivity : AppCompatActivity() {

    private lateinit var rvJoinRequests: RecyclerView
    private lateinit var tvNoRequests: TextView
    private lateinit var adapter: JoinRequestAdapter
    private val requestsList = mutableListOf<JoinRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_join_requests)

        // Apply edge-to-edge system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        rvJoinRequests = findViewById(R.id.rvJoinRequests)
        tvNoRequests = findViewById(R.id.tvNoRequests)

        // Configure LayoutManager & Adapter
        rvJoinRequests.layoutManager = LinearLayoutManager(this)
        adapter = JoinRequestAdapter(requestsList, 
            onAccept = { request ->
                respondToRequest(request, "accepted")
            },
            onDecline = { request ->
                respondToRequest(request, "declined")
            },
            onEmailClick = { email ->
                // Direct navigation to email compose action
                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$email")
                    putExtra(Intent.EXTRA_SUBJECT, "TravelBuddy Trip Inquiry")
                }
                try {
                    startActivity(Intent.createChooser(emailIntent, "Compose Email"))
                } catch (ex: Exception) {
                    Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        rvJoinRequests.adapter = adapter

        // Fetch real-world join requests from backend
        fetchJoinRequests()
    }

    private fun fetchJoinRequests() {
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
        val activeUserName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta") ?: "Aarav Mehta"

        RetrofitClient.instance.getJoinRequests(activeUserName).enqueue(object : Callback<JoinRequestsResponse> {
            override fun onResponse(call: Call<JoinRequestsResponse>, response: Response<JoinRequestsResponse>) {
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null && responseBody.status == "success") {
                    val data = responseBody.data ?: emptyList()
                    
                    runOnUiThread {
                        requestsList.clear()
                        for (item in data) {
                            val initials = getInitials(item.requester_name)
                            requestsList.add(
                                JoinRequest(
                                    id = item.id,
                                    name = item.requester_name,
                                    initials = initials,
                                    destination = item.destination,
                                    email = item.requester_email
                                )
                            )
                        }
                        adapter.notifyDataSetChanged()
                        checkEmptyState()
                    }
                } else {
                    runOnUiThread {
                        checkEmptyState()
                    }
                }
            }

            override fun onFailure(call: Call<JoinRequestsResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@JoinRequestsActivity, "Network error fetching requests", Toast.LENGTH_SHORT).show()
                    checkEmptyState()
                }
            }
        })
    }

    private fun respondToRequest(request: JoinRequest, status: String) {
        RetrofitClient.instance.respondJoinRequest(request.id, status).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        if (status == "accepted") {
                            AlertHelper.showNotification(
                                this@JoinRequestsActivity,
                                "Join Request Accepted",
                                "You accepted ${request.name}'s request to join your trip to ${request.destination}!",
                                "accept"
                            )
                            Toast.makeText(this@JoinRequestsActivity, "Accepted ${request.name}'s request! Notifying traveler...", Toast.LENGTH_SHORT).show()
                        } else {
                            AlertHelper.showNotification(
                                this@JoinRequestsActivity,
                                "Join Request Declined",
                                "You declined ${request.name}'s request to join your trip to ${request.destination}.",
                                "decline"
                            )
                            Toast.makeText(this@JoinRequestsActivity, "Declined ${request.name}'s request.", Toast.LENGTH_SHORT).show()
                        }
                        removeRequest(request)
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@JoinRequestsActivity, "Failed to update request", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@JoinRequestsActivity, "Network error updating request", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun getInitials(name: String): String {
        val words = name.trim().split("\\s+".toRegex())
        return if (words.isNotEmpty()) {
            val first = words[0].take(1).uppercase()
            val second = if (words.size > 1) words[1].take(1).uppercase() else ""
            first + second
        } else {
            "TR"
        }
    }

    private fun removeRequest(request: JoinRequest) {
        val position = requestsList.indexOf(request)
        if (position != -1) {
            requestsList.removeAt(position)
            adapter.notifyItemRemoved(position)
            checkEmptyState()
        }
    }

    private fun checkEmptyState() {
        if (requestsList.isEmpty()) {
            tvNoRequests.visibility = View.VISIBLE
            rvJoinRequests.visibility = View.GONE
        } else {
            tvNoRequests.visibility = View.GONE
            rvJoinRequests.visibility = View.VISIBLE
        }
    }

    // Recycler list adapter definition
    class JoinRequestAdapter(
        private val list: List<JoinRequest>,
        private val onAccept: (JoinRequest) -> Unit,
        private val onDecline: (JoinRequest) -> Unit,
        private val onEmailClick: (String) -> Unit
    ) : RecyclerView.Adapter<JoinRequestAdapter.RequestViewHolder>() {

        class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvAvatarInitials: TextView = itemView.findViewById(R.id.tvAvatarInitials)
            val tvRequesterName: TextView = itemView.findViewById(R.id.tvRequesterName)
            val tvTripDestination: TextView = itemView.findViewById(R.id.tvTripDestination)
            val tvRequesterEmail: TextView = itemView.findViewById(R.id.tvRequesterEmail)
            val btnAccept: MaterialButton = itemView.findViewById(R.id.btnAccept)
            val btnDecline: MaterialButton = itemView.findViewById(R.id.btnDecline)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_join_request, parent, false)
            return RequestViewHolder(view)
        }

        override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
            val item = list[position]
            holder.tvAvatarInitials.text = item.initials
            holder.tvRequesterName.text = item.name
            holder.tvTripDestination.text = "wants to join: ${item.destination}"
            holder.tvRequesterEmail.text = item.email

            holder.btnAccept.setOnClickListener { onAccept(item) }
            holder.btnDecline.setOnClickListener { onDecline(item) }
            holder.tvRequesterEmail.setOnClickListener { onEmailClick(item.email) }
        }

        override fun getItemCount(): Int = list.size
    }
}
