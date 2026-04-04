package com.example.ghostlink

import android.bluetooth.BluetoothSocket
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream

class MessageActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private val messages = mutableListOf<String>()

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val listView = findViewById<ListView>(R.id.chatListView)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<Button>(R.id.btnSend)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, messages)
        listView.adapter = adapter

        // Получаем сокет из нашего сервиса
        val socket = BluetoothService.connectedSocket

        if (socket != null && socket.isConnected) {
            try {
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                // Запускаем прослушку в отдельном потоке
                listenForMessages()
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка потоков: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Соединение не найдено", Toast.LENGTH_LONG).show()
            finish() // Закрываем экран, если сокета нет
        }

        btnSend.setOnClickListener {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                input.setText("")
            }
        }
    }

    private fun sendMessage(message: String) {
        Thread {
            try {
                val bytesToSend = message.toByteArray(Charsets.UTF_8)
                outputStream?.write(bytesToSend)
                outputStream?.flush() // Это "пинок" для байтов, чтобы они улетели

                runOnUiThread {
                    messages.add("Вы: $message")
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }

    private fun listenForMessages() {
        Thread {
            val buffer = ByteArray(1024)
            while (isListening) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val incomingMsg = String(buffer, 0, bytes)
                        runOnUiThread {
                            messages.add("Собеседник: $incomingMsg")
                            adapter.notifyDataSetChanged()
                        }
                    }
                } catch (e: Exception) {
                    isListening = false
                    break
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
        // Закрывать сокет здесь не будем, чтобы не прервать связь при повороте экрана
    }
}