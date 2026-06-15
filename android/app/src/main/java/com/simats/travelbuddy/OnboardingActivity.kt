package com.simats.travelbuddy

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var tvSkip: TextView
    private lateinit var indicators: List<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_onboarding)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)
        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3)
        )

        val items = listOf(
            OnboardingItem(
                R.drawable.img_onboarding_1,
                R.drawable.ic_location,
                "Discover Amazing Places",
                "Explore destinations worldwide and plan your perfect getaway with AI-powered recommendations."
            ),
            OnboardingItem(
                R.drawable.img_onboarding_2,
                R.drawable.ic_calendar,
                "Smart Trip Planning",
                "Create detailed itineraries, manage bookings, and never miss a flight with intelligent reminders."
            ),
            OnboardingItem(
                R.drawable.img_onboarding_3,
                R.drawable.ic_wallet,
                "Budget Like a Pro",
                "Track expenses, set budgets, and get insights to make your travel money go further."
            )
        )

        viewPager.adapter = OnboardingAdapter(items)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateIndicators(position)
                if (position == items.size - 1) {
                    btnNext.text = "Get Started"
                } else {
                    btnNext.text = "Next"
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem < items.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                navigateToLogin()
            }
        }

        tvSkip.setOnClickListener {
            navigateToLogin()
        }
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, view ->
            val layoutParams = view.layoutParams as LinearLayout.LayoutParams
            if (index == position) {
                view.setBackgroundResource(R.drawable.bg_indicator_active)
                layoutParams.width = (24 * resources.displayMetrics.density).toInt()
                layoutParams.height = (6 * resources.displayMetrics.density).toInt()
            } else {
                view.setBackgroundResource(R.drawable.bg_indicator_inactive)
                layoutParams.width = (8 * resources.displayMetrics.density).toInt()
                layoutParams.height = (8 * resources.displayMetrics.density).toInt()
            }
            view.layoutParams = layoutParams
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
