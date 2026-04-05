package com.example.ghostlink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.UUID
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null

    // Список необходимых разрешений
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    companion object {
        private const val NAME = "GhostLinkBT"
        private val MY_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Включаем режим "от края до края" правильно
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // Добавляем отступы, чтобы контент не залезал под часы и камеру
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Системные акцентные цвета
        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(application)

        setContentView(R.layout.activity_main)

        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter

        findViewById<Button>(R.id.btnHost).setOnClickListener {
            checkPermissionsAndRun {
                // Делаем устройство видимым
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                    putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                }
                startActivity(discoverableIntent)

                // Запускаем сервер
                AcceptThread().start()
                Toast.makeText(this, "Ожидание подключений...", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnJoin).setOnClickListener {
            checkPermissionsAndRun {
                startActivity(Intent(this, DeviceListActivity::class.java))
            }
        }
    }

    private fun checkPermissionsAndRun(action: () -> Unit) {
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            action()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    private inner class AcceptThread : Thread() {
        @SuppressLint("MissingPermission")
        override fun run() {
            val mmServerSocket: BluetoothServerSocket? = try {
                bluetoothAdapter?.listenUsingRfcommWithServiceRecord(NAME, MY_UUID)
            } catch (e: Exception) { null }

            var socket: BluetoothSocket?
            while (true) {
                socket = try {
                    mmServerSocket?.accept()
                } catch (e: Exception) {
                    null
                }

                if (socket != null) {
                    // Сохраняем сокет в глобальный объект
                    BluetoothService.connectedSocket = socket

                    runOnUiThread {
                        startActivity(Intent(this@MainActivity, MessageActivity::class.java))
                    }

                    try { mmServerSocket?.close() } catch (e: Exception) {}
                    break
                }
            }
        }
    }
}