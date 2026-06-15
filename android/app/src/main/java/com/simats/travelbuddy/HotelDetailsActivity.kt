package com.simats.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HotelDetailsActivity : AppCompatActivity(), PaymentResultListener {
    private var personCount = 1
    private var basePrice = 2000.0
    private var nights = 1
    private lateinit var hotelName: String
    private var checkInDate: Long = 0
    private var checkOutDate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hotel_details)

        hotelName = intent.getStringExtra("HOTEL_NAME") ?: "Hotel"
        val desc = intent.getStringExtra("HOTEL_DESC") ?: ""
        
        val priceRaw = intent.getStringExtra("HOTEL_PRICE") ?: "2000"
        basePrice = priceRaw.replace("[^0-9.]".toRegex(), "").toDoubleOrNull() ?: 2000.0

        findViewById<TextView>(R.id.tvHotelName).text = hotelName
        findViewById<TextView>(R.id.tvHotelDescription).text = desc

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        setupDatePickers()
        setupBookingLogic()
    }

    private fun setupDatePickers() {
        val tvIn = findViewById<TextView>(R.id.tvCheckInDate)
        val tvOut = findViewById<TextView>(R.id.tvCheckOutDate)
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())

        val datePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Stay Dates")
            .setTheme(com.google.android.material.R.style.ThemeOverlay_Material3_MaterialCalendar)
            .build()

        findViewById<View>(R.id.cvCheckIn).setOnClickListener { datePicker.show(supportFragmentManager, "DATES") }
        findViewById<View>(R.id.cvCheckOut).setOnClickListener { datePicker.show(supportFragmentManager, "DATES") }

        datePicker.addOnPositiveButtonClickListener { selection ->
            checkInDate = selection.first ?: 0
            checkOutDate = selection.second ?: 0
            
            tvIn.text = sdf.format(Date(checkInDate))
            tvOut.text = sdf.format(Date(checkOutDate))
            
            val diff = checkOutDate - checkInDate
            nights = (diff / (1000 * 60 * 60 * 24)).toInt()
            if (nights < 1) nights = 1
        }
    }

    private fun setupBookingLogic() {
        val tvCount = findViewById<TextView>(R.id.tvPersonCount)
        findViewById<View>(R.id.ivPlus).setOnClickListener {
            personCount++
            tvCount.text = personCount.toString()
        }
        findViewById<View>(R.id.ivMinus).setOnClickListener {
            if (personCount > 1) {
                personCount--
                tvCount.text = personCount.toString()
            }
        }

        findViewById<View>(R.id.btnBookNow).setOnClickListener {
            if (checkInDate == 0L) {
                Toast.makeText(this, "Please select dates first", Toast.LENGTH_SHORT).show()
            } else {
                startRazorpayPayment()
            }
        }
        
        findViewById<View>(R.id.btnContact).setOnClickListener {
            val contact = intent.getStringExtra("HOTEL_CONTACT") ?: "+91 0000000000"
            Toast.makeText(this, "Calling Hotel: $contact", Toast.LENGTH_LONG).show()
        }
    }

    private val UPI_PAYMENT = 101

    private fun startRazorpayPayment() {
        val isAC = findViewById<RadioButton>(R.id.rbAC).isChecked
        val multiplier = if (isAC) 1.5 else 1.0
        val totalAmount = (basePrice * multiplier * personCount * nights * 100).toInt()

        val checkout = Checkout()
        
        try {
            val options = JSONObject()
            // Explicitly pass the new API key in options to avoid Manifest or Initialization race conditions
            options.put("key", "rzp_test_SqOZwDnHPrqJm0")
            options.put("name", "TravelBuddy")
            options.put("description", "Room booking at $hotelName")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#2563EB")
            options.put("currency", "INR")
            // Pass amount as Integer (Int), not String, to prevent Javascript TypeErrors inside the WebView
            options.put("amount", totalAmount)
            
            options.put("prefill.email", "test@razorpay.com")
            options.put("prefill.contact", "9999999999")
            options.put("prefill.vpa", "ayyappamuppavarapu2004@oksbi") // Prefill user UPI ID
            
            val retryObj = JSONObject()
            retryObj.put("enabled", true)
            retryObj.put("max_count", 4)
            options.put("retry", retryObj)

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        AlertHelper.showNotification(
            this,
            "Hotel Booking Successful",
            "Your room in $hotelName has been booked successfully!",
            "hotel_success"
        )
        AlertDialog.Builder(this)
            .setTitle("Booking Successful")
            .setMessage("Your room in $hotelName has been booked successfully!\n\nRazorpay Payment ID: $razorpayPaymentID")
            .setPositiveButton("OK") { _, _ -> 
                finish() 
            }
            .setCancelable(false)
            .show()
    }

    private fun cleanRazorpayError(code: Int, response: String?): String {
        if (code == Checkout.PAYMENT_CANCELED) {
            return "Payment was cancelled."
        }
        var cleanMessage = "The transaction was not completed."
        try {
            if (!response.isNullOrEmpty()) {
                val json = JSONObject(response)
                val errorObj = json.optJSONObject("error")
                if (errorObj != null) {
                    val desc = errorObj.optString("description", "")
                    val reason = errorObj.optString("reason", "")
                    val step = errorObj.optString("step", "")
                    
                    cleanMessage = when {
                        !desc.isNullOrEmpty() && desc != "undefined" -> desc
                        step == "payment_authentication" -> "Payment failed during authentication. Please verify your UPI/bank credentials and try again."
                        step == "payment_authorization" -> "Payment authorization failed. The transaction was declined by the bank."
                        !reason.isNullOrEmpty() && reason != "undefined" -> {
                            reason.replace("_", " ").uppercase()
                        }
                        else -> "Payment failed or cancelled."
                    }
                } else {
                    val desc = json.optString("description", "")
                    if (!desc.isNullOrEmpty() && desc != "undefined") {
                        cleanMessage = desc
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (!response.isNullOrEmpty()) {
                cleanMessage = response
            }
        }
        return cleanMessage
    }

    override fun onPaymentError(code: Int, response: String?) {
        val cleanMessage = cleanRazorpayError(code, response)
        AlertHelper.showNotification(
            this,
            "Hotel Booking Failed",
            "Failed to book room in $hotelName. $cleanMessage",
            "hotel_failure"
        )
        Toast.makeText(this, "Payment failed: $cleanMessage", Toast.LENGTH_LONG).show()
    }
}
