package com.example.ghostlink

import android.bluetooth.BluetoothSocket

object BluetoothService {
    var connectedSocket: BluetoothSocket? = null

    // Храним имя собеседника для отображения на карточке главного экрана
    var remoteDeviceName: String? = null

    // Функция быстрой проверки, живо ли еще наше соединение
    fun isConnectionActive(): Boolean {
        val socket = connectedSocket
        return socket != null && socket.isConnected
    }

    // Метод для полной очистки данных при выходе из чата (если мы выходим осознанно)
    fun clearConnection() {
        try {
            connectedSocket?.close()
        } catch (e: Exception) {
            // Игнорируем ошибки закрытия
        }
        connectedSocket = null
        remoteDeviceName = null
    }
}