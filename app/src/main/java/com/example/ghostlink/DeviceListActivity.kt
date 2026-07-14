package com.example.ghostlink

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.UUID

class DeviceListActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    private lateinit var searchAnimationContainer: FrameLayout

    private val MY_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_list)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top + 20, systemBars.right, systemBars.bottom)
            insets
        }

        // Находим контейнер, куда поместим индикатор
        searchAnimationContainer = findViewById(R.id.searchAnimationContainer)

        // Безопасно собираем волновой индикатор через явные вызовы Java-методов
        val searchAnimationView = try {
            CircularProgressIndicator(
                this,
                null,
                com.google.android.material.R.attr.circularProgressIndicatorStyle
            ).apply {
                // Переводим все размеры в Int пиксели, так как этого требуют методы Material 3
                val amplitude = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2.0f, resources.displayMetrics).toInt()
                val wavelength = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics).toInt()
                val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
                val thickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()

                // Используем явные сеттеры базового класса, чтобы IDE не ругалась на свойства
                setIndicatorSize(size)
                setTrackThickness(thickness)
                setWaveAmplitude(amplitude)
                setWavelengthIndeterminate(wavelength)

                isIndeterminate = true
            }
        } catch (e: Throwable) {
            // Если Expressive Wavy методы не слинковались, создаем классический круг
            CircularProgressIndicator(this).apply {
                val size = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24f, resources.displayMetrics).toInt()
                val thickness = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
                setIndicatorSize(size)
                setTrackThickness(thickness)
                isIndeterminate = true
            }
        }

        // Добавляем созданную вьюшку в наш контейнер
        searchAnimationContainer.addView(searchAnimationView)

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

    private fun startSearchAnimation() {
        runOnUiThread {
            searchAnimationContainer.visibility = View.VISIBLE
        }
    }

    private fun stopSearchAnimation() {
        runOnUiThread {
            searchAnimationContainer.visibility = View.GONE
        }
    }

    private fun startScanning() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            bluetoothAdapter?.startDiscovery()
            startSearchAnimation()
        } else {
            Toast.makeText(this, "Нет разрешений для поиска", Toast.LENGTH_SHORT).show()
        }
    }

    private inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if (ActivityCompat.checkSelfPermission(this@DeviceListActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                device.createRfcommSocketToServiceRecord(MY_UUID)
            } else null
        }

        override fun run() {
            if (ActivityCompat.checkSelfPermission(this@DeviceListActivity, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                bluetoothAdapter?.cancelDiscovery()
            }
            stopSearchAnimation()

            try {
                mmSocket?.connect()
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
        BluetoothService.connectedSocket = socket
        runOnUiThread {
            val intent = Intent(this, MessageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && device.name != null) {
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device)
                        deviceNames.add(device.name)

                        // Обновляем список и запускаем анимацию появления
                        deviceAdapter.notifyDataSetChanged()
                        val listView = findViewById<ListView>(R.id.devicesListView)
                        listView?.scheduleLayoutAnimation()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSearchAnimation()
        try {
            bluetoothAdapter?.cancelDiscovery()
            unregisterReceiver(receiver)
        } catch (e: Exception) {}
    }
}