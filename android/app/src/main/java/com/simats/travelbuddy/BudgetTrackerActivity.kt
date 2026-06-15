package com.simats.travelbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BudgetTrackerActivity : AppCompatActivity() {
    private lateinit var tripTitle: String
    private lateinit var adapter: ExpenseAdapter
    private var expenseList = mutableListOf<ExpenseItem>()
    private var totalBudget: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget_tracker)

        tripTitle = intent.getStringExtra("TRIP_TITLE") ?: "General"
        
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        
        setupRecyclerView()
        loadBudgetData()

        findViewById<View>(R.id.ivEditBudget).setOnClickListener { showSetLimitDialog() }
        findViewById<View>(R.id.btnAddExpense).setOnClickListener { showAddExpenseDialog() }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvExpenses)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ExpenseAdapter(expenseList) { item ->
            deleteExpense(item)
        }
        rv.adapter = adapter
    }

    private fun loadBudgetData() {
        RetrofitClient.instance.getBudgetInfo(tripTitle).enqueue(object : Callback<BudgetResponse> {
            override fun onResponse(call: Call<BudgetResponse>, response: Response<BudgetResponse>) {
                response.body()?.let {
                    totalBudget = it.total_budget
                    expenseList.clear()
                    expenseList.addAll(it.expenses)
                    
                    if (totalBudget <= 0) {
                        showSetLimitDialog()
                    }
                    updateUI()
                }
            }
            override fun onFailure(call: Call<BudgetResponse>, t: Throwable) {}
        })
    }

    private fun updateUI() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            val spent = expenseList.sumOf { it.amount }
            val remaining = totalBudget - spent
            val percent = if (totalBudget > 0) (spent * 100 / totalBudget).toInt() else 0

            findViewById<TextView>(R.id.tvTotalBudgetLabel).text = "Budget: ₹${totalBudget.toInt()}"
            findViewById<TextView>(R.id.tvTotalSpent).text = "₹${spent.toInt()}"
            findViewById<TextView>(R.id.tvRemainingBalance).text = "₹${remaining.toInt()} left"
            findViewById<TextView>(R.id.tvPercentUsed).text = "$percent% used"
            
            val pb = findViewById<LinearProgressIndicator>(R.id.pbBudget)
            pb.progress = if (percent > 100) 100 else percent
            
            // Visual warning if over budget
            if (remaining < 0) {
                findViewById<TextView>(R.id.tvRemainingBalance).setTextColor(resources.getColor(android.R.color.holo_red_light))
            } else {
                findViewById<TextView>(R.id.tvRemainingBalance).setTextColor(resources.getColor(R.color.onboarding_blue))
            }
        }
    }

    private fun showSetLimitDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Budget Limit")
        
        val input = EditText(this)
        input.hint = "Enter amount in ₹"
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        val padding = (24 * resources.displayMetrics.density).toInt()
        val container = FrameLayout(this)
        container.addView(input)
        input.setPadding(padding, padding / 2, padding, padding / 2)
        builder.setView(container)

        builder.setPositiveButton("Set") { _, _ ->
            val limit = input.text.toString().toDoubleOrNull() ?: 0.0
            if (limit > 0) {
                setBudgetLimit(limit)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun showAddExpenseDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
        builder.setTitle("Add Expense")

        val dialog = builder.create()
        
        dialogView.findViewById<Button>(R.id.btnSaveExpense).setOnClickListener {
            val amount = dialogView.findViewById<EditText>(R.id.etExpenseAmount).text.toString().toDoubleOrNull() ?: 0.0
            val note = dialogView.findViewById<EditText>(R.id.etExpenseNote).text.toString()
            
            if (amount > 0 && note.isNotEmpty()) {
                addExpense(amount, note)
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Please enter details", Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }

    private fun setBudgetLimit(limit: Double) {
        RetrofitClient.instance.setBudgetLimit(tripTitle, limit).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                loadBudgetData()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun addExpense(amount: Double, note: String) {
        RetrofitClient.instance.addExpense(tripTitle, amount, note).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                loadBudgetData()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun deleteExpense(item: ExpenseItem) {
        RetrofitClient.instance.deleteExpense(tripTitle, item.id).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                loadBudgetData()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    class ExpenseAdapter(private val items: List<ExpenseItem>, private val onDelete: (ExpenseItem) -> Unit) :
        RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val note: TextView = view.findViewById(R.id.tvExpenseNote)
            val date: TextView = view.findViewById(R.id.tvExpenseDate)
            val amount: TextView = view.findViewById(R.id.tvExpenseAmount)
            val deleteBtn: ImageView = view.findViewById(R.id.ivDeleteExpense)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.note.text = item.note
            holder.date.text = item.created_at
            holder.amount.text = "-₹${item.amount.toInt()}"
            
            holder.deleteBtn.setOnClickListener { onDelete(item) }
        }

        override fun getItemCount() = items.size
    }
}
