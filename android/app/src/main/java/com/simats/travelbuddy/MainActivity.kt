package com.simats.travelbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

// Data Models


class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvLocationName: TextView
    private lateinit var tvLocationStatus: TextView
    private lateinit var tvWeatherLocation: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvLocationName = findViewById(R.id.tvUpcomingTitle)
        tvLocationStatus = findViewById(R.id.tvUpcomingDates)
        tvWeatherLocation = findViewById(R.id.tvWeatherLocation)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }


        setupNavigation()
        fetchCurrentLocation()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun fetchCurrentLocation() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getLastLocation()
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            getLastLocation()
        }
    }

    private var currentLat: Double? = null
    private var currentLng: Double? = null

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val city = addresses[0].locality ?: "Unknown City"
                        val country = addresses[0].countryName ?: "Unknown Country"
                        tvLocationName.text = city
                        tvLocationStatus.text = "Current: $country"
                        tvWeatherLocation.text = "Weather in $city"
                    }
                } else {
                    tvLocationName.text = "Location Unknown"
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.llSearchBar).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.cvUpcomingTrip).setOnClickListener {
            if (currentLat != null && currentLng != null) {
                val gmmIntentUri = android.net.Uri.parse("geo:$currentLat,$currentLng?z=15")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                startActivity(mapIntent)
            } else {
                // Fallback to trip details if location isn't ready
                startActivity(Intent(this, TripDetailsActivity::class.java))
            }
        }
        findViewById<View>(R.id.navTrips).setOnClickListener {
            startActivity(Intent(this, MyTripsActivity::class.java))
        }
        findViewById<View>(R.id.cvAskBuddy).setOnClickListener {
            startActivity(Intent(this, ChatBuddyActivity::class.java))
        }
        findViewById<View>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        findViewById<View>(R.id.navAlerts).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
        }
        findViewById<View>(R.id.cvCommunity).setOnClickListener {
            startActivity(Intent(this, CommunityActivity::class.java))
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }




}