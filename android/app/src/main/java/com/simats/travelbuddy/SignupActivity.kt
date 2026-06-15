package com.simats.travelbuddy

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etFullName = findViewById<EditText>(R.id.etFullName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnCreateAccount = findViewById<MaterialButton>(R.id.btnCreateAccount)
        val tvSignIn = findViewById<TextView>(R.id.tvSignIn)

        tvSignIn.setOnClickListener {
            finish() // Go back to login
        }

        btnCreateAccount.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (fullName.isEmpty()) {
                Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 1: Email must end with "@gmail.com"
            if (!email.lowercase().endsWith("@gmail.com")) {
                Toast.makeText(this, "Email must end with @gmail.com", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 2a: Password must be longer than 6 digits/letters (length > 6)
            if (password.length <= 6) {
                Toast.makeText(this, "Password must be longer than 6 characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Rule 2b: Password must contain at least 1 capital letter (uppercase)
            if (!password.any { it.isUpperCase() }) {
                Toast.makeText(this, "Password must contain at least 1 uppercase letter", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Show loading dialog
            val progressDialog = ProgressDialog(this).apply {
                setMessage("Creating account...")
                setCancelable(false)
                show()
            }

            // Execute registration against backend API
            RetrofitClient.instance.register(fullName, email, password)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        progressDialog.dismiss()
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.status == "success") {
                                Toast.makeText(this@SignupActivity, "Account created successfully!", Toast.LENGTH_LONG).show()
                                
                                // Save local auth state
                                val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().apply {
                                    putString("ACTIVE_EMAIL", email)
                                    putString("ACTIVE_USER_NAME", fullName)
                                    apply()
                                }

                                val intent = Intent(this@SignupActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMsg = apiResponse?.message ?: "Registration failed"
                                Toast.makeText(this@SignupActivity, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@SignupActivity, "Server error. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        progressDialog.dismiss()
                        Toast.makeText(this@SignupActivity, "Connection error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
}
