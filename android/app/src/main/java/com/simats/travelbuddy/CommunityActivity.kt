package com.simats.travelbuddy

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommunityActivity : AppCompatActivity() {

    private lateinit var rvCommunityPosts: RecyclerView
    private lateinit var adapter: CommunityAdapter
    private val postsList = mutableListOf<CommunityPost>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_community)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rlToolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        rvCommunityPosts = findViewById(R.id.rvCommunityPosts)
        rvCommunityPosts.layoutManager = LinearLayoutManager(this)
        adapter = CommunityAdapter(
            postsList,
            onJoinClick = { post -> joinRequest(post) },
            onEditClick = { post -> showEditPostDialog(post) },
            onDeleteClick = { post -> showDeleteConfirmationDialog(post) }
        )
        rvCommunityPosts.adapter = adapter

        findViewById<View>(R.id.fabCreatePost).setOnClickListener {
            showCreatePostDialog()
        }

        loadCommunityPosts()
    }

    private fun loadCommunityPosts() {
        RetrofitClient.instance.getCommunityPosts().enqueue(object : Callback<CommunityResponse> {
            override fun onResponse(call: Call<CommunityResponse>, response: Response<CommunityResponse>) {
                val data = response.body()?.data
                if (data != null) {
                    runOnUiThread {
                        postsList.clear()
                        postsList.addAll(data)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun onFailure(call: Call<CommunityResponse>, t: Throwable) {
                runOnUiThread {
                    Toast.makeText(this@CommunityActivity, "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun joinRequest(post: CommunityPost) {
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
        val activeName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta") ?: "Aarav Mehta"
        val activeEmail = sharedPreferences.getString("ACTIVE_EMAIL", "aarav.mehta@gmail.com") ?: "aarav.mehta@gmail.com"

        RetrofitClient.instance.joinCommunityPost(post.id, activeName, activeEmail).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        AlertHelper.showNotification(
                            this@CommunityActivity,
                            "Join Request Sent",
                            "You requested to join ${post.user_name}'s travel plan to ${post.destination}!",
                            "join"
                        )
                        Toast.makeText(this@CommunityActivity, "Join Request Sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun showCreatePostDialog() {
        val bottomSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_create_post, null)
        bottomSheet.setContentView(sheetView)

        bottomSheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ViewCompat.setOnApplyWindowInsetsListener(sheetView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val density = v.resources.displayMetrics.density
            val defaultPadding = (24 * density).toInt()
            
            val bottomPadding = defaultPadding + kotlin.math.max(imeInsets.bottom, systemBars.bottom)
            v.setPadding(defaultPadding, defaultPadding, defaultPadding, bottomPadding)
            
            insets
        }

        val etDestination = sheetView.findViewById<EditText>(R.id.etDestination)
        val etDates = sheetView.findViewById<EditText>(R.id.etDates)
        val etDescription = sheetView.findViewById<EditText>(R.id.etDescription)
        val btnPost = sheetView.findViewById<Button>(R.id.btnPostPlan)

        var selectedDatesRange = ""

        etDates.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Travel Dates")
                .build()
            
            picker.addOnPositiveButtonClickListener { selection ->
                val firstLong = selection.first
                val secondLong = selection.second
                if (firstLong != null && secondLong != null) {
                    val startStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(firstLong))
                    val endStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(secondLong))
                    selectedDatesRange = "$startStr - $endStr"
                    etDates.setText(selectedDatesRange)
                }
            }
            picker.show(supportFragmentManager, "RANGE_PICKER")
        }

        btnPost.setOnClickListener {
            val destination = etDestination.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (destination.isEmpty() || description.isEmpty() || selectedDatesRange.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
            val activeUserName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta") ?: "Aarav Mehta"

            RetrofitClient.instance.addCommunityPost(
                userName = activeUserName,
                destination = destination,
                dates = selectedDatesRange,
                description = description
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@CommunityActivity, "Post Shared Successfully!", Toast.LENGTH_SHORT).show()
                            bottomSheet.dismiss()
                            loadCommunityPosts()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@CommunityActivity, "Network error sharing post", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        bottomSheet.show()
    }

    private fun showEditPostDialog(post: CommunityPost) {
        val bottomSheet = BottomSheetDialog(this)
        val sheetView = layoutInflater.inflate(R.layout.dialog_create_post, null)
        bottomSheet.setContentView(sheetView)

        bottomSheet.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        ViewCompat.setOnApplyWindowInsetsListener(sheetView) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            val density = v.resources.displayMetrics.density
            val defaultPadding = (24 * density).toInt()
            
            val bottomPadding = defaultPadding + kotlin.math.max(imeInsets.bottom, systemBars.bottom)
            v.setPadding(defaultPadding, defaultPadding, defaultPadding, bottomPadding)
            
            insets
        }

        val etDestination = sheetView.findViewById<EditText>(R.id.etDestination)
        val etDates = sheetView.findViewById<EditText>(R.id.etDates)
        val etDescription = sheetView.findViewById<EditText>(R.id.etDescription)
        val btnPost = sheetView.findViewById<Button>(R.id.btnPostPlan)

        etDestination.setText(post.destination)
        etDates.setText(post.dates)
        etDescription.setText(post.description)
        btnPost.text = "Update Plan"

        var selectedDatesRange = post.dates

        etDates.setOnClickListener {
            val picker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Select Travel Dates")
                .build()
            
            picker.addOnPositiveButtonClickListener { selection ->
                val firstLong = selection.first
                val secondLong = selection.second
                if (firstLong != null && secondLong != null) {
                    val startStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(firstLong))
                    val endStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(secondLong))
                    selectedDatesRange = "$startStr - $endStr"
                    etDates.setText(selectedDatesRange)
                }
            }
            picker.show(supportFragmentManager, "RANGE_PICKER")
        }

        btnPost.setOnClickListener {
            val destination = etDestination.text.toString().trim()
            val description = etDescription.text.toString().trim()

            if (destination.isEmpty() || description.isEmpty() || selectedDatesRange.isEmpty()) {
                Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            RetrofitClient.instance.editCommunityPost(
                id = post.id,
                destination = destination,
                dates = selectedDatesRange,
                description = description
            ).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@CommunityActivity, "Post Updated Successfully!", Toast.LENGTH_SHORT).show()
                            bottomSheet.dismiss()
                            loadCommunityPosts()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@CommunityActivity, "Network error updating post", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        bottomSheet.show()
    }

    private fun showDeleteConfirmationDialog(post: CommunityPost) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Delete Travel Plan")
        builder.setMessage("Are you sure you want to delete this plan?")
        builder.setPositiveButton("Delete") { _, _ ->
            RetrofitClient.instance.deleteCommunityPost(post.id).enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@CommunityActivity, "Post Deleted Successfully!", Toast.LENGTH_SHORT).show()
                            loadCommunityPosts()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    runOnUiThread {
                        Toast.makeText(this@CommunityActivity, "Network error deleting post", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    class CommunityAdapter(
        private val posts: List<CommunityPost>,
        private val onJoinClick: (CommunityPost) -> Unit,
        private val onEditClick: (CommunityPost) -> Unit,
        private val onDeleteClick: (CommunityPost) -> Unit
    ) : RecyclerView.Adapter<CommunityAdapter.ViewHolder>() {

        private val requestedPostIds = mutableSetOf<Int>()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
            val tvUserName: TextView = view.findViewById(R.id.tvUserName)
            val tvUserRating: TextView = view.findViewById(R.id.tvUserRating)
            val tvDestination: TextView = view.findViewById(R.id.tvDestination)
            val tvDates: TextView = view.findViewById(R.id.tvDates)
            val tvDescription: TextView = view.findViewById(R.id.tvDescription)
            val tvInterestedCount: TextView = view.findViewById(R.id.tvInterestedCount)
            val btnJoinRequest: Button = view.findViewById(R.id.btnJoinRequest)
            val ivEditPost: ImageView = view.findViewById(R.id.ivEditPost)
            val ivDeletePost: ImageView = view.findViewById(R.id.ivDeletePost)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_community_post, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val post = posts[position]
            val sharedPreferences = holder.itemView.context.getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
            val activeUserName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta") ?: "Aarav Mehta"
            val displayedUserName = if (post.user_name == "Ayyappa") activeUserName else post.user_name
            holder.tvUserName.text = displayedUserName
            holder.tvUserRating.text = post.user_rating
            holder.tvDestination.text = post.destination
            holder.tvDates.text = post.dates
            holder.tvDescription.text = post.description

            val baseInterested = post.interested_count
            val isRequested = requestedPostIds.contains(post.id)
            val currentInterestVal = if (isRequested) baseInterested + 1 else baseInterested
            holder.tvInterestedCount.text = "$currentInterestVal interested"

            // Cycle avatars nicely matching user designs
            val avatarRes = when (post.avatar_id) {
                1 -> R.drawable.img_resort // Sarah avatar style
                2 -> R.drawable.img_bali   // Mike avatar style
                3 -> R.drawable.img_ooty
                4 -> R.drawable.img_pondy
                else -> R.drawable.img_tokyo
            }
            holder.ivAvatar.setImageResource(avatarRes)

            if (isRequested) {
                holder.btnJoinRequest.isEnabled = false
                holder.btnJoinRequest.text = "Requested ✓"
                holder.btnJoinRequest.setBackgroundColor(android.graphics.Color.parseColor("#94A3B8"))
                holder.btnJoinRequest.setTextColor(android.graphics.Color.WHITE)
            } else {
                holder.btnJoinRequest.isEnabled = true
                holder.btnJoinRequest.text = "Join Request"
                holder.btnJoinRequest.setBackgroundColor(android.graphics.Color.parseColor("#0F172A"))
                holder.btnJoinRequest.setTextColor(android.graphics.Color.WHITE)
            }

            holder.btnJoinRequest.setOnClickListener {
                requestedPostIds.add(post.id)
                notifyItemChanged(position)
                onJoinClick(post)
            }

            holder.ivEditPost.setOnClickListener { onEditClick(post) }
            holder.ivDeletePost.setOnClickListener { onDeleteClick(post) }
        }

        override fun getItemCount() = posts.size
    }
}
