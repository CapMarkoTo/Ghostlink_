package com.example.ghostlink

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

object BluetoothClientManager {
    private const val TAG = "GhostClientManager"
    private val GROUP_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    var isConnected = false
        private set

    // Колбэк для получения сообщений от Хоста (широковещательные)
    var onMessageReceived: ((String) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun connectToHost(device: BluetoothDevice) {
        thread(start = true, name = "ClientConnectThread") {
            try {
                socket = device.createRfcommSocketToServiceRecord(GROUP_UUID)
                socket?.connect()
                outputStream = socket?.outputStream
                isConnected = true
                Log.d(TAG, "Подключились к Хосту: ${device.name}")

                // Запускаем поток чтения от Хоста
                listenForMessages()
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка подключения к хосту", e)
                isConnected = false
            }
        }
    }

    private fun listenForMessages() {
        val inputStream = socket?.inputStream ?: return
        val buffer = ByteArray(1024)

        while (isConnected) {
            try {
                val bytes = inputStream.read(buffer)
                if (bytes > 0) {
                    val message = String(buffer, 0, bytes)
                    onMessageReceived?.invoke(message)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Потеряна связь с Хостом", e)
                isConnected = false
                break
            }
        }
    }

    fun sendMessage(message: String) {
        thread {
            try {
                outputStream?.write(message.toByteArray())
                outputStream?.flush()
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка отправки сообщения Хосту", e)
            }
        }
    }

    fun disconnect() {
        isConnected = false
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Ошибка закрытия сокета клиента", e)
        }
    }
}