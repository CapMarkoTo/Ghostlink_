package com.example.ghostlink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class GroupWaitingActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var originalDeviceName: String? = null

    // Список имен подключившихся призраков и адаптер для RecyclerView
    private val connectedGhostsList = mutableListOf<String>()
    private lateinit var ghostsAdapter: GhostsAdapter

    private lateinit var tvStatus: TextView
    private lateinit var btnStartGroupChat: MaterialButton

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_waiting)

        // 1. Инициализация UI
        tvStatus = findViewById(R.id.tvStatus)
        btnStartGroupChat = findViewById(R.id.btnStartGroupChat)
        val rvConnectedDevices = findViewById<RecyclerView>(R.id.rvConnectedDevices)

        // Настройка RecyclerView
        ghostsAdapter = GhostsAdapter(connectedGhostsList)
        rvConnectedDevices.layoutManager = LinearLayoutManager(this)
        rvConnectedDevices.adapter = ghostsAdapter

        // 2. Инициализация Bluetooth
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        // 3. Магия с подменой имени (как в одиночном чате)
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val ghostName = prefs.getString("ghost_name", "GhostLink User")

        bluetoothAdapter?.let { adapter ->
            originalDeviceName = adapter.name
            adapter.name = ghostName
        }

        // Подпись в заголовке лобби (опционально, выведем в статус)
        updateStatusUI()

        // 4. Включаем видимость устройства на 5 минут (300 сек)
        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)

        // 5. Настройка колбэков менеджера группы
        BluetoothGroupManager.onDeviceConnected = { deviceName ->
            runOnUiThread {
                if (!connectedGhostsList.contains(deviceName)) {
                    connectedGhostsList.add(deviceName)
                    ghostsAdapter.notifyItemInserted(connectedGhostsList.size - 1)
                    updateStatusUI()
                }
            }
        }

        // 6. Запуск сервера через менеджер
        BluetoothGroupManager.startGroupServer(bluetoothAdapter)

        // 7. Кнопка старта чата
        btnStartGroupChat.setOnClickListener {
            // Сигнализируем всем клиентам, что чат начинается
            // Например, шлем кодовую команду "@SYSTEM_START_CHAT@"
            BluetoothGroupManager.broadcastMessage("@SYSTEM_START_CHAT@")

            // Открываем экран группового чата у себя (у Хоста)
            openGroupChatActivity()
        }
    }

    private fun updateStatusUI() {
        val count = connectedGhostsList.size
        tvStatus.text = "Ожидание призраков ($count/7)..."

        // Активируем кнопку, если подключился хотя бы 1 человек
        btnStartGroupChat.enabled = count > 0
    }

    private fun openGroupChatActivity() {
        // Сюда мы передадим переход на будущую GroupMessageActivity
        // val intent = Intent(this, GroupMessageActivity::class.java)
        // startActivity(intent)
        // finish()
    }

    @SuppressLint("MissingPermission")
    private fun restoreOriginalName() {
        if (originalDeviceName != null) {
            bluetoothAdapter?.name = originalDeviceName
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        restoreOriginalName()

        // Если Хост выходит из лобби до старта — тушим сервер
        if (!BluetoothGroupManager.isHosting) {
            BluetoothGroupManager.stopGroup()
        }
    }

    // --- ВСТРОЕННЫЙ АДАПТЕР ДЛЯ СПИСКА ПОДКЛЮЧЕННЫХ УСТРОЙСТВ ---
    private inner class GhostsAdapter(private val ghosts: List<String>) :
        RecyclerView.Adapter<GhostsAdapter.GhostViewHolder>() {

        class GhostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GhostViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ghost_device, parent, false)
            return GhostViewHolder(view)
        }

        override fun onBindViewHolder(holder: GhostViewHolder, position: Int) {
            holder.tvDeviceName.text = ghosts[position]
        }

        override fun getItemCount(): Int = ghosts.size
    }
}