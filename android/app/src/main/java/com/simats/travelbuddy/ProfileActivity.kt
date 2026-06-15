package com.simats.travelbuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        // Apply edge-to-edge window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back button navigation
        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        // Load dynamics active userName from database preferences
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
        val activeUserName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta")
        findViewById<TextView>(R.id.tvProfileName).text = activeUserName

        // Set up click listeners for the list options
        findViewById<LinearLayout>(R.id.llMyInformation).setOnClickListener {
            startActivity(Intent(this, MyInfoActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llJoinRequest).setOnClickListener {
            startActivity(Intent(this, JoinRequestsActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llPrivacyPolicy).setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.llHelpSupport).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        // Logout action: clear sessions and redirect cleanly to LoginActivity
        findViewById<LinearLayout>(R.id.llLogOut).setOnClickListener {
            // Clear credentials
            sharedPreferences.edit().apply {
                remove("ACTIVE_EMAIL")
                remove("ACTIVE_USER_NAME")
                apply()
            }

            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show()

            // Reset backstack and navigate to Login page
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }
}
