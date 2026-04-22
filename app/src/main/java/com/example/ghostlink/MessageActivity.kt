package com.example.ghostlink

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.content.Context
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

data class Message(
    val text: String,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MessageActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = true

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, maxOf(systemBars.bottom, imeInsets.bottom))
            insets
        }

        // Элементы управления
        val deviceNameTitle = findViewById<TextView>(R.id.deviceNameTitle)
        val listView = findViewById<ListView>(R.id.chatListView)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        // PictoChat элементы
        val btnOpenDrawing = findViewById<MaterialButton>(R.id.btnOpenDrawing)
        val drawingContainer = findViewById<LinearLayout>(R.id.drawingContainer)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnSendDrawing = findViewById<Button>(R.id.btnSendDrawing)

        adapter = MessageAdapter(this, messages)
        listView.adapter = adapter

        val socket = BluetoothService.connectedSocket

        // Настройка Bluetooth потоков
        if (socket != null && socket.isConnected) {
            try {
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
            }
        } else {
            finish()
        }

        // --- ЛОГИКА КНОПОК ---

        // Отправка текста
        btnSend.setOnClickListener {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessage(msg)
                input.setText("")
            }
        }

        // Открыть/закрыть рисовалку
        btnOpenDrawing.setOnClickListener {
            if (drawingContainer.visibility == View.GONE) {
                drawingContainer.visibility = View.VISIBLE
            } else {
                drawingContainer.visibility = View.GONE
            }
        }

        // Очистить холст
        btnClear.setOnClickListener {
            drawingView.clearCanvas()
        }

        // Отправить рисунок
        btnSendDrawing.setOnClickListener {
            // Пока отправляем просто текст, завтра научим передавать байты картинки
            sendMessage("[Рисованное послание]")
            drawingView.clearCanvas()
            drawingContainer.visibility = View.GONE
        }
    }

    private fun sendMessage(message: String) {
        Thread {
            try {
                val bytesToSend = message.toByteArray(Charsets.UTF_8)
                outputStream?.write(bytesToSend)
                outputStream?.flush()

                runOnUiThread {
                    messages.add(Message(message, true))
                    adapter.notifyDataSetChanged()
                    findViewById<ListView>(R.id.chatListView).setSelection(messages.size - 1)
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
                            messages.add(Message(incomingMsg, false))
                            adapter.notifyDataSetChanged()
                            findViewById<ListView>(R.id.chatListView).setSelection(messages.size - 1)
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

class MessageAdapter(context: Context, private val objects: List<Message>) :
    ArrayAdapter<Message>(context, R.layout.item_message, objects) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        val message = getItem(position)

        val textView = view.findViewById<TextView>(R.id.messageText)
        val timeView = view.findViewById<TextView>(R.id.messageTime)
        val container = view.findViewById<LinearLayout>(R.id.messageContainer)
        val bubble = view.findViewById<LinearLayout>(R.id.bubbleBackground)

        textView.text = message?.text
        timeView.text = if (message != null) timeFormat.format(Date(message.timestamp)) else ""

        if (message?.isMine == true) {
            container.gravity = Gravity.END
            val params = bubble.layoutParams as LinearLayout.LayoutParams
            params.marginStart = 60
            params.marginEnd = 0
            bubble.layoutParams = params
            bubble.setBackgroundResource(R.drawable.bg_message_out)
        } else {
            container.gravity = Gravity.START
            val params = bubble.layoutParams as LinearLayout.LayoutParams
            params.marginEnd = 60
            params.marginStart = 0
            bubble.layoutParams = params
            bubble.setBackgroundResource(R.drawable.bg_message_in)
        }
        return view
    }
}