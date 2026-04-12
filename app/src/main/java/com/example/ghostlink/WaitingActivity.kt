package com.example.ghostlink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.*

class WaitingActivity : AppCompatActivity() {

    private var serverThread: AcceptThread? = null
    private val MY_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val adapter = bluetoothManager?.adapter

        // 1. Отображаем имя устройства
        val nameView = findViewById<TextView>(R.id.deviceNameTextView)
        nameView.text = adapter?.name ?: "GhostLink Device"

        // 2. ЗАПРОС ВИДИМОСТИ (теперь здесь, чтобы окно всплыло поверх этого экрана)
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)

        // 3. Кнопка отмены
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            stopServer()
            finish()
        }

        // 4. Запуск сервера
        serverThread = AcceptThread(adapter)
        serverThread?.start()
    }

    @SuppressLint("MissingPermission")
    private inner class AcceptThread(val adapter: BluetoothAdapter?) : Thread() {

        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            try {
                adapter?.listenUsingRfcommWithServiceRecord("GhostLinkChat", MY_UUID)
            } catch (e: Exception) {
                null
            }
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }

                socket?.let {
                    handleConnection(it)
                    try {
                        mmServerSocket?.close()
                    } catch (e: IOException) { }
                    shouldLoop = false
                }
            }
        }
    }

    private fun handleConnection(socket: BluetoothSocket) {
        BluetoothService.connectedSocket = socket
        runOnUiThread {
            val intent = Intent(this, MessageActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun stopServer() {
        try {
            serverThread?.interrupt()
        } catch (e: Exception) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
    }
}