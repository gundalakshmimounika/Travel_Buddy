package com.simats.travelbuddy

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.datepicker.MaterialDatePicker
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TransportMode(val type: String, val price: String, val duration: String, val iconRes: Int, val color: Int)

class TicketBookingActivity : AppCompatActivity(), PaymentResultListener {
    
    private lateinit var rvTransportOptions: RecyclerView
    private lateinit var tvLoading: TextView
    private lateinit var tvFromLocation: TextView
    private lateinit var destination: String
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    
    private var currentFromLocation: String = "Chennai, India"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ticket_booking)

        destination = intent.getStringExtra("DESTINATION") ?: "Mahabalipuram"
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        findViewById<TextView>(R.id.tvBookingHeader).text = "Tickets to $destination"
        tvFromLocation = findViewById(R.id.tvFromLocation)
        tvFromLocation.text = "From: Locating..."
        
        tvLoading = TextView(this).apply {
            text = "Loading travel options..."
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 50, 0, 50)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        rvTransportOptions = findViewById(R.id.rvTransportOptions)
        rvTransportOptions.layoutManager = LinearLayoutManager(this)

        fetchCurrentLocation()
    }

    private fun fetchCurrentLocation() {
        val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                getLastLocation()
            } else {
                tvFromLocation.text = "From: $currentFromLocation"
                fetchDynamicTransportOptions(currentFromLocation, destination)
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            getLastLocation()
        }
    }

    private fun getLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        val city = addresses[0].locality ?: "Unknown City"
                        val country = addresses[0].countryName ?: "India"
                        currentFromLocation = "$city, $country"
                    }
                }
                tvFromLocation.text = "From: $currentFromLocation"
                fetchDynamicTransportOptions(currentFromLocation, destination)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            tvFromLocation.text = "From: $currentFromLocation"
            fetchDynamicTransportOptions(currentFromLocation, destination)
        }
    }

    private fun fetchDynamicTransportOptions(from: String, to: String) {
        RetrofitClient.instance.getTickets(from, to).enqueue(object : retrofit2.Callback<TicketResponse> {
            override fun onResponse(call: retrofit2.Call<TicketResponse>, response: retrofit2.Response<TicketResponse>) {
                val tickets = response.body()?.data
                if (tickets != null && tickets.isNotEmpty()) {
                    val items = tickets.map {
                        val iconRes = when(it.type.lowercase()) {
                            "airways", "flight" -> R.drawable.ic_location
                            "train" -> R.drawable.ic_location
                            "bus" -> R.drawable.ic_location
                            else -> R.drawable.ic_location
                        }
                        val color = when(it.type.lowercase()) {
                            "airways", "flight" -> 0xFF0EA5E9.toInt()
                            "train" -> 0xFFF97316.toInt()
                            "bus" -> 0xFF10B981.toInt()
                            else -> 0xFF8B5CF6.toInt()
                        }
                        TransportMode(it.type, it.price, it.duration, iconRes, color)
                    }
                    runOnUiThread {
                        rvTransportOptions.adapter = TransportAdapter(items, destination) { mode ->
                            showBookingDialog(mode)
                        }
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@TicketBookingActivity, "No routes found", Toast.LENGTH_SHORT).show() }
                }
            }
            override fun onFailure(call: retrofit2.Call<TicketResponse>, t: Throwable) {
                runOnUiThread { Toast.makeText(this@TicketBookingActivity, "Failed to load tickets", Toast.LENGTH_SHORT).show() }
            }
        })
    }
    
    private fun showBookingDialog(mode: TransportMode) {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_book_ticket, null)
        bottomSheet.setContentView(view)
        
        val tvTitle = view.findViewById<TextView>(R.id.tvBookingDialogTitle)
        val tvDate = view.findViewById<TextView>(R.id.tvDepartureDate)
        val btnMinus = view.findViewById<TextView>(R.id.btnMinusSeat)
        val btnPlus = view.findViewById<TextView>(R.id.btnPlusSeat)
        val tvSeatCount = view.findViewById<TextView>(R.id.tvSeatCount)
        val rgClass = view.findViewById<RadioGroup>(R.id.rgSeatClass)
        val rbPremium = view.findViewById<RadioButton>(R.id.rbPremium)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalAmount)
        val btnProceed = view.findViewById<View>(R.id.btnProceedPay)
        
        tvTitle.text = "Book ${mode.type} Ticket"
        
        val rawPrice = mode.price.replace("[^0-9]".toRegex(), "")
        val basePrice = rawPrice.toIntOrNull() ?: 0
        
        var seats = 1
        var isPremium = false
        var selectedDate = 0L
        
        fun updateTotal() {
            val multiplier = if (isPremium) 1.5 else 1.0
            val total = (basePrice * seats * multiplier).toInt()
            tvTotal.text = "₹$total"
        }
        
        updateTotal()
        
        tvDate.setOnClickListener {
            val picker = MaterialDatePicker.Builder.datePicker().build()
            picker.addOnPositiveButtonClickListener { selection ->
                selectedDate = selection
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(selection))
                tvDate.text = dateStr
            }
            picker.show(supportFragmentManager, "DATE_PICKER")
        }
        
        btnMinus.setOnClickListener {
            if (seats > 1) {
                seats--
                tvSeatCount.text = seats.toString()
                updateTotal()
            }
        }
        
        btnPlus.setOnClickListener {
            if (seats < 10) {
                seats++
                tvSeatCount.text = seats.toString()
                updateTotal()
            }
        }
        
        rgClass.setOnCheckedChangeListener { _, checkedId ->
            isPremium = (checkedId == R.id.rbPremium)
            updateTotal()
        }
        
        btnProceed.setOnClickListener {
            if (selectedDate == 0L) {
                Toast.makeText(this, "Please select a date of departure", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bottomSheet.dismiss()
            val multiplier = if (isPremium) 1.5 else 1.0
            val finalAmount = (basePrice * seats * multiplier).toInt()
            initiateRazorpayPayment(mode, finalAmount)
        }
        
        bottomSheet.show()
    }

    private fun initiateRazorpayPayment(mode: TransportMode, amount: Int) {
        val totalAmountPaise = amount * 100

        val checkout = Checkout()
        try {
            val options = JSONObject()
            options.put("key", "rzp_test_SqOZwDnHPrqJm0")
            options.put("name", "TravelBuddy Tickets")
            options.put("description", "${mode.type} Ticket to $destination")
            options.put("image", "https://s3.amazonaws.com/rzp-mobile/images/rzp.png")
            options.put("theme.color", "#2563EB")
            options.put("currency", "INR")
            options.put("amount", totalAmountPaise)
            
            options.put("prefill.email", "test@razorpay.com")
            options.put("prefill.contact", "9999999999")
            options.put("prefill.vpa", "ayyappamuppavarapu2004@oksbi")

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Error in payment: " + e.message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentID: String?) {
        AlertHelper.showNotification(
            this,
            "Ticket Booked Successfully",
            "Your ticket to $destination has been confirmed successfully!",
            "travel"
        )
        AlertDialog.Builder(this)
            .setTitle("Ticket Booked Successfully")
            .setMessage("Your ticket to $destination has been confirmed!\n\nTransaction ID: $razorpayPaymentID")
            .setPositiveButton("OK") { _, _ -> finish() }
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
            "Ticket Booking Failed",
            "Failed to book ticket to $destination. $cleanMessage",
            "hotel_failure" // This type uses the professional red error style
        )
        Toast.makeText(this, "Payment failed: $cleanMessage", Toast.LENGTH_LONG).show()
    }

    class TransportAdapter(
        private val items: List<TransportMode>, 
        private val destination: String,
        private val onBookClicked: (TransportMode) -> Unit
    ) : RecyclerView.Adapter<TransportAdapter.ViewHolder>() {
        
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val type: TextView = view.findViewById(R.id.tvTransportType)
            val price: TextView = view.findViewById(R.id.tvTransportPrice)
            val duration: TextView = view.findViewById(R.id.tvTransportDuration)
            val icon: View = view.findViewById(R.id.vTransportIcon)
            val bookBtn: View = view.findViewById(R.id.btnBookNow)
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_transport_option, parent, false))
            
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.type.text = item.type
            holder.price.text = item.price
            holder.duration.text = item.duration
            holder.icon.backgroundTintList = android.content.res.ColorStateList.valueOf(item.color)
            
            holder.itemView.setOnClickListener {
                val mode = when(item.type.lowercase()) {
                    "airways", "flight" -> "transit"
                    "train" -> "transit"
                    "bus" -> "transit"
                    else -> "driving"
                }
                val gmmIntentUri = android.net.Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destination&travelmode=$mode")
                val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                holder.itemView.context.startActivity(mapIntent)
            }
            
            holder.bookBtn.setOnClickListener {
                onBookClicked(item)
            }
        }
        override fun getItemCount() = items.size
    }
}
