package com.example.ghostlink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View // Добавлено
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge // Добавлено
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat // Добавлено
import androidx.core.view.WindowInsetsCompat // Добавлено
import android.bluetooth.BluetoothSocket
import java.util.UUID

class DeviceListActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    private val MY_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. ВКЛЮЧАЕМ EDGE-TO-EDGE
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        // 2. НАСТРОЙКА ОТСТУПОВ (ИНСЕТОВ)
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Делаем отступ сверху чуть больше (на 20 пикселей), чтобы текст не прилипал к камере
            v.setPadding(systemBars.left, systemBars.top + 20, systemBars.right, systemBars.bottom)
            insets
        }

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter
        val listView = findViewById<ListView>(R.id.devicesListView)
        deviceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = deviceAdapter

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        startScanning()

        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedDevice = discoveredDevices[position]
            Toast.makeText(this, "Подключение к ${selectedDevice.name}...", Toast.LENGTH_SHORT).show()
            ConnectThread(selectedDevice).start()
        }
    }
    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if (ActivityCompat.checkSelfPermission(this@DeviceListActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                device.createRfcommSocketToServiceRecord(MY_UUID)
            } else null
        }

        override fun run() {
            // Останавливаем поиск, чтобы не мешать соединению
            if (ActivityCompat.checkSelfPermission(this@DeviceListActivity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothAdapter?.cancelDiscovery()
            }

            try {
                mmSocket?.connect()
                // ЕСЛИ МЫ ЗДЕСЬ — СОЕДИНЕНИЕ УСТАНОВЛЕНО!
                handleSuccessConnection(mmSocket!!)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@DeviceListActivity, "Ошибка подключения: ${e.message}", Toast.LENGTH_LONG).show()
                }
                try { mmSocket?.close() } catch (e2: Exception) {}
            }
        }
    }

    private fun handleSuccessConnection(socket: BluetoothSocket) {
        // Сохраняем сокет в глобальный сервис
        BluetoothService.connectedSocket = socket

        runOnUiThread {
            val intent = Intent(this, MessageActivity::class.java)
            // Флаг, чтобы очистить историю переходов и чат был "чистым"
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }
    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter?.startDiscovery()
        } else {
            Toast.makeText(this, "Нет разрешений для поиска", Toast.LENGTH_SHORT).show()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && device.name != null) {
                    // Проверяем, нет ли уже такого устройства в списке
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device)
                        deviceNames.add(device.name)
                        deviceAdapter.notifyDataSetChanged() // Обновляем список на экране
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Обязательно выключаем поиск и приемник
        try {
            bluetoothAdapter?.cancelDiscovery()
            unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }
}