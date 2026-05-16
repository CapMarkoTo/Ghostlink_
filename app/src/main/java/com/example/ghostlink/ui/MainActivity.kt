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
import android.widget.LinearLayout
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
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var navigationView: NavigationView

    // Переменная для отслеживания состояния анимации кнопок
    private var isMenuExpanded = false

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

        // Кнопки и их контейнеры
        val buttonsContainer = findViewById<ViewGroup>(R.id.buttonsContainer)
        val btnHost = findViewById<MaterialButton>(R.id.btnHost)
        val btnJoin = findViewById<MaterialButton>(R.id.btnJoin)
        val subHostButtons = findViewById<LinearLayout>(R.id.subHostButtons)
        val btnPrivateChat = findViewById<MaterialButton>(R.id.btnPrivateChat)
        val btnGroupChat = findViewById<MaterialButton>(R.id.btnGroupChat)

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

        // 5. Анимация деления кнопки "Создать чат"
        btnHost.setOnClickListener {
            if (!isMenuExpanded) {
                // Настраиваем плавный переход
                val transition = TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(ChangeBounds())
                    addTransition(Fade())
                    duration = 350
                }
                TransitionManager.beginDelayedTransition(buttonsContainer, transition)

                // Запускаем изменения видимости
                btnJoin.visibility = View.GONE
                btnHost.visibility = View.INVISIBLE
                subHostButtons.visibility = View.VISIBLE

                isMenuExpanded = true
            }
        }

        // 6. Действия для новых кнопок меню
        btnPrivateChat.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, WaitingActivity::class.java)) }
        }

        btnGroupChat.setOnClickListener {
            // Запуск группового лобби ожидания с предварительной проверкой Bluetooth-пермишенов
            checkPermissionsAndRun { startActivity(Intent(this, GroupWaitingActivity::class.java)) }
        }

        btnJoin.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, DeviceListActivity::class.java)) }
        }

        // 7. Обработка кнопки Назад (Сворачивание подменю или закрытие Drawer)
        onBackPressedDispatcher.addCallback(this) {
            when {
                drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                isMenuExpanded -> {
                    // Анимируем возвращение кнопок в дефолтное состояние
                    val transition = TransitionSet().apply {
                        ordering = TransitionSet.ORDERING_TOGETHER
                        addTransition(ChangeBounds())
                        addTransition(Fade())
                        duration = 300
                    }
                    TransitionManager.beginDelayedTransition(buttonsContainer, transition)

                    subHostButtons.visibility = View.GONE
                    btnHost.visibility = View.VISIBLE
                    btnJoin.visibility = View.VISIBLE

                    isMenuExpanded = false
                }
                else -> {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
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

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)

        // Применяем радиус скругления ко ВСЕМ 4 кнопкам
        val radius = prefs.getFloat("button_radius", 16f)
        val radiusPx = (radius * resources.displayMetrics.density).toInt()
        findViewById<MaterialButton>(R.id.btnHost).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnJoin).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnPrivateChat).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnGroupChat).cornerRadius = radiusPx

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

        navigationView.setCheckedItem(R.id.nav_home)
    }
}