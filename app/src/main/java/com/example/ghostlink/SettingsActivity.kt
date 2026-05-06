package com.example.ghostlink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider

class SettingsActivity : AppCompatActivity() {

    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Инициализация вьюх
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val mainLayout = findViewById<View>(R.id.main)
        navigationView = findViewById(R.id.navigationView)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val editName = findViewById<TextInputEditText>(R.id.editGhostName)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveSettings)

        // Floating Toolbar элементы
        val floatingToolbar = findViewById<View>(R.id.floatingToolbar)
        val btnNavHome = findViewById<ImageButton>(R.id.btnNavHome)
        val btnNavSettings = findViewById<ImageButton>(R.id.btnNavSettings)

        // Элементы управления настройками
        val sliderCorner = findViewById<Slider>(R.id.sliderCornerRadius)
        val switchNavType = findViewById<MaterialSwitch>(R.id.switchNavigationType)
        val switchNotif = findViewById<MaterialSwitch>(R.id.switchNotifications)

        // Находим скролл (убедись, что в activity_settings.xml у NestedScrollView стоит этот id)
        val settingsScrollView = findViewById<NestedScrollView>(R.id.settingsScrollView)

        // --- EDGE-TO-EDGE ФИКС (Поднимаем панель над кнопками системы) ---
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Отступ основного контента сверху (статус-бар)
            mainLayout.setPadding(0, systemBars.top, 0, 0)

            // Динамический отступ для Floating Toolbar снизу
            val density = resources.displayMetrics.density
            val margin32dp = (32 * density).toInt()
            val params = floatingToolbar.layoutParams as? ViewGroup.MarginLayoutParams
            params?.setMargins(0, 0, 0, systemBars.bottom + margin32dp)
            floatingToolbar.layoutParams = params

            insets
        }

        // 1. Загружаем текущие настройки
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        editName.setText(prefs.getString("ghost_name", android.os.Build.MODEL))
        sliderCorner.value = prefs.getFloat("button_radius", 16f)
        switchNavType.isChecked = prefs.getBoolean("use_floating_toolbar", false)
        switchNotif.isChecked = prefs.getBoolean("notifications_enabled", true)

        // --- ЛОГИКА НАВИГАЦИИ ---

        // Проверка режима навигации при запуске
        if (switchNavType.isChecked) {
            btnOpenMenu.visibility = View.GONE
            floatingToolbar.visibility = View.VISIBLE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        } else {
            btnOpenMenu.visibility = View.VISIBLE
            floatingToolbar.visibility = View.GONE
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        }

        // Клики по Floating Toolbar
        btnNavHome?.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnNavSettings?.setOnClickListener {
            // Теперь используем прямой ID для скролла — ошибка исчезнет
            settingsScrollView?.smoothScrollTo(0, 0)
        }

        btnOpenMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    finish()
                }
                R.id.nav_settings -> drawerLayout.closeDrawers()
            }
            true
        }

        // --- СОХРАНЕНИЕ ---
        btnSave.setOnClickListener {
            val newName = editName.text.toString().trim()

            if (newName.isNotEmpty()) {
                prefs.edit().apply {
                    putString("ghost_name", newName)
                    putFloat("button_radius", sliderCorner.value)
                    putBoolean("use_floating_toolbar", switchNavType.isChecked)
                    putBoolean("notifications_enabled", switchNotif.isChecked)
                    apply()
                }
                Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()

                // Перезапуск для обновления UI
                finish()
                startActivity(intent)
                overridePendingTransition(0, 0)
            } else {
                Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        navigationView.setCheckedItem(R.id.nav_settings)

        onBackPressedDispatcher.addCallback(this) {
            val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
            if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}