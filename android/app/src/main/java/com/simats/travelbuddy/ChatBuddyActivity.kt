package com.simats.travelbuddy

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

data class ChatMessage(val text: String, val isUser: Boolean)

class ChatBuddyActivity : AppCompatActivity() {

    private lateinit var rvChat: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: View
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_buddy)

        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        val llInputArea = findViewById<View>(R.id.llInputArea)
        val rlToolbar = findViewById<View>(R.id.rlToolbar)
        val density = resources.displayMetrics.density
        val padding16 = (16 * density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomInset = if (imeInsets.bottom > 0) imeInsets.bottom else systemBars.bottom
            llInputArea.setPadding(padding16, padding16, padding16, padding16 + bottomInset)
            
            rlToolbar.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            
            insets
        }

        rvChat = findViewById(R.id.rvChat)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        chatAdapter = ChatAdapter(messages)
        rvChat.adapter = chatAdapter

        // Initial Greeting
        addMessage("Hello! I'm your Travel Buddy. Ask me anything about destinations, hotels, or travel tips!", false)

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addMessage(text, true)
                etMessage.setText("")
                askAI(text)
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(ChatMessage(text, isUser))
        chatAdapter.notifyItemInserted(messages.size - 1)
        rvChat.scrollToPosition(messages.size - 1)
    }

    private fun askAI(question: String) {
        val request = ChatRequest(question)
        RetrofitClient.instance.askBuddy(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    val answer = response.body()?.answer ?: "Sorry, I couldn't find an answer."
                    runOnUiThread { addMessage(answer, false) }
                } else {
                    runOnUiThread { addMessage("Error: ${response.code()}. Make sure your API key is set in the backend!", false) }
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                runOnUiThread { addMessage("Network Failure: ${t.message}", false) }
            }
        })
    }

    class ChatAdapter(private val items: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: LinearLayout = view.findViewById(R.id.llChatContainer)
            val card: MaterialCardView = view.findViewById(R.id.cvMessage)
            val text: TextView = view.findViewById(R.id.tvMessage)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.text
            
            val params = holder.card.layoutParams as LinearLayout.LayoutParams
            if (item.isUser) {
                params.gravity = Gravity.END
                holder.card.setCardBackgroundColor(android.graphics.Color.parseColor("#3B82F6")) // Blue
                holder.text.setTextColor(android.graphics.Color.WHITE)
            } else {
                params.gravity = Gravity.START
                holder.card.setCardBackgroundColor(android.graphics.Color.parseColor("#2D3748")) // Dark gray
                holder.text.setTextColor(android.graphics.Color.WHITE)
            }
            holder.card.layoutParams = params
        }

        override fun getItemCount() = items.size
    }
}
