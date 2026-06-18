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

    private val connectedGhostsList = mutableListOf<String>()
    private lateinit var ghostsAdapter: GhostsAdapter

    private lateinit var tvStatus: TextView
    private lateinit var btnStartGroupChat: MaterialButton

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_waiting)

        tvStatus = findViewById(R.id.tvStatus)
        btnStartGroupChat = findViewById(R.id.btnStartGroupChat)
        val rvConnectedDevices = findViewById<RecyclerView>(R.id.rvConnectedDevices)

        ghostsAdapter = GhostsAdapter(connectedGhostsList)
        rvConnectedDevices.layoutManager = LinearLayoutManager(this)
        rvConnectedDevices.adapter = ghostsAdapter

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val ghostName = prefs.getString("ghost_name", "GhostLink User")

        bluetoothAdapter?.let { adapter ->
            originalDeviceName = adapter.name
            adapter.name = ghostName
        }

        updateStatusUI()

        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
        }
        startActivity(discoverableIntent)

        BluetoothGroupManager.onDeviceConnected = { deviceName ->
            runOnUiThread {
                if (!connectedGhostsList.contains(deviceName)) {
                    connectedGhostsList.add(deviceName)
                    ghostsAdapter.notifyItemInserted(connectedGhostsList.size - 1)
                    updateStatusUI()
                }
            }
        }

        BluetoothGroupManager.startGroupServer(bluetoothAdapter)

        btnStartGroupChat.setOnClickListener {
            BluetoothGroupManager.broadcastMessage("@SYSTEM_START_CHAT@")
            openGroupChatActivity()
        }
    }

    private fun updateStatusUI() {
        val count = connectedGhostsList.size
        tvStatus.text = "Ожидание призраков ($count/7)..."
        // Важно: .isEnabled, а не .enabled
        btnStartGroupChat.isEnabled = count > 0
    }

    private fun openGroupChatActivity() {
        // Реализация перехода
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
        if (!BluetoothGroupManager.isHosting) {
            BluetoothGroupManager.stopGroup()
        }
    }
}

// --- Выносим адаптер и холдер наружу ---
class GhostsAdapter(private val ghosts: List<String>) :
    RecyclerView.Adapter<GhostViewHolder>() {

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

class GhostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
}