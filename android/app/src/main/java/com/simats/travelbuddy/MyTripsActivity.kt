package com.simats.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class Trip(val title: String, val dates: String, val budget: String, val status: String, val imageRes: Int, val statusColor: String)

class MyTripsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_trips)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.llHeader)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        setupNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadTrips()
    }

    private fun getDestinationImage(title: String): Int {
        return when {
            title.contains("Mahabalipuram", true) -> R.drawable.img_mahabali
            title.contains("Pondicherry", true) -> R.drawable.img_pondy
            title.contains("Ooty", true) -> R.drawable.img_ooty
            title.contains("Madurai", true) -> R.drawable.img_madurai
            title.contains("New York", true) -> R.drawable.img_tokyo
            title.contains("Los Angeles", true) -> R.drawable.img_bali
            title.contains("Las Vegas", true) -> R.drawable.img_dubai
            title.contains("Taj Mahal", true) -> R.drawable.img_madurai
            title.contains("Paris", true) -> R.drawable.img_bali
            title.contains("London", true) -> R.drawable.img_iceland
            title.contains("Bali", true) -> R.drawable.img_bali
            title.contains("Tokyo", true) -> R.drawable.img_tokyo
            title.contains("Santorini", true) -> R.drawable.img_santorini
            title.contains("Dubai", true) -> R.drawable.img_dubai
            else -> R.drawable.img_mahabali // fallback to Mahabalipuram image
        }
    }

    private fun getActiveEmail(): String {
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("ACTIVE_EMAIL", "guest@travelbuddy.com") ?: "guest@travelbuddy.com"
    }

    private fun loadTrips() {
        RetrofitClient.instance.getTrips(getActiveEmail()).enqueue(object : retrofit2.Callback<TripListResponse> {
            override fun onResponse(call: retrofit2.Call<TripListResponse>, response: retrofit2.Response<TripListResponse>) {
                if (response.isSuccessful) {
                    val apiTrips = response.body()?.data ?: emptyList()
                    val trips = apiTrips.map {
                        val resolvedImage = getDestinationImage(it.destination)
                        Trip(it.destination, "Planned Trip", "Pending", "Active", resolvedImage, "#3B82F6")
                    }.toMutableList()

                    runOnUiThread {
                        val rv = findViewById<RecyclerView>(R.id.rvTrips)
                        val emptyView = findViewById<View>(R.id.llEmptyState)
                        
                        if (trips.isEmpty()) {
                            rv.visibility = View.GONE
                            emptyView.visibility = View.VISIBLE
                        } else {
                            rv.visibility = View.VISIBLE
                            emptyView.visibility = View.GONE
                            rv.layoutManager = LinearLayoutManager(this@MyTripsActivity)
                            rv.adapter = TripAdapter(trips) { tripToDelete ->
                                deleteTrip(tripToDelete)
                            }
                        }
                    }
                } else {
                    android.widget.Toast.makeText(this@MyTripsActivity, "Server Error: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<TripListResponse>, t: Throwable) {
                android.widget.Toast.makeText(this@MyTripsActivity, "Network Failure: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun deleteTrip(trip: Trip) {
        RetrofitClient.instance.deleteTrip(getActiveEmail(), trip.title).enqueue(object : retrofit2.Callback<ApiResponse> {
            override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                loadTrips()
            }
            override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAlerts).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    class TripAdapter(private val items: MutableList<Trip>, private val onDelete: (Trip) -> Unit) : RecyclerView.Adapter<TripAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.ivTripImage)
            val title: TextView = view.findViewById(R.id.tvTripTitle)
            val dates: TextView = view.findViewById(R.id.tvTripDates)
            val status: TextView = view.findViewById(R.id.tvStatusBadge)
            val deleteBtn: View = view.findViewById(R.id.ivDeleteTrip)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_trip, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.title.text = item.title
            holder.dates.text = item.dates
            holder.status.text = item.status
            holder.image.setImageResource(item.imageRes)
            holder.status.background.setTint(android.graphics.Color.parseColor(item.statusColor))
            
            holder.deleteBtn.setOnClickListener { onDelete(item) }

            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, TripDetailsActivity::class.java)
                intent.putExtra("DESTINATION_TITLE", item.title)
                intent.putExtra("DESTINATION_IMAGE", item.imageRes)
                holder.itemView.context.startActivity(intent)
            }
        }
        override fun getItemCount() = items.size
    }
}
