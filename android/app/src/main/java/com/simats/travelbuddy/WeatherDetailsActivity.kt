package com.simats.travelbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class WeatherDetailsActivity : AppCompatActivity() {
    private lateinit var hourlyAdapter: HourlyAdapter
    private lateinit var dailyAdapter: DailyAdapter
    private val hourlyList = mutableListOf<HourlyWeather>()
    private val dailyList = mutableListOf<DailyWeather>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_weather_details)

        val destination = intent.getStringExtra("DESTINATION") ?: "Mahabalipuram"
        findViewById<TextView>(R.id.tvWeatherHeader).text = "Weather in $destination"

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        setupRecyclerViews()
        fetchWeatherData(destination)
    }

    private fun setupRecyclerViews() {
        val rvHourly = findViewById<RecyclerView>(R.id.rvHourly)
        rvHourly.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        hourlyAdapter = HourlyAdapter(hourlyList)
        rvHourly.adapter = hourlyAdapter

        val rvDaily = findViewById<RecyclerView>(R.id.rvDaily)
        rvDaily.layoutManager = LinearLayoutManager(this)
        dailyAdapter = DailyAdapter(dailyList)
        rvDaily.adapter = dailyAdapter
    }

    private fun fetchWeatherData(place: String) {
        RetrofitClient.instance.getWeather(place).enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(call: Call<WeatherResponse>, response: Response<WeatherResponse>) {
                response.body()?.let {
                    hourlyList.clear()
                    hourlyList.addAll(it.hourly)
                    
                    dailyList.clear()
                    dailyList.addAll(it.daily)
                    
                    updateUI()

                    // Check for severe weather warnings
                    val badWeather = it.daily.firstOrNull { day ->
                        val cond = day.condition.lowercase()
                        cond.contains("rain") || cond.contains("storm") || cond.contains("snow") || cond.contains("thunderstorm") || cond.contains("heavy")
                    }
                    if (badWeather != null) {
                        AlertHelper.showNotification(
                            this@WeatherDetailsActivity,
                            "Severe Weather Warning",
                            "Severe weather warning for $place: Expect ${badWeather.condition} on ${badWeather.day}!",
                            "weather"
                        )
                    }
                }
            }
            override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                Toast.makeText(this@WeatherDetailsActivity, "Failed to load weather", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI() {
        runOnUiThread {
            hourlyAdapter.notifyDataSetChanged()
            dailyAdapter.notifyDataSetChanged()
        }
    }

    class HourlyAdapter(private val items: List<HourlyWeather>) :
        RecyclerView.Adapter<HourlyAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val time: TextView = view.findViewById(R.id.tvHourlyTime)
            val temp: TextView = view.findViewById(R.id.tvHourlyTemp)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_weather_hourly, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.time.text = item.time
            holder.temp.text = item.temp
        }
        override fun getItemCount() = items.size
    }

    class DailyAdapter(private val items: List<DailyWeather>) :
        RecyclerView.Adapter<DailyAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val day: TextView = view.findViewById(R.id.tvDailyDay)
            val low: TextView = view.findViewById(R.id.tvDailyLow)
            val high: TextView = view.findViewById(R.id.tvDailyHigh)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_weather_daily, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.day.text = item.day
            holder.low.text = item.low
            holder.high.text = item.high
        }
        override fun getItemCount() = items.size
    }
}
