package com.simats.travelbuddy

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

data class BudgetData(val min: Int, val max: Int, val travel: String, val food: String, val stay: String, val extra: String)

class BudgetPlanningActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget_planning)

        val destination = intent.getStringExtra("DESTINATION") ?: "Mahabalipuram"
        findViewById<TextView>(R.id.tvBudgetHeader).text = "Budget for $destination"

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        calculateBudget(destination)
    }

    private fun calculateBudget(destination: String) {
        val data = when {
            destination.contains("Ooty", true) -> BudgetData(9000, 28000, "₹2,500 - ₹6,000", "₹2,000 - ₹5,000", "₹3,500 - ₹15,000", "₹1,000 - ₹2,000")
            destination.contains("Pondicherry", true) -> BudgetData(7500, 24000, "₹1,500 - ₹4,000", "₹2,500 - ₹7,000", "₹2,500 - ₹11,000", "₹1,000 - ₹2,000")
            destination.contains("Madurai", true) -> BudgetData(6000, 18000, "₹1,200 - ₹3,000", "₹1,500 - ₹4,000", "₹2,500 - ₹9,000", "₹800 - ₹2,000")
            destination.contains("Mahabalipuram", true) -> BudgetData(5000, 16000, "₹800 - ₹2,500", "₹1,500 - ₹4,500", "₹2,000 - ₹8,000", "₹700 - ₹1,000")
            else -> BudgetData(5000, 15000, "₹1,000 - ₹3,000", "₹1,500 - ₹4,000", "₹2,000 - ₹7,000", "₹500 - ₹1,000")
        }

        findViewById<TextView>(R.id.tvTotalMin).text = "₹${data.min}"
        findViewById<TextView>(R.id.tvTotalMax).text = "₹${data.max}"

        findViewById<TextView>(R.id.tvTravelCost).text = data.travel
        findViewById<TextView>(R.id.tvFoodCost).text = data.food
        findViewById<TextView>(R.id.tvStayCost).text = data.stay
        findViewById<TextView>(R.id.tvExtraCost).text = data.extra
    }
}
