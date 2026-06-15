package com.simats.travelbuddy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

class MyInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_info)

        // Handle edge-to-edge system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.ivBack).setOnClickListener {
            finish()
        }

        // Fetch dynamical registered credentials from local context storage
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
        val activeName = sharedPreferences.getString("ACTIVE_USER_NAME", "Aarav Mehta")
        val activeEmail = sharedPreferences.getString("ACTIVE_EMAIL", "aarav.mehta@gmail.com")

        findViewById<TextView>(R.id.tvInfoName).text = activeName
        findViewById<TextView>(R.id.tvInfoEmail).text = activeEmail

        // Working real Reset Password connector passing dynamic email
        findViewById<MaterialButton>(R.id.btnResetPassword).setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java).apply {
                putExtra("USER_EMAIL", activeEmail)
            }
            startActivity(intent)
        }
    }
}
