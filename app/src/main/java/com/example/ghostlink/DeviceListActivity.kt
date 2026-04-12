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
import android.view.View
import android.view.animation.LinearInterpolator
import android.animation.ValueAnimator
import android.animation.ObjectAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.UUID

class DeviceListActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var deviceAdapter: ArrayAdapter<String>
    private val discoveredDevices = mutableListOf<BluetoothDevice>()
    private val deviceNames = mutableListOf<String>()

    private var shapeAnimator: ValueAnimator? = null
    private var rotateAnimator: ObjectAnimator? = null

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
        val animView = findViewById<View>(R.id.searchAnimationView) ?: return
        animView.visibility = View.VISIBLE

        val typedValue = TypedValue()
        val colorPrimary = if (theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, typedValue, true)) {
            typedValue.data
        } else {
            Color.parseColor("#6200EE")
        }

        val morphingDrawable = MorphingDrawable(colorPrimary)
        animView.background = morphingDrawable

        shapeAnimator = ValueAnimator.ofFloat(0f, 3f).apply {
            duration = 2500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animator ->
                morphingDrawable.progress = animator.animatedValue as Float
            }
            start()
        }

        rotateAnimator = ObjectAnimator.ofFloat(animView, "rotation", 0f, 360f).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun stopSearchAnimation() {
        runOnUiThread {
            shapeAnimator?.cancel()
            rotateAnimator?.cancel()
            findViewById<View>(R.id.searchAnimationView)?.visibility = View.GONE
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

class MorphingDrawable(private val color: Int) : Drawable() {
    private val path = Path()
    private val paint = Paint().apply {
        color = this@MorphingDrawable.color
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    var progress: Float = 0f
        set(value) {
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()
        val cx = width / 2f

        path.reset()

        val p1: PointF; val p2: PointF; val p3: PointF; val p4: PointF
        val currentRadius: Float

        when {
            progress <= 1f -> {
                p1 = PointF(0f, 0f); p2 = PointF(width, 0f)
                p3 = PointF(width, height); p4 = PointF(0f, height)
                currentRadius = cx * (1f - progress)
            }
            progress <= 2f -> {
                val p = progress - 1f
                p1 = lerp(PointF(0f, 0f), PointF(cx, 0f), p)
                p2 = lerp(PointF(width, 0f), PointF(cx, 0f), p)
                p3 = PointF(width, height)
                p4 = PointF(0f, height)
                currentRadius = 15f
            }
            else -> {
                val p = progress - 2f
                p1 = lerp(PointF(cx, 0f), PointF(0f, 0f), p)
                p2 = lerp(PointF(cx, 0f), PointF(width, 0f), p)
                p3 = PointF(width, height)
                p4 = PointF(0f, height)
                currentRadius = 15f + (cx - 15f) * p
            }
        }

        paint.pathEffect = CornerPathEffect(Math.max(currentRadius, 8f))
        path.moveTo(p1.x, p1.y)
        path.lineTo(p2.x, p2.y)
        path.lineTo(p3.x, p3.y)
        path.lineTo(p4.x, p4.y)
        path.close()

        canvas.drawPath(path, paint)
    }

    private fun lerp(start: PointF, end: PointF, fraction: Float): PointF {
        return PointF(
            start.x + fraction * (end.x - start.x),
            start.y + fraction * (end.y - start.y)
        )
    }

    override fun setAlpha(alpha: Int) { paint.alpha = alpha }
    override fun setColorFilter(colorFilter: ColorFilter?) { paint.colorFilter = colorFilter }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}