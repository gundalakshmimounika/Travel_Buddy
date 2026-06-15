package com.simats.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class SearchActivity : AppCompatActivity() {
    private lateinit var trendingAdapter: TrendingAdapter
    private val allDestinations = listOf(
        "Mahabalipuram", "Ooty", "Pondicherry", "Madurai", "Bali, Indonesia", 
        "Tokyo, Japan", "Santorini, Greece", "Dubai, UAE", "Paris, France", 
        "Iceland", "Maldives", "New York, USA", "London, UK", "Rome, Italy", 
        "Singapore", "Sydney, Australia", "Kodaikanal", "Munnar", "Hampi"
    )

    private val countryMap = mapOf(
        "america" to listOf("New York, USA", "Los Angeles", "Grand Canyon", "Las Vegas", "Chicago"),
        "usa" to listOf("New York, USA", "Los Angeles", "Grand Canyon", "Las Vegas", "Chicago"),
        "india" to listOf("New Delhi", "Mumbai", "Taj Mahal", "Goa", "Jaipur", "Kerala"),
        "tamil nadu" to listOf("Chennai", "Madurai", "Ooty", "Mahabalipuram", "Kodaikanal", "Coimbatore"),
        "france" to listOf("Paris", "Nice", "Lyon", "Marseille", "Bordeaux"),
        "uk" to listOf("London", "Edinburgh", "Manchester", "Liverpool"),
        "australia" to listOf("Sydney", "Melbourne", "Great Barrier Reef", "Perth")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupTrending()
        setupSearch()
        setupNavigation()
    }

    private fun setupSearch() {
        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterDestinations(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterDestinations(query: String) {
        val lowerQuery = query.lowercase().trim()
        val filtered = when {
            query.isEmpty() -> allDestinations
            countryMap.containsKey(lowerQuery) -> countryMap[lowerQuery]!!
            else -> {
                val matches = allDestinations.filter { it.contains(query, ignoreCase = true) }.toMutableList()
                if (matches.isEmpty()) matches.add(query)
                matches
            }
        }
        trendingAdapter.updateItems(filtered)
    }

    private fun setupTrending() {
        val rv = findViewById<RecyclerView>(R.id.rvTrending)
        trendingAdapter = TrendingAdapter(allDestinations.toMutableList())
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = trendingAdapter
    }

    private fun setupNavigation() {
        findViewById<View>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navTrips).setOnClickListener {
            startActivity(Intent(this, MyTripsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navAlerts).setOnClickListener {
            startActivity(Intent(this, AlertsActivity::class.java))
            finish()
        }
        findViewById<View>(R.id.navProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    class TrendingAdapter(private var items: MutableList<String>) : RecyclerView.Adapter<TrendingAdapter.ViewHolder>() {
        fun updateItems(newItems: List<String>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvDestinationName)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_trending, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val title = items[position]
            holder.name.text = title
            holder.itemView.setOnClickListener {
                val intent = Intent(holder.itemView.context, TripDetailsActivity::class.java)
                intent.putExtra("DESTINATION_TITLE", title)
                val imageRes = when {
                    title.contains("Mahabalipuram", true) -> R.drawable.img_mahabali
                    title.contains("Pondicherry", true) -> R.drawable.img_pondy
                    title.contains("Ooty", true) -> R.drawable.img_ooty
                    title.contains("Madurai", true) -> R.drawable.img_madurai
                    title.contains("New York", true) -> R.drawable.img_tokyo // Use proxy for now
                    title.contains("Los Angeles", true) -> R.drawable.img_bali
                    title.contains("Las Vegas", true) -> R.drawable.img_dubai
                    title.contains("Taj Mahal", true) -> R.drawable.img_madurai
                    title.contains("Paris", true) -> R.drawable.img_bali
                    title.contains("London", true) -> R.drawable.img_iceland
                    title.contains("Bali", true) -> R.drawable.img_bali
                    title.contains("Tokyo", true) -> R.drawable.img_tokyo
                    title.contains("Santorini", true) -> R.drawable.img_santorini
                    title.contains("Dubai", true) -> R.drawable.img_dubai
                    else -> R.drawable.img_bali
                }
                intent.putExtra("DESTINATION_IMAGE", imageRes)
                holder.itemView.context.startActivity(intent)
            }
        }
        override fun getItemCount() = items.size
    }
}
