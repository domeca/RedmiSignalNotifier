package com.example.redmisignalnotifier

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class SignalMonitorService : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var targetMacAddress: String = ""
    private var rssiThreshold: Int = -85
    private var alertSent = false
    private var scanCallback: ScanCallback? = null

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = manager.adapter
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        targetMacAddress = intent?.getStringExtra("EXTRA_MAC_ADDRESS") ?: ""
        rssiThreshold = intent?.getIntExtra("EXTRA_RSSI_THRESHOLD", -85) ?: -85

        if (targetMacAddress.isNotEmpty()) {
            startRssiMonitoring()
        }

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startRssiMonitoring() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        // Detener escaneo anterior si existía
        scanCallback?.let { scanner.stopScan(it) }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val deviceAddress = result.device.address
                if (deviceAddress.equals(targetMacAddress, ignoreCase = true)) {
                    val rssi = result.rssi
                    
                    // Si la señal cae por debajo del umbral, se envía la alerta
                    if (rssi < rssiThreshold && !alertSent) {
                        triggerWatchAlert("¡Alerta! La señal con Redmi Watch es débil ($rssi dBm). Distancia extrema.")
                        alertSent = true
                    } else if (rssi >= rssiThreshold) {
                        alertSent = false // Restablece el estado al recuperar señal
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanner.startScan(null, settings, scanCallback)
    }

    private fun triggerWatchAlert(message: String) {
        val channelId = "watch_alert_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas Redmi Watch",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para enviar al smartwatch mediante Mi Fitness"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Aviso Redmi Watch")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "monitor_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Monitor de Señal BLE",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitor BLE Activo")
            .setContentText("Supervisando la intensidad de señal del smartwatch...")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        scanCallback?.let {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
