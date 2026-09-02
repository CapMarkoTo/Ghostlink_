package com.example.ghostlink

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.activity.enableEdgeToEdge
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.transition.ChangeBounds
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var navigationView: NavigationView

    // Переменные для управления карточкой возврата
    private lateinit var activeChatCard: MaterialCardView
    private lateinit var activeChatDesc: TextView
    private lateinit var btnReturnToChat: MaterialButton
    private lateinit var btnDisconnectChat: Button
    private lateinit var hostContainer: View
    private lateinit var btnJoin: MaterialButton

    // Элементы контейнеров (для бесшовной смены экранов)
    private lateinit var mainContentLayout: LinearLayout
    private lateinit var settingsScrollView: NestedScrollView
    private lateinit var titleText: TextView
    private lateinit var contentContainer: ViewGroup

    // Слайдеры для волновой анимации
    private lateinit var sliderWaveAmplitude: Slider
    private lateinit var sliderWaveLength: Slider

    // Состояние экранов
    private var isSettingsVisible = false
    private var isMenuExpanded = false

    // Массив разрешений с динамической поддержкой уведомлений на Android 13+
    private val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val list = mutableListOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        list.toTypedArray()
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            Toast.makeText(this, "Разрешения получены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Нужны разрешения для корректной работы", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Инициализируем системный канал для пуш-уведомлений
        NotificationHelper.createNotificationChannel(this)

        // Инициализация UI
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val mainLayout = findViewById<View>(R.id.main)
        val floatingToolbar = findViewById<View>(R.id.floatingToolbar)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        titleText = findViewById(R.id.titleText)

        // Контейнеры экранов
        contentContainer = findViewById(R.id.contentContainer)
        mainContentLayout = findViewById(R.id.mainContentLayout)
        settingsScrollView = findViewById(R.id.settingsScrollView)

        // Карточка активного чата
        activeChatCard = findViewById(R.id.activeChatCard)
        activeChatDesc = findViewById(R.id.activeChatDesc)
        btnReturnToChat = findViewById(R.id.btnReturnToChat)
        btnDisconnectChat = findViewById(R.id.btnDisconnectChat)

        // Кнопки главного экрана
        val buttonsContainer = findViewById<ViewGroup>(R.id.buttonsContainer)
        val btnHost = findViewById<MaterialButton>(R.id.btnHost)
        btnJoin = findViewById(R.id.btnJoin)
        hostContainer = findViewById(R.id.hostContainer)
        val subHostButtons = findViewById<LinearLayout>(R.id.subHostButtons)
        val btnPrivateChat = findViewById<MaterialButton>(R.id.btnPrivateChat)
        val btnGroupChat = findViewById<MaterialButton>(R.id.btnGroupChat)

        // Элементы Настроек
        val editName = findViewById<TextInputEditText>(R.id.editGhostName)
        val sliderCorner = findViewById<Slider>(R.id.sliderCornerRadius)
        val switchNavType = findViewById<MaterialSwitch>(R.id.switchNavigationType)
        val switchNotif = findViewById<MaterialSwitch>(R.id.switchNotifications)
        val btnSaveSettings = findViewById<MaterialButton>(R.id.btnSaveSettings)

        // Слайдеры для волновой анимации
        sliderWaveAmplitude = findViewById(R.id.sliderWaveAmplitude)
        sliderWaveLength = findViewById(R.id.sliderWaveLength)

        // Кнопки на Floating Toolbar
        val btnNavHome = findViewById<ImageButton>(R.id.btnNavHome)
        val btnNavSettings = findViewById<ImageButton>(R.id.btnNavSettings)

        btnGroupChat.isEnabled = false
        btnGroupChat.alpha = 0.5f

        // Edge-to-Edge настройка отступов системных баров
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainLayout.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            val density = resources.displayMetrics.density
            val margin32dp = (32 * density).toInt()
            val params = floatingToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            params?.setMargins(0, 0, 0, systemBars.bottom + margin32dp)
            floatingToolbar.layoutParams = params

            insets
        }

        com.google.android.material.color.DynamicColors.applyToActivitiesIfAvailable(application)
        bluetoothAdapter = getSystemService(BluetoothManager::class.java)?.adapter

        // Загрузка настроек при запуске
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        editName.setText(prefs.getString("ghost_name", Build.MODEL))
        sliderCorner.value = prefs.getFloat("button_radius", 16f)
        switchNavType.isChecked = prefs.getBoolean("use_floating_toolbar", false)
        switchNotif.isChecked = prefs.getBoolean("notifications_enabled", true)

        // Загружаем сохраненные значения волны (или берем дефолтные)
        sliderWaveAmplitude.value = prefs.getFloat("wave_amplitude_factor", 2.0f)
        sliderWaveLength.value = prefs.getFloat("wave_length_factor", 10.0f)

        // Инициализация бокового меню (Drawer)
        btnOpenMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    showHomeScreen()
                    drawerLayout.closeDrawers()
                }
                R.id.nav_settings -> {
                    showSettingsScreen()
                    drawerLayout.closeDrawers()
                }
            }
            true
        }

        // Логика переключения Floating Toolbar
        btnNavHome.setOnClickListener {
            showHomeScreen()
        }

        btnNavSettings.setOnClickListener {
            if (isSettingsVisible) {
                settingsScrollView.smoothScrollTo(0, 0)
            } else {
                showSettingsScreen()
            }
        }

        // Кнопка сохранения настроек
        btnSaveSettings.setOnClickListener {
            val newName = editName.text.toString().trim()
            if (newName.isNotEmpty()) {
                prefs.edit().apply {
                    putString("ghost_name", newName)
                    putFloat("button_radius", sliderCorner.value)
                    putBoolean("use_floating_toolbar", switchNavType.isChecked)
                    putBoolean("notifications_enabled", switchNotif.isChecked)

                    // Сохраняем новые настройки волны
                    putFloat("wave_amplitude_factor", sliderWaveAmplitude.value)
                    putFloat("wave_length_factor", sliderWaveLength.value)

                    apply()
                }
                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
                applyThemeSettings() // Сразу применяем радиусы к кнопкам
            } else {
                Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }

        // Логика создания чата
        btnHost.setOnClickListener {
            if (!isMenuExpanded) {
                val transition = TransitionSet().apply {
                    ordering = TransitionSet.ORDERING_TOGETHER
                    addTransition(ChangeBounds())
                    addTransition(Fade())
                    duration = 350
                }
                TransitionManager.beginDelayedTransition(buttonsContainer, transition)

                btnJoin.visibility = View.GONE
                btnHost.visibility = View.INVISIBLE
                subHostButtons.visibility = View.VISIBLE

                isMenuExpanded = true
            }
        }

        btnPrivateChat.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, WaitingActivity::class.java)) }
        }

        btnGroupChat.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, GroupWaitingActivity::class.java)) }
        }

        btnJoin.setOnClickListener {
            checkPermissionsAndRun { startActivity(Intent(this, DeviceListActivity::class.java)) }
        }

        btnReturnToChat.setOnClickListener {
            startActivity(Intent(this, MessageActivity::class.java))
        }

        btnDisconnectChat.setOnClickListener {
            BluetoothService.clearConnection()
            val transition = TransitionSet().apply {
                ordering = TransitionSet.ORDERING_TOGETHER
                addTransition(ChangeBounds())
                addTransition(Fade())
                duration = 300
            }
            TransitionManager.beginDelayedTransition(buttonsContainer, transition)

            activeChatCard.visibility = View.GONE
            hostContainer.visibility = View.VISIBLE
            btnJoin.visibility = View.VISIBLE

            Toast.makeText(this, "Подключение закрыто", Toast.LENGTH_SHORT).show()
        }

        // Обработка системной кнопки Назад
        onBackPressedDispatcher.addCallback(this) {
            when {
                drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                }
                isSettingsVisible -> {
                    showHomeScreen() // Если мы в настройках — возвращаемся на главный
                }
                isMenuExpanded -> {
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

    // Показываем главный экран с плавной анимацией
    private fun showHomeScreen() {
        if (!isSettingsVisible) return

        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade())
            duration = 250
        }
        TransitionManager.beginDelayedTransition(contentContainer, transition)

        settingsScrollView.visibility = View.GONE
        mainContentLayout.visibility = View.VISIBLE
        titleText.text = "GhostLink BT"
        isSettingsVisible = false

        updateToolbarTints()
        navigationView.setCheckedItem(R.id.nav_home)
    }

    // Показываем настройки с плавной анимацией
    private fun showSettingsScreen() {
        if (isSettingsVisible) return

        val transition = TransitionSet().apply {
            ordering = TransitionSet.ORDERING_TOGETHER
            addTransition(Fade())
            duration = 250
        }
        TransitionManager.beginDelayedTransition(contentContainer, transition)

        mainContentLayout.visibility = View.GONE
        settingsScrollView.visibility = View.VISIBLE
        titleText.text = "Настройки"
        isSettingsVisible = true

        updateToolbarTints()
        navigationView.setCheckedItem(R.id.nav_settings)
    }

    // Красим иконки Floating Toolbar в зависимости от того, какой экран активен
    private fun updateToolbarTints() {
        val btnNavHome = findViewById<ImageButton>(R.id.btnNavHome)
        val btnNavSettings = findViewById<ImageButton>(R.id.btnNavSettings)

        val activeColor = TypedValue().let { typedValue ->
            theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            typedValue.data
        }

        val inactiveColor = com.google.android.material.color.MaterialColors.getColor(
            this,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            android.graphics.Color.GRAY
        )

        if (isSettingsVisible) {
            btnNavHome.setColorFilter(inactiveColor)
            btnNavSettings.setColorFilter(activeColor)
        } else {
            btnNavHome.setColorFilter(activeColor)
            btnNavSettings.setColorFilter(inactiveColor)
        }
    }

    private fun checkPermissionsAndRun(action: () -> Unit) {
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            action()
        } else {
            requestPermissionLauncher.launch(permissions)
        }
    }

    // Настройка радиусов и навигации на лету
    private fun applyThemeSettings() {
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)

        val radius = prefs.getFloat("button_radius", 16f)
        val radiusPx = (radius * resources.displayMetrics.density).toInt()
        findViewById<MaterialButton>(R.id.btnHost).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnJoin).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnPrivateChat).cornerRadius = radiusPx
        findViewById<MaterialButton>(R.id.btnGroupChat).cornerRadius = radiusPx
        btnReturnToChat.cornerRadius = radiusPx

        val useFloating = prefs.getBoolean("use_floating_toolbar", false)
        val floatingToolbar = findViewById<View>(R.id.floatingToolbar)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)

        if (useFloating) {
            floatingToolbar?.visibility = View.VISIBLE
            btnOpenMenu.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            updateToolbarTints()
        } else {
            floatingToolbar?.visibility = View.GONE
            btnOpenMenu.visibility = View.VISIBLE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }
    }

    override fun onResume() {
        super.onResume()

        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val savedAmplitude = prefs.getFloat("wave_amplitude_factor", 2.0f)
        val savedWavelength = prefs.getFloat("wave_length_factor", 10.0f)

        // Синхронизируем настройки для DeviceListActivity
        getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().apply {
            putFloat("wave_amplitude_factor", savedAmplitude)
            putFloat("wave_length_factor", savedWavelength)
            apply()
        }

        applyThemeSettings()

        // Проверяем, активно ли Bluetooth соединение
        if (BluetoothService.isConnectionActive()) {
            val deviceName = BluetoothService.remoteDeviceName ?: "подключение..."
            activeChatDesc.text = "Собеседник: $deviceName"
            activeChatCard.visibility = View.VISIBLE
            hostContainer.visibility = View.GONE
            btnJoin.visibility = View.GONE
        } else {
            activeChatCard.visibility = View.GONE
            hostContainer.visibility = View.VISIBLE
            btnJoin.visibility = View.VISIBLE
        }

        navigationView.setCheckedItem(if (isSettingsVisible) R.id.nav_settings else R.id.nav_home)
    }
}