package com.example.ghostlink

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object NotificationHelper {
    private const val CHANNEL_ID = "ghostlink_messages"
    private const val CHANNEL_NAME = "Входящие сообщения"
    private const val CHANNEL_DESC = "Уведомления о новых сообщениях в чате GhostLink"
    private const val NOTIFICATION_ID = 1001

    // Создание канала (вызывается один раз при старте приложения)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Отправка уведомления
    fun showMessageNotification(context: Context, senderName: String, messageText: String) {
        // Проверяем, включены ли уведомления в настройках GhostPrefs
        val prefs = context.getSharedPreferences("GhostPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("notifications_enabled", true)
        if (!isEnabled) return

        // Проверяем разрешение на отправку уведомлений для Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                return // Нет разрешения — не шлем
            }
        }

        // Открываем MessageActivity при клике на уведомление
        val intent = Intent(context, MessageActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Временная системная иконка чата
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Удаляет уведомление после клика
            .setOnlyAlertOnce(true) // Не вибрировать постоянно на каждое сообщение

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID, builder.build())
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}