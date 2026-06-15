package com.simats.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HotelBookingActivity : AppCompatActivity() {
    private lateinit var adapter: HotelAdapter
    private val hotelList = mutableListOf<HotelItem>()
    private lateinit var destination: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hotel_booking)

        destination = intent.getStringExtra("DESTINATION") ?: "Ooty"
        findViewById<TextView>(R.id.tvHotelHeader).text = "Hotels in $destination"

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        setupRecyclerView()
        loadHotels()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvHotels)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = HotelAdapter(hotelList) { hotel ->
            val intent = Intent(this, HotelDetailsActivity::class.java)
            intent.putExtra("HOTEL_NAME", hotel.name)
            intent.putExtra("HOTEL_DESC", hotel.description)
            intent.putExtra("HOTEL_PRICE", hotel.price)
            intent.putExtra("HOTEL_CONTACT", hotel.contact)
            startActivity(intent)
        }
        rv.adapter = adapter
    }

    private fun loadHotels() {
        findViewById<ProgressBar>(R.id.pbLoading).visibility = View.VISIBLE
        RetrofitClient.instance.getHotels(destination).enqueue(object : Callback<List<HotelItem>> {
            override fun onResponse(call: Call<List<HotelItem>>, response: Response<List<HotelItem>>) {
                findViewById<ProgressBar>(R.id.pbLoading).visibility = View.GONE
                response.body()?.let {
                    hotelList.clear()
                    hotelList.addAll(it)
                    adapter.notifyDataSetChanged()
                }
            }
            override fun onFailure(call: Call<List<HotelItem>>, t: Throwable) {
                findViewById<ProgressBar>(R.id.pbLoading).visibility = View.GONE
                Toast.makeText(this@HotelBookingActivity, "Failed to load hotels", Toast.LENGTH_SHORT).show()
            }
        })
    }

    class HotelAdapter(private val items: List<HotelItem>, private val onClick: (HotelItem) -> Unit) :
        RecyclerView.Adapter<HotelAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvHotelName)
            val rating: TextView = view.findViewById(R.id.tvHotelRating)
            val price: TextView = view.findViewById(R.id.tvHotelPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_hotel, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.name
            holder.rating.text = item.rating
            holder.price.text = "₹${item.price} / night"
            holder.itemView.setOnClickListener { onClick(item) }
        }

        override fun getItemCount() = items.size
    }
}
