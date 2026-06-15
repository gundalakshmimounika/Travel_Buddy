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

data class ItineraryDay(val day: String, val title: String, val activities: String)
data class Attraction(val name: String, val distance: String, val imageRes: Int)

class TripDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_details)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rlToolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val title = intent.getStringExtra("DESTINATION_TITLE") ?: "Bali, Indonesia"
        val imageRes = intent.getIntExtra("DESTINATION_IMAGE", R.drawable.img_bali)

        findViewById<TextView>(R.id.tvTitle).text = title
        findViewById<ImageView>(R.id.ivHeroImage).setImageResource(imageRes)

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        findViewById<View>(R.id.cvPacking).setOnClickListener {
            val packingIntent = Intent(this, PackingListActivity::class.java)
            packingIntent.putExtra("TRIP_TITLE", title)
            startActivity(packingIntent)
        }

        findViewById<View>(R.id.cvTickets).setOnClickListener {
            val ticketIntent = Intent(this, TicketBookingActivity::class.java)
            ticketIntent.putExtra("DESTINATION", title)
            ticketIntent.putExtra("FROM_LOCATION", "Chennai, India") // Mock location or fetch from Prefs
            startActivity(ticketIntent)
        }

        findViewById<View>(R.id.cvHotels).setOnClickListener {
            val hotelIntent = Intent(this, HotelBookingActivity::class.java)
            hotelIntent.putExtra("DESTINATION", title)
            startActivity(hotelIntent)
        }

        findViewById<View>(R.id.cvBudget).setOnClickListener {
            val budgetIntent = Intent(this, BudgetTrackerActivity::class.java)
            budgetIntent.putExtra("TRIP_TITLE", title)
            startActivity(budgetIntent)
        }

        findViewById<View>(R.id.cvWeather).setOnClickListener {
            val weatherIntent = Intent(this, WeatherDetailsActivity::class.java)
            weatherIntent.putExtra("DESTINATION", title)
            startActivity(weatherIntent)
        }

        findViewById<View>(R.id.btnPlanWithBuddy).setOnClickListener {
            saveTrip(title, imageRes)
        }

        findViewById<View>(R.id.btnDeleteTrip).setOnClickListener {
            removeTrip(title, imageRes)
        }

        updateButtonStates(title)
        loadPlaceDetails(title)
    }

    private fun loadPlaceDetails(title: String) {
        RetrofitClient.instance.getPlaceDetails(title).enqueue(object : retrofit2.Callback<PlaceDetailsResponse> {
            override fun onResponse(call: retrofit2.Call<PlaceDetailsResponse>, response: retrofit2.Response<PlaceDetailsResponse>) {
                val info = response.body()?.data
                info?.let { it ->
                    runOnUiThread {
                        findViewById<TextView>(R.id.tvDestinationDescription).text = it.description
                        findViewById<TextView>(R.id.tvRating).text = it.rating
                        
                        // Populate new AI fields
                        findViewById<TextView>(R.id.tvHotels).text = it.hotels.joinToString("\n") { "• $it" }
                        findViewById<TextView>(R.id.tvTravelMethods).text = it.travel_methods
                        findViewById<TextView>(R.id.tvBudgetMin).text = "Min: ₹${it.budget_min}"
                        findViewById<TextView>(R.id.tvBudgetMax).text = "Max: ₹${it.budget_max}"
                        
                        // Setup Recommended Places RecyclerView
                        val places = if (it.recommended_places != null && it.recommended_places.isNotEmpty()) {
                            it.recommended_places
                        } else {
                            listOf("Scenic Viewpoint", "Historic Temple", "Local Market", "Beautiful Beach")
                        }
                        val tvRecommendedTitle = findViewById<TextView>(R.id.tvRecommendedPlacesTitle)
                        val rvRecommended = findViewById<RecyclerView>(R.id.rvRecommendedPlaces)
                        
                        tvRecommendedTitle.visibility = View.VISIBLE
                        rvRecommended.visibility = View.VISIBLE
                        rvRecommended.layoutManager = LinearLayoutManager(this@TripDetailsActivity, LinearLayoutManager.HORIZONTAL, false)
                        rvRecommended.adapter = RecommendedPlacesAdapter(places)
                        
                        // Setup Route Button
                        findViewById<View>(R.id.btnGetRoute).setOnClickListener { _ ->
                            val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${it.latitude},${it.longitude}")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            if (mapIntent.resolveActivity(packageManager) != null) {
                                  startActivity(mapIntent)
                            } else {
                                  // Fallback to browser
                                  val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${it.latitude},${it.longitude}"))
                                  startActivity(browserIntent)
                            }
                        }
                    }
                }
            }
            override fun onFailure(call: retrofit2.Call<PlaceDetailsResponse>, t: Throwable) {}
        })
    }

    private fun getActiveEmail(): String {
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("ACTIVE_EMAIL", "guest@travelbuddy.com") ?: "guest@travelbuddy.com"
    }

    private fun updateButtonStates(title: String) {
        RetrofitClient.instance.getTrips(getActiveEmail()).enqueue(object : retrofit2.Callback<TripListResponse> {
            override fun onResponse(call: retrofit2.Call<TripListResponse>, response: retrofit2.Response<TripListResponse>) {
                val trips = response.body()?.data ?: emptyList()
                val isSaved = trips.any { it.destination == title }
                
                runOnUiThread {
                    findViewById<View>(R.id.btnPlanWithBuddy).visibility = if (isSaved) View.GONE else View.VISIBLE
                    findViewById<View>(R.id.btnDeleteTrip).visibility = if (isSaved) View.VISIBLE else View.GONE
                    findViewById<View>(R.id.llQuickActionsContainer).visibility = if (isSaved) View.VISIBLE else View.GONE
                }
            }
            override fun onFailure(call: retrofit2.Call<TripListResponse>, t: Throwable) {}
        })
    }

    private fun saveTrip(title: String, imageRes: Int) {
        RetrofitClient.instance.addTrip(getActiveEmail(), title, imageRes).enqueue(object : retrofit2.Callback<ApiResponse> {
            override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                if (response.isSuccessful) {
                    updateButtonStates(title)
                    AlertHelper.showNotification(
                        this@TripDetailsActivity,
                        "Let's begun journey",
                        "Let's begun journey for $title",
                        "trip"
                    )
                    android.widget.Toast.makeText(this@TripDetailsActivity, "Trip Added to My Trips", android.widget.Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@TripDetailsActivity, MyTripsActivity::class.java))
                }
            }
            override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {
                android.widget.Toast.makeText(this@TripDetailsActivity, "Network Failure", android.widget.Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun removeTrip(title: String, imageRes: Int) {
        RetrofitClient.instance.deleteTrip(getActiveEmail(), title).enqueue(object : retrofit2.Callback<ApiResponse> {
            override fun onResponse(call: retrofit2.Call<ApiResponse>, response: retrofit2.Response<ApiResponse>) {
                if (response.isSuccessful) {
                    updateButtonStates(title)
                    android.widget.Toast.makeText(this@TripDetailsActivity, "Trip Deleted", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(this@TripDetailsActivity, "Error: ${response.code()}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: retrofit2.Call<ApiResponse>, t: Throwable) {
                android.widget.Toast.makeText(this@TripDetailsActivity, "Network Failure: ${t.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        })
    }

    class RecommendedPlacesAdapter(private val places: List<String>) : RecyclerView.Adapter<RecommendedPlacesAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val image: ImageView = view.findViewById(R.id.ivDestination)
            val rating: TextView = view.findViewById(R.id.tvRating)
            val title: TextView = view.findViewById(R.id.tvTitle)
        }
        
        private val images = listOf(
            R.drawable.img_resort,
            R.drawable.img_bali,
            R.drawable.img_mahabali,
            R.drawable.img_ooty,
            R.drawable.img_pondy,
            R.drawable.img_dubai,
            R.drawable.img_iceland,
            R.drawable.img_maldives,
            R.drawable.img_santorini,
            R.drawable.img_tokyo
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recommended, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val place = places[position]
            holder.title.text = place
            val mockRating = 4.5 + (position % 5) * 0.1
            holder.rating.text = String.format(java.util.Locale.US, "★ %.1f", mockRating)
            
            val imgRes = images[position % images.size]
            holder.image.setImageResource(imgRes)
        }

        override fun getItemCount() = places.size
    }
}
