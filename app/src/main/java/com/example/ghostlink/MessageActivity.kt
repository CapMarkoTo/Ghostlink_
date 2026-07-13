package com.example.ghostlink

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

// Модель сообщения
data class Message(
    val text: String? = null,
    val image: Bitmap? = null,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MessageActivity : AppCompatActivity() {

    // Переименовали адаптер в ChatMessageAdapter
    private lateinit var adapter: ChatMessageAdapter
    private val messages = mutableListOf<Message>()
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = true

    private lateinit var deviceNameTitle: TextView
    private lateinit var messageInput: EditText
    private lateinit var btnSend: MaterialButton
    private lateinit var btnOpenDrawing: MaterialButton
    private lateinit var drawingContainer: LinearLayout

    private var currentRemoteName: String = "Подключение..."

    private val TYPE_TEXT: Byte = 0x01
    private val TYPE_IMAGE: Byte = 0x02
    private val TYPE_NAME: Byte = 0x03

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

        deviceNameTitle = findViewById(R.id.deviceNameTitle)
        messageInput = findViewById(R.id.messageInput)
        btnSend = findViewById(R.id.btnSend)
        btnOpenDrawing = findViewById(R.id.btnOpenDrawing)
        drawingContainer = findViewById(R.id.drawingContainer)

        val listView = findViewById<ListView>(R.id.chatListView)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnSendDrawing = findViewById<Button>(R.id.btnSendDrawing)
        val btnEraser = findViewById<MaterialButton>(R.id.btnEraser)

        // Инициализируем с новым именем класса
        adapter = ChatMessageAdapter(this, messages)
        listView.adapter = adapter

        val socket = BluetoothService.connectedSocket

        if (socket != null && socket.isConnected) {
            try {
                deviceNameTitle.text = currentRemoteName
                outputStream = socket.outputStream
                inputStream = socket.inputStream
                listenForMessages()
                sendMyGhostName()
            } catch (e: Exception) {
                deviceNameTitle.text = "Ошибка потоков"
            }
        } else {
            finish()
        }

        btnSend.setOnClickListener {
            val msg = messageInput.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendTextMessage(msg)
                messageInput.setText("")
            }
        }

        btnOpenDrawing.setOnClickListener {
            if (drawingContainer.visibility == View.GONE) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
                drawingContainer.visibility = View.VISIBLE
            } else {
                drawingContainer.visibility = View.GONE
            }
        }

        btnClear.setOnClickListener { drawingView.clearCanvas() }

        var isEraserActive = false
        btnEraser.setOnClickListener {
            isEraserActive = !isEraserActive
            drawingView.setEraserMode(isEraserActive)
            if (isEraserActive) {
                btnEraser.text = "Кисть"
                btnEraser.setIconResource(R.drawable.ic_draw)
            } else {
                btnEraser.text = "Ластик"
                btnEraser.setIconResource(R.drawable.ic_eraser)
            }
        }

        btnSendDrawing.setOnClickListener {
            val drawingBytes = drawingView.getCompressedByteArray()
            val bitmap = drawingView.getBitmap()
            if (drawingBytes != null && bitmap != null) {
                val staticBitmap = Bitmap.createBitmap(bitmap)
                sendImageMessage(drawingBytes, staticBitmap)
                drawingView.clearCanvas()
                drawingContainer.visibility = View.GONE
                isEraserActive = false
                drawingView.setEraserMode(false)
                btnEraser.text = "Ластик"
                btnEraser.setIconResource(R.drawable.ic_eraser)
            }
        }
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        runOnUiThread {
            if (isConnected) {
                deviceNameTitle.text = "Чат с: $currentRemoteName (В сети)"
                deviceNameTitle.setTextColor(resources.getColor(android.R.color.holo_green_dark, theme))
                messageInput.isEnabled = true
                btnSend.isEnabled = true
                btnOpenDrawing.isEnabled = true
            } else {
                deviceNameTitle.text = "Чат с: $currentRemoteName (Отключен)"
                deviceNameTitle.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))

                // Блокируем интерфейс ввода при обрыве
                messageInput.isEnabled = false
                btnSend.isEnabled = false
                btnOpenDrawing.isEnabled = false
                drawingContainer.visibility = View.GONE

                Toast.makeText(this, "Связь с собеседником потеряна", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendMyGhostName() {
        Thread {
            try {
                val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
                val myName = prefs.getString("ghost_name", Build.MODEL) ?: Build.MODEL
                val nameBytes = myName.toByteArray(Charsets.UTF_8)

                outputStream?.write(TYPE_NAME.toInt())
                outputStream?.write(nameBytes.size)
                outputStream?.write(nameBytes)
                outputStream?.flush()
            } catch (e: Exception) { }
        }.start()
    }

    private fun sendTextMessage(text: String) {
        Thread {
            try {
                val bytes = text.toByteArray(Charsets.UTF_8)
                outputStream?.write(TYPE_TEXT.toInt())
                outputStream?.write(bytes.size shr 8)
                outputStream?.write(bytes.size)
                outputStream?.write(bytes)
                outputStream?.flush()
                runOnUiThread { updateUI(Message(text = text, isMine = true)) }
            } catch (e: Exception) { }
        }.start()
    }

    private fun sendImageMessage(bytes: ByteArray, bitmap: Bitmap) {
        Thread {
            try {
                outputStream?.write(TYPE_IMAGE.toInt())
                val size = bytes.size
                outputStream?.write(size shr 24)
                outputStream?.write(size shr 16)
                outputStream?.write(size shr 8)
                outputStream?.write(size)
                outputStream?.write(bytes)
                outputStream?.flush()
                runOnUiThread { updateUI(Message(image = bitmap, isMine = true)) }
            } catch (e: Exception) { }
        }.start()
    }

    private fun listenForMessages() {
        Thread {
            // При успешном запуске чтения выставляем статус "В сети"
            updateConnectionStatus(true)

            while (isListening) {
                try {
                    val type = inputStream?.read() ?: -1
                    if (type == -1) {
                        updateConnectionStatus(false)
                        break
                    }

                    when (type.toByte()) {
                        TYPE_NAME -> {
                            val len = inputStream?.read() ?: 0
                            val buffer = ByteArray(len)
                            inputStream?.read(buffer)
                            val remoteName = String(buffer, Charsets.UTF_8)
                            currentRemoteName = remoteName
                            updateConnectionStatus(true)
                        }
                        TYPE_TEXT -> {
                            val len1 = inputStream?.read() ?: 0
                            val len2 = inputStream?.read() ?: 0
                            val len = (len1 shl 8) or len2
                            val buffer = ByteArray(len)
                            var totalRead = 0
                            while (totalRead < len) {
                                val r = inputStream?.read(buffer, totalRead, len - totalRead) ?: -1
                                if (r == -1) {
                                    updateConnectionStatus(false)
                                    return@Thread
                                }
                                totalRead += r
                            }
                            val text = String(buffer, Charsets.UTF_8)
                            runOnUiThread { updateUI(Message(text = text, isMine = false)) }
                        }
                        TYPE_IMAGE -> {
                            val s1 = inputStream?.read() ?: 0
                            val s2 = inputStream?.read() ?: 0
                            val s3 = inputStream?.read() ?: 0
                            val s4 = inputStream?.read() ?: 0
                            val size = (s1 shl 24) or (s2 shl 16) or (s3 shl 8) or s4

                            val buffer = ByteArray(size)
                            var bytesRead = 0
                            while (bytesRead < size) {
                                val result = inputStream?.read(buffer, bytesRead, size - bytesRead) ?: -1
                                if (result == -1) {
                                    updateConnectionStatus(false)
                                    return@Thread
                                }
                                bytesRead += result
                            }
                            val bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
                            runOnUiThread { updateUI(Message(image = bitmap, isMine = false)) }
                        }
                    }
                } catch (e: Exception) {
                    updateConnectionStatus(false)
                    break
                }
            }
        }.start()
    }

    private fun updateUI(msg: Message) {
        messages.add(msg)
        adapter.notifyDataSetChanged()
        findViewById<ListView>(R.id.chatListView).setSelection(messages.size - 1)
    }

    override fun onDestroy() {
        super.onDestroy()
        isListening = false
    }
}

// Теперь класс называется ChatMessageAdapter
class ChatMessageAdapter(context: Context, private val objects: List<Message>) :
    ArrayAdapter<Message>(context, R.layout.item_message, objects) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        val message = getItem(position)

        val textView = view.findViewById<TextView>(R.id.messageText)
        val imageView = view.findViewById<ImageView>(R.id.messageImage)
        val timeView = view.findViewById<TextView>(R.id.messageTime)
        val container = view.findViewById<LinearLayout>(R.id.messageContainer)
        val bubble = view.findViewById<LinearLayout>(R.id.bubbleBackground)

        if (message?.image != null) {
            textView.visibility = View.GONE
            imageView.visibility = View.VISIBLE
            imageView.setImageBitmap(message.image)
        } else {
            textView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            textView.text = message?.text
        }

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