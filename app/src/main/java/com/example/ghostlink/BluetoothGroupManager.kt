package com.example.ghostlink

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

object BluetoothGroupManager {
    private const val TAG = "GhostGroupManager"
    private const val NAME = "GhostLinkGroup"

    // Уникальный UUID именно для групповых чатов GhostLink
    private val GROUP_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    private var serverSocket: BluetoothServerSocket? = null

    // Список всех подключенных клиентов (их сокетов)
    val connectedClients = mutableListOf<BluetoothSocket>()

    // Список потоков вывода, чтобы отправлять сообщения
    private val outputStreams = mutableListOf<OutputStream>()

    var isHosting = false
        private set

    // Колбэк, который будет сообщать экрану, что кто-то подключился или пришло сообщение
    var onDeviceConnected: ((String) -> Unit)? = null
    var onMessageReceived: ((String) -> Unit)? = null

    /**
     * Запуск сервера группового чата (для Хоста)
     */
    fun startGroupServer(adapter: BluetoothAdapter?) {
        if (isHosting) return
        isHosting = true
        connectedClients.clear()
        outputStreams.clear()

        thread(start = true, name = "GroupServerThread") {
            try {
                serverSocket = adapter?.listenUsingRfcommWithServiceRecord(NAME, GROUP_UUID)
                Log.d(TAG, "Сервер группового чата запущен. Ожидание клиентов...")

                // Крутим бесконечный цикл и принимаем до 7 клиентов (ограничение Bluetooth)
                while (isHosting && connectedClients.size < 7) {
                    val socket = try {
                        serverSocket?.accept()
                    } catch (e: IOException) {
                        Log.e(TAG, "Accept вылетел или сервер остановлен", e)
                        null
                    }

                    if (socket != null) {
                        manageConnectedClient(socket)
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Ошибка запуска сервера", e)
            }
        }
    }

    /**
     * Обработка каждого нового подключившегося клиента
     */
    private fun manageConnectedClient(socket: BluetoothSocket) {
        synchronized(connectedClients) {
            connectedClients.add(socket)
            try {
                val outStream = socket.outputStream
                outputStreams.add(outStream)
            } catch (e: IOException) {
                Log.e(TAG, "Не удалось получить OutputStream клиента", e)
            }
        }

        val deviceName = socket.remoteDevice.name ?: "Неизвестный Призрак"
        Log.d(TAG, "Подключилось устройство: $deviceName")

        // Уведомляем интерфейс лобби
        onDeviceConnected?.invoke(deviceName)

        // Запускаем отдельный поток на чтение данных от ЭТОГО конкретного клиента
        thread(start = true, name = "ClientReader-${deviceName}") {
            val inputStream: InputStream? = try {
                socket.inputStream
            } catch (e: IOException) {
                Log.e(TAG, "Не удалось получить InputStream", e)
                null
            }

            val buffer = ByteArray(1024)
            var bytes: Int

            while (isHosting && socket.isConnected) {
                try {
                    bytes = inputStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val rawMessage = String(buffer, 0, bytes)
                        Log.d(TAG, "Получено сообщение: $rawMessage")

                        // 1. Передаем в наш UI чата
                        onMessageReceived?.invoke(rawMessage)

                        // 2. Ретранслируем (Broadcast) это сообщение ВСЕМ остальным участникам сети!
                        broadcastMessage(rawMessage, senderSocket = socket)
                    }
                } catch (e: IOException) {
                    Log.d(TAG, "Клиент $deviceName отключился")
                    synchronized(connectedClients) {
                        connectedClients.remove(socket)
                    }
                    break
                }
            }
        }
    }

    /**
     * Рассылка сообщения ВСЕМ подключенным участникам
     * Снабжено параметром senderSocket, чтобы не отправлять автору его же сообщение назад
     */
    fun broadcastMessage(message: String, senderSocket: BluetoothSocket? = null) {
        val data = message.toByteArray()
        synchronized(connectedClients) {
            for (i in connectedClients.indices) {
                // Пропускаем автора сообщения
                if (connectedClients[i] == senderSocket) continue

                try {
                    outputStreams[i].write(data)
                    outputStreams[i].flush()
                } catch (e: IOException) {
                    Log.e(TAG, "Ошибка отправки сообщения участнику сети", e)
                }
            }
        }
    }

    /**
     * Полная остановка группы и закрытие всех сокетов
     */
    fun stopGroup() {
        isHosting = false
        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Не удалось закрыть серверный сокет", e)
        }

        synchronized(connectedClients) {
            for (socket in connectedClients) {
                try {
                    socket.close()
                } catch (e: IOException) {
                    Log.e(TAG, "Не удалось закрыть клиентский сокет", e)
                }
            }
            connectedClients.clear()
            outputStreams.clear()
        }
        Log.d(TAG, "Групповой сервер полностью остановлен.")
    }
}