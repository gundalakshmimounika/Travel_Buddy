package com.simats.travelbuddy

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
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

class ResetPasswordActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_reset_password)

        // Handle edge-to-edge system bar insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val email = intent.getStringExtra("USER_EMAIL") ?: "your account"

        val ivBack = findViewById<ImageView>(R.id.ivBack)
        val tvResetSubtitle = findViewById<TextView>(R.id.tvResetSubtitle)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnSave = findViewById<MaterialButton>(R.id.btnSave)

        // Customize description dynamically
        tvResetSubtitle.text = "Create a strong, secure new password for:\n$email"

        ivBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Rule 2a: Password must be longer than 6 digits/letters (length > 6)
            if (newPassword.length <= 6) {
                Toast.makeText(this, "Password must be longer than 6 characters", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Rule 2b: Password must contain at least 1 uppercase letter
            if (!newPassword.any { it.isUpperCase() }) {
                Toast.makeText(this, "Password must contain at least 1 uppercase letter", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please re-enter your new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show loading dialog
            val progressDialog = ProgressDialog(this).apply {
                setMessage("Resetting password...")
                setCancelable(false)
                show()
            }

            // Execute reset call against backend API
            RetrofitClient.instance.resetPassword(email, newPassword)
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        progressDialog.dismiss()
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.status == "success") {
                                // Persist locally in SharedPreferences
                                val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
                                sharedPreferences.edit().apply {
                                    putString("PASSWORD_$email", newPassword)
                                    putString("ACTIVE_EMAIL", email)
                                    apply()
                                }

                                Toast.makeText(this@ResetPasswordActivity, "Password updated successfully!", Toast.LENGTH_LONG).show()

                                // Clear active activity stack and redirect back to LoginActivity
                                val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                }
                                startActivity(intent)
                                finish()
                            } else {
                                val errorMsg = apiResponse?.message ?: "Failed to reset password"
                                Toast.makeText(this@ResetPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this@ResetPasswordActivity, "Server error. Please try again.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        progressDialog.dismiss()
                        Toast.makeText(this@ResetPasswordActivity, "Connection error: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
}
