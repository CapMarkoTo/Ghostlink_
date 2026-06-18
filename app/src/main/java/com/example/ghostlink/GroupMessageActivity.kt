package com.example.ghostlink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GroupMessageActivity : AppCompatActivity() {

    private val messages = mutableListOf<String>()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)

        val rvMessages = findViewById<RecyclerView>(R.id.rvMessages)
        val etMessage = findViewById<EditText>(R.id.etMessage)
        val btnSend = findViewById<Button>(R.id.btnSend)

        // Инициализация адаптера с правильным списком
        adapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter

        // Подписываемся на входящие сообщения
        BluetoothGroupManager.onMessageReceived = { msg ->
            runOnUiThread {
                messages.add(msg)
                adapter.notifyItemInserted(messages.size - 1)
                rvMessages.scrollToPosition(messages.size - 1)
            }
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString()
            if (text.isNotEmpty()) {
                BluetoothGroupManager.broadcastMessage(text)
                messages.add("Вы: $text")
                adapter.notifyItemInserted(messages.size - 1)
                rvMessages.scrollToPosition(messages.size - 1)
                etMessage.text.clear()
            }
        }
    }
}

// --- Адаптер теперь определен правильно ---
class MessageAdapter(private val messages: List<String>) : RecyclerView.Adapter<MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        // Если у тебя нет специального layout для сообщения, используем стандартный системный
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.textView.text = messages[position]
    }

    override fun getItemCount(): Int = messages.size
}

class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    // android.R.id.text1 - это стандартный ID в simple_list_item_1
    val textView: TextView = view.findViewById(android.R.id.text1)
}