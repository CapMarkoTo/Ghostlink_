package com.example.ghostlink

import android.view.Gravity
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import com.example.ghostlink.R
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.InputStream
import java.io.OutputStream

class MessageActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter // Было ArrayAdapter<String>
    private val messages = mutableListOf<Message>() // Было mutableListOf<String>()
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Настройки экрана
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        // Настройка отступов для выреза камеры и КЛАВИАТУРЫ
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            // Верх — системный бар, низ — максимум между баром и клавиатурой
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(systemBars.bottom, imeInsets.bottom))
            insets
        }

        // Инициализация UI
        val deviceNameTitle = findViewById<TextView>(R.id.deviceNameTitle)
        val listView = findViewById<ListView>(R.id.chatListView)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<Button>(R.id.btnSend)

        // Инициализация Адаптера
        adapter = MessageAdapter(this, R.layout.item_message, messages)
        listView.adapter = adapter

        // Работа с Bluetooth
        val socket = BluetoothService.connectedSocket

        if (socket != null && socket.isConnected) {
            // Установка имени устройства в заголовок
            try {
                // Проверка разрешения для Android 12+
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    deviceNameTitle.text = "Чат: ${socket.remoteDevice.name ?: "Устройство"}"
                } else {
                    deviceNameTitle.text = "Чат: ${socket.remoteDevice.address}"
                }

                outputStream = socket.outputStream
                inputStream = socket.inputStream
                listenForMessages()
            } catch (e: Exception) {
                deviceNameTitle.text = "Чат: Ошибка"
                Toast.makeText(this, "Ошибка потоков: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Соединение не найдено", Toast.LENGTH_LONG).show()
            finish()
        }

        btnSend.setOnClickListener {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                input.setText("")
            }
        }
    }
    // Отправка Сообщений
    private fun sendMessage(message: String) {
        Thread {
            try {
                val bytesToSend = message.toByteArray(Charsets.UTF_8)
                outputStream?.write(bytesToSend)
                outputStream?.flush()

                runOnUiThread {
                    // ТУТ ИЗМЕНЕНИЕ: создаем объект Message вместо строки
                    messages.add(Message(message, true))
                    adapter.notifyDataSetChanged()

                    val listView = findViewById<ListView>(R.id.chatListView)
                    listView.setSelection(messages.size - 1)
                }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show() }
            }
        }.start()
    }
    // Получение Сообщений
    private fun listenForMessages() {
        Thread {
            val buffer = ByteArray(1024)
            while (isListening) {
                try {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val incomingMsg = String(buffer, 0, bytes)
                        runOnUiThread {
                            // ТУТ ИЗМЕНЕНИЕ: создаем объект Message с пометкой "чужой"
                            messages.add(Message(incomingMsg, false))
                            adapter.notifyDataSetChanged()

                            val listView = findViewById<ListView>(R.id.chatListView)
                            listView.setSelection(messages.size - 1)
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
    }
}
data class Message(val text: String, val isMine: Boolean)
class MessageAdapter(context: android.content.Context, resource: Int, objects: List<Message>) :
    ArrayAdapter<Message>(context, resource, objects) {

    override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
        val view = convertView ?: android.view.LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        val message = getItem(position)
        val textView = view.findViewById<TextView>(R.id.messageText)
        val container = view.findViewById<LinearLayout>(R.id.messageContainer)

        textView.text = message?.text

        if (message?.isMine == true) {
            container.gravity = android.view.Gravity.END
            textView.setBackgroundResource(R.drawable.bg_message_out)
        } else {
            container.gravity = android.view.Gravity.START
            textView.setBackgroundResource(R.drawable.bg_message_in)
        }

        return view
    }
}