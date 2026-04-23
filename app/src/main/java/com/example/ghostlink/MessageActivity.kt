package com.example.ghostlink

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

// Обновленная модель: теперь поддерживает и текст, и картинку
data class Message(
    val text: String? = null,
    val image: Bitmap? = null,
    val isMine: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class MessageActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var isListening = true

    // Маркеры типов сообщений
    private val TYPE_TEXT: Byte = 0x01
    private val TYPE_IMAGE: Byte = 0x02

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

        val deviceNameTitle = findViewById<TextView>(R.id.deviceNameTitle)
        val listView = findViewById<ListView>(R.id.chatListView)
        val input = findViewById<EditText>(R.id.messageInput)
        val btnSend = findViewById<MaterialButton>(R.id.btnSend)

        val btnOpenDrawing = findViewById<MaterialButton>(R.id.btnOpenDrawing)
        val drawingContainer = findViewById<LinearLayout>(R.id.drawingContainer)
        val drawingView = findViewById<DrawingView>(R.id.drawingView)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnSendDrawing = findViewById<Button>(R.id.btnSendDrawing)

        adapter = MessageAdapter(this, messages)
        listView.adapter = adapter

        val socket = BluetoothService.connectedSocket

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

        // Отправка текста
        btnSend.setOnClickListener {
            val msg = input.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendTextMessage(msg)
                input.setText("")
            }
        }

        // Рисовалка
        btnOpenDrawing.setOnClickListener {
            drawingContainer.visibility = if (drawingContainer.visibility == View.GONE) View.VISIBLE else View.GONE
        }

        btnClear.setOnClickListener { drawingView.clearCanvas() }

        // Отправка рисунка
        btnSendDrawing.setOnClickListener {
            val drawingBytes = drawingView.getCompressedByteArray()
            val bitmap = drawingView.getBitmap()
            if (drawingBytes != null && bitmap != null) {
                // Создаем копию битмапа для списка, так как холст будет очищен
                val staticBitmap = Bitmap.createBitmap(bitmap)
                sendImageMessage(drawingBytes, staticBitmap)
                drawingView.clearCanvas()
                drawingContainer.visibility = View.GONE
            }
        }
    }

    private fun sendTextMessage(text: String) {
        Thread {
            try {
                val bytes = text.toByteArray(Charsets.UTF_8)
                outputStream?.write(TYPE_TEXT.toInt())
                outputStream?.write(bytes.size shr 8) // Длина (байт 1)
                outputStream?.write(bytes.size)       // Длина (байт 2)
                outputStream?.write(bytes)
                outputStream?.flush()

                runOnUiThread {
                    messages.add(Message(text = text, isMine = true))
                    adapter.notifyDataSetChanged()
                    findViewById<ListView>(R.id.chatListView).setSelection(messages.size - 1)
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun sendImageMessage(bytes: ByteArray, bitmap: Bitmap) {
        Thread {
            try {
                outputStream?.write(TYPE_IMAGE.toInt())
                // Отправляем размер массива байтов (4 байта для надежности)
                val size = bytes.size
                outputStream?.write(size shr 24)
                outputStream?.write(size shr 16)
                outputStream?.write(size shr 8)
                outputStream?.write(size)

                outputStream?.write(bytes)
                outputStream?.flush()

                runOnUiThread {
                    messages.add(Message(image = bitmap, isMine = true))
                    adapter.notifyDataSetChanged()
                    findViewById<ListView>(R.id.chatListView).setSelection(messages.size - 1)
                }
            } catch (e: Exception) { }
        }.start()
    }

    private fun listenForMessages() {
        Thread {
            while (isListening) {
                try {
                    val type = inputStream?.read() ?: -1
                    if (type == -1) break

                    if (type == TYPE_TEXT.toInt()) {
                        val len1 = inputStream?.read() ?: 0
                        val len2 = inputStream?.read() ?: 0
                        val len = (len1 shl 8) or len2
                        val buffer = ByteArray(len)
                        inputStream?.read(buffer)
                        val text = String(buffer, Charsets.UTF_8)
                        runOnUiThread { updateUI(Message(text = text, isMine = false)) }
                    } else if (type == TYPE_IMAGE.toInt()) {
                        val s1 = inputStream?.read() ?: 0
                        val s2 = inputStream?.read() ?: 0
                        val s3 = inputStream?.read() ?: 0
                        val s4 = inputStream?.read() ?: 0
                        val size = (s1 shl 24) or (s2 shl 16) or (s3 shl 8) or s4

                        val buffer = ByteArray(size)
                        var bytesRead = 0
                        while (bytesRead < size) {
                            val result = inputStream?.read(buffer, bytesRead, size - bytesRead) ?: -1
                            if (result == -1) break
                            bytesRead += result
                        }
                        val bitmap = BitmapFactory.decodeByteArray(buffer, 0, buffer.size)
                        runOnUiThread { updateUI(Message(image = bitmap, isMine = false)) }
                    }
                } catch (e: Exception) { break }
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

class MessageAdapter(context: Context, private val objects: List<Message>) :
    ArrayAdapter<Message>(context, R.layout.item_message, objects) {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)
        val message = getItem(position)

        val textView = view.findViewById<TextView>(R.id.messageText)
        val imageView = view.findViewById<ImageView>(R.id.messageImage) // Нужно добавить в XML
        val timeView = view.findViewById<TextView>(R.id.messageTime)
        val container = view.findViewById<LinearLayout>(R.id.messageContainer)
        val bubble = view.findViewById<LinearLayout>(R.id.bubbleBackground)

        // Логика отображения: Текст или Картинка
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