package com.simats.travelbuddy

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PackingListActivity : AppCompatActivity() {
    private lateinit var tripTitle: String
    private lateinit var adapter: PackingAdapter
    private var itemList = mutableListOf<PackingItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_packing_list)

        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val addItemBar = findViewById<View>(R.id.cvAddItemBar)
        val rlHeader = findViewById<View>(R.id.rlHeader)
        val density = resources.displayMetrics.density
        val margin16 = (16 * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            
            val params = addItemBar.layoutParams as ViewGroup.MarginLayoutParams
            params.setMargins(margin16, margin16, margin16, margin16 + bottomInset)
            addItemBar.layoutParams = params
            
            rlHeader.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            insets
        }

        tripTitle = intent.getStringExtra("TRIP_TITLE") ?: "General"
        
        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.tvReady).text = tripTitle

        setupRecyclerView()
        loadPackingItems()

        findViewById<View>(R.id.fabAddItem).setOnClickListener {
            val et = findViewById<EditText>(R.id.etNewItem)
            val name = et.text.toString()
            if (name.isNotEmpty()) {
                addItem(name)
                et.text.clear()
            }
        }
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvPackingList)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = PackingAdapter(itemList, 
            onToggle = { item -> toggleItem(item) },
            onDelete = { item -> deleteItem(item) }
        )
        rv.adapter = adapter
    }

    private fun loadPackingItems() {
        RetrofitClient.instance.getPackingItems(tripTitle).enqueue(object : Callback<PackingListResponse> {
            override fun onResponse(call: Call<PackingListResponse>, response: Response<PackingListResponse>) {
                response.body()?.data?.let {
                    itemList.clear()
                    itemList.addAll(it)
                    updateUI()
                }
            }
            override fun onFailure(call: Call<PackingListResponse>, t: Throwable) {
                Toast.makeText(this@PackingListActivity, "Failed to load", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateUI() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
            val total = itemList.size
            val packed = itemList.count { it.is_packed }
            val percent = if (total > 0) (packed * 100) / total else 0

            findViewById<TextView>(R.id.tvPackedStatus).text = "$packed of $total items packed"
            findViewById<TextView>(R.id.tvPercentage).text = "$percent%"
            findViewById<CircularProgressIndicator>(R.id.pbPacking).progress = percent
        }
    }

    private fun addItem(name: String) {
        RetrofitClient.instance.addPackingItem(tripTitle, name, "Personal").enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                loadPackingItems()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun toggleItem(item: PackingItem) {
        val newStatus = if (item.is_packed) 1 else 0
        RetrofitClient.instance.togglePackingItem(tripTitle, item.id, newStatus).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                updateUI()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    private fun deleteItem(item: PackingItem) {
        RetrofitClient.instance.deletePackingItem(tripTitle, item.id).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                loadPackingItems()
            }
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {}
        })
    }

    class PackingAdapter(
        private val items: List<PackingItem>, 
        private val onToggle: (PackingItem) -> Unit,
        private val onDelete: (PackingItem) -> Unit
    ) : RecyclerView.Adapter<PackingAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val name: TextView = view.findViewById(R.id.tvItemName)
            val checkbox: CheckBox = view.findViewById(R.id.cbPacked)
            val deleteBtn: ImageView = view.findViewById(R.id.ivDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_packing, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.name.text = item.item_name
            
            holder.checkbox.setOnCheckedChangeListener(null)
            holder.checkbox.isChecked = item.is_packed
            updateTextStyle(holder.name, item.is_packed)

            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                item.is_packed = isChecked
                updateTextStyle(holder.name, isChecked)
                onToggle(item)
            }

            holder.deleteBtn.setOnClickListener {
                onDelete(item)
            }
        }

        private fun updateTextStyle(tv: TextView, isChecked: Boolean) {
            if (isChecked) {
                tv.paintFlags = tv.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                tv.alpha = 0.5f
            } else {
                tv.paintFlags = tv.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                tv.alpha = 1.0f
            }
        }

        override fun getItemCount() = items.size
    }
}
