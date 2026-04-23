package com.example.ghostlink

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {

    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        // Инициализация вьюх
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val editName = findViewById<TextInputEditText>(R.id.editGhostName)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveSettings)

        // Свитчи из нового дизайна
        val switchFonts = findViewById<MaterialSwitch>(R.id.switchSystemFonts)
        val switchHeaders = findViewById<MaterialSwitch>(R.id.switchSeparateHeaders)
        val switchThemes = findViewById<MaterialSwitch>(R.id.switchCustomThemes)

        // 1. Загружаем текущие настройки из SharedPreferences
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)

        editName.setText(prefs.getString("ghost_name", android.os.Build.MODEL))
        switchFonts.isChecked = prefs.getBoolean("use_system_fonts", false)
        switchHeaders.isChecked = prefs.getBoolean("separate_headers", true)
        switchThemes.isChecked = prefs.getBoolean("custom_themes", false)

        // 2. Логика Drawer
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

        // 3. Единая кнопка сохранения для всех настроек
        btnSave.setOnClickListener {
            val newName = editName.text.toString().trim()

            if (newName.isNotEmpty()) {
                prefs.edit().apply {
                    putString("ghost_name", newName)
                    putBoolean("use_system_fonts", switchFonts.isChecked)
                    putBoolean("separate_headers", switchHeaders.isChecked)
                    putBoolean("custom_themes", switchThemes.isChecked)
                    apply()
                }
                Toast.makeText(this, "Все настройки сохранены", Toast.LENGTH_SHORT).show()
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