package com.simats.travelbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class PlannerDay(val day: String, val title: String, val description: String)

class TripPlannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trip_planner)

        val destination = intent.getStringExtra("DESTINATION") ?: "Mahabalipuram"
        findViewById<TextView>(R.id.tvPlannerHeader).text = "Plan for $destination"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        setupItinerary(destination)
    }

    private fun setupItinerary(destination: String) {
        val rv = findViewById<RecyclerView>(R.id.rvPlannerItinerary)
        val items = when(destination) {
            "Mahabalipuram" -> listOf(
                PlannerDay("Day 1", "The Heritage Core", "Shore Temple, Krishna's Butter Ball, Arjuna's Penance, and Ganesha Ratha. End the day at the main beach."),
                PlannerDay("Day 2", "Monolithic Wonders", "Five Rathas (Pancha Rathas), Mahabalipuram Lighthouse, and the Olakkanneshvara Temple."),
                PlannerDay("Day 3", "Caves & Culture", "Tiger Cave, India Seashell Museum, and a visit to the stone-carving workshops in the village.")
            )
            "Pondicherry" -> listOf(
                PlannerDay("Day 1", "French Quarter & Heritage", "Walk through the White Town, visit Sri Aurobindo Ashram, Manakula Vinayagar Temple, and Promenade Beach."),
                PlannerDay("Day 2", "Spirituality & Peace", "Spend the day at Auroville, visit the Matrimandir, and relax at Serenity Beach."),
                PlannerDay("Day 3", "Beaches & History", "Take a ferry to Paradise Beach (Chunnambar), explore the ruins of Arikamedu, and visit the Sacred Heart Basilica."),
                PlannerDay("Day 4", "Gardens & Museums", "Botanical Garden, Pondicherry Museum, and shopping at Mission Street for local handicrafts.")
            )
            "Ooty" -> listOf(
                PlannerDay("Day 1", "Ooty Highlights", "Government Botanical Garden, Rose Garden, Ooty Lake, and the Thread Garden."),
                PlannerDay("Day 2", "Peak & Tea Heritage", "Doddabetta Peak (highest point), Tea Factory & Museum, and Chocolate Museum."),
                PlannerDay("Day 3", "Nature & Lakes", "Pykara Waterfalls, Pykara Boat House, Shooting Spot (6th Mile), and Wenlock Downs (9th Mile)."),
                PlannerDay("Day 4", "Coonoor Toy Train", "Nilgiri Mountain Railway (Toy Train) ride from Ooty to Coonoor, visit Sim's Park, Lamb's Rock, and Dolphin's Nose.")
            )
            else -> listOf(
                PlannerDay("Day 1", "Arrival & Landmark Tour", "Visit the most famous icons and settle into the local atmosphere."),
                PlannerDay("Day 2", "Culture & Markets", "Explore local heritage, museums, and traditional markets."),
                PlannerDay("Day 3", "Hidden Gems", "Venture off the beaten path to discover quiet spots and local favorites."),
                PlannerDay("Day 4", "Nature & Farewell", "Enjoy a scenic spot for sunrise and do some final souvenir shopping.")
            )
        }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = PlannerAdapter(items)
    }

    class PlannerAdapter(private val items: List<PlannerDay>) : RecyclerView.Adapter<PlannerAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val day: TextView = view.findViewById(R.id.tvDayBadge)
            val title: TextView = view.findViewById(R.id.tvDayTitle)
            val desc: TextView = view.findViewById(R.id.tvDayDescription)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_planner_day, parent, false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.day.text = item.day
            holder.title.text = item.title
            holder.desc.text = item.description
        }
        override fun getItemCount() = items.size
    }
}
