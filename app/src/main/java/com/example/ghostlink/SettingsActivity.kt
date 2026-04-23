package com.example.ghostlink

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val editName = findViewById<TextInputEditText>(R.id.editGhostName)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        // Загружаем текущее имя (если его нет — используем модель телефона)
        val prefs = getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val currentName = prefs.getString("ghost_name", android.os.Build.MODEL)
        editName.setText(currentName)

        btnSave.setOnClickListener {
            val newName = editName.text.toString().trim()

            if (newName.isNotEmpty()) {
                // Сохраняем имя в память
                prefs.edit().putString("ghost_name", newName).apply()

                // Важно: Меняем имя самого Bluetooth-адаптера (если есть разрешения)
                // Но для начала просто сохраним локально
                Toast.makeText(this, "Имя призрака изменено!", Toast.LENGTH_SHORT).show()
                finish() // Возвращаемся назад
            } else {
                Toast.makeText(this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show()
            }
        }
    }
}