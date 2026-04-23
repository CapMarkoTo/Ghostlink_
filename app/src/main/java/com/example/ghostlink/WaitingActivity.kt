package com.example.ghostlink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
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

    // Переменная для хранения оригинального имени телефона
    private var originalDeviceName: String? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_waiting)

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        // --- ЛОГИКА ИМЕНИ ПРИЗРАКА ---

        // 1. Достаем сохраненное имя из настроек
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val ghostName = prefs.getString("ghost_name", "GhostLink User")

        // 2. Запоминаем старое имя и ставим новое
        bluetoothAdapter?.let { adapter ->
            originalDeviceName = adapter.name // Сохраняем "Samsung" или "Xiaomi"
            adapter.name = ghostName          // Теперь телефон в сети виден как твой ник
        }

        // 3. Отображаем имя в UI
        val nameView = findViewById<TextView>(R.id.deviceNameTextView)
        nameView.text = "Ваш профиль: $ghostName"

        // --- ОСТАЛЬНАЯ ЛОГИКА ---

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)

        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            restoreOriginalName()
            stopServer()
            finish()
        }

        serverThread = AcceptThread(bluetoothAdapter)
        serverThread?.start()
    }

    @SuppressLint("MissingPermission")
    private fun restoreOriginalName() {
        // Возвращаем телефону его исходное имя
        if (originalDeviceName != null) {
            bluetoothAdapter?.name = originalDeviceName
        }
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
        // Если мы закрываем экран, не забываем вернуть имя телефону
        restoreOriginalName()
        stopServer()
    }
}