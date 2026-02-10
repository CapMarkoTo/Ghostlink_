package com.example.ghostlink

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ghostlink.data.AppDatabase
import com.example.ghostlink.data.Message
import com.example.ghostlink.ui.MessageAdapter
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var messageAdapter: MessageAdapter
    private val messageList = ArrayList<Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Инициализируем базу данных
        db = AppDatabase.getInstance(this)

        // 2. Настраиваем RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerViewMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(messageList)
        recyclerView.adapter = messageAdapter

        // 3. Загружаем сообщения из БД
        loadMessages()

        // 4. Настраиваем кнопку отправки
        val editText: EditText = findViewById(R.id.editTextMessage)
        val sendButton: ImageButton = findViewById(R.id.buttonSend)

        sendButton.setOnClickListener {
            val text = editText.text.toString()
            if (text.isNotEmpty()) {
                val newMessage = Message(text, System.currentTimeMillis(), true)

                // Сохраняем в БД (в отдельном потоке!)
                Thread {
                    db.messageDao().insert(newMessage)
                    // Обновляем UI на главном потоке
                    runOnUiThread {
                        messageList.add(newMessage)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        recyclerView.scrollToPosition(messageList.size - 1)
                        editText.setText("")
                    }
                }.start()
            }
        }
    }

    private fun loadMessages() {
        Thread {
            val messages = db.messageDao().getAllMessages()
            runOnUiThread {
                messageList.clear()
                messageList.addAll(messages)
                messageAdapter.notifyDataSetChanged()
            }
        }.start()
    }
}