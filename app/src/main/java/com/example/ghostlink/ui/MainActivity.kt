package com.example.ghostlink

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null

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
    ) { grants ->
        if (grants.values.all { it }) {
            Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Для работы GhostLink нужны все разрешения", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(application)

        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter

        // КНОПКА "СОЗДАТЬ ЧАТ"
        findViewById<Button>(R.id.btnHost).setOnClickListener {
            checkPermissionsAndRun {
                startActivity(Intent(this, WaitingActivity::class.java))
            }
        }

        // КНОПКА "ПОДКЛЮЧИТЬСЯ"
        findViewById<Button>(R.id.btnJoin).setOnClickListener {
            checkPermissionsAndRun {
                startActivity(Intent(this, DeviceListActivity::class.java))
            }
        }

        // --- НОВОЕ: КНОПКА "НАСТРОЙКИ" ---
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            // Для перехода в настройки разрешения не обязательны,
            // поэтому вызываем startActivity напрямую
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissionsAndRun(action: () -> Unit) {
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            action()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }
}