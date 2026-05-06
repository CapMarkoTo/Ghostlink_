package com.example.ghostlink

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var navigationView: NavigationView

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
            Toast.makeText(this, "Нужны разрешения", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // 1. Инициализация UI
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val mainLayout = findViewById<View>(R.id.main)
        val floatingToolbar = findViewById<View>(R.id.floatingToolbar)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val btnHost = findViewById<MaterialButton>(R.id.btnHost)
        val btnJoin = findViewById<MaterialButton>(R.id.btnJoin)

        // 2. Исправленный Listener отступов (Edge-to-Edge фикс)
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Отступ контента сверху (чтобы не залез под статус-бар)
            mainLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            // Поднимаем Floating Toolbar над системными кнопками навигации
            val density = resources.displayMetrics.density
            val margin32dp = (32 * density).toInt()
            val params = floatingToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            params?.setMargins(0, 0, 0, systemBars.bottom + margin32dp)
            floatingToolbar.layoutParams = params

            insets
        }

        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(application)
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter

        // 3. Логика Drawer (Боковое меню)
        btnOpenMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawers()
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawers()
                }
            }
            true
        }

        // 4. Логика Floating Toolbar (Нижняя панель)
        findViewById<View>(R.id.btnNavSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // 5. Кнопки основного экрана
        btnHost.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, WaitingActivity::class.java)) }
        }

        btnJoin.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, DeviceListActivity::class.java)) }
        }
    }

    private fun checkPermissionsAndRun(action: () -> Unit) {
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            action()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)

        // Применяем радиус скругления кнопок
        val radius = prefs.getFloat("button_radius", 16f)
        val radiusPx = (radius * resources.displayMetrics.density).toInt()
        findViewById<MaterialButton>(R.id.btnHost).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnJoin).cornerRadius = radiusPx

        // Применяем тип навигации (Floating vs Drawer)
        val useFloating = prefs.getBoolean("use_floating_toolbar", false)
        val floatingToolbar = findViewById<View>(R.id.floatingToolbar)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        if (useFloating) {
            floatingToolbar?.visibility = View.VISIBLE
            btnOpenMenu.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            floatingToolbar?.visibility = View.GONE
            btnOpenMenu.visibility = View.VISIBLE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        // Управление заголовком (Раздельные заголовки)
        val showHeader = prefs.getBoolean("separate_headers", true)
        findViewById<TextView>(R.id.titleText)?.visibility = if (showHeader) View.VISIBLE else View.GONE

        navigationView.setCheckedItem(R.id.nav_home)

        // Обработка кнопки Назад
        onBackPressedDispatcher.addCallback(this) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}