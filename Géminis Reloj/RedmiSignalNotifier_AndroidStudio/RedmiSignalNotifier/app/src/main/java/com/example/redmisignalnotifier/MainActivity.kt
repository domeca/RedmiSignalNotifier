package com.example.redmisignalnotifier

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.redmisignalnotifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartService.setOnClickListener {
            if (checkAndRequestPermissions()) {
                startMonitoringService()
            }
        }

        binding.btnStopService.setOnClickListener {
            val intent = Intent(this, SignalMonitorService::class.java)
            stopService(intent)
            Toast.makeText(this, "Servicio detenido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startMonitoringService() {
        val mac = binding.etMacAddress.text.toString().trim()
        val rssiStr = binding.etRssiThreshold.text.toString().trim()

        if (mac.isEmpty()) {
            Toast.makeText(this, "Ingresa la dirección MAC del reloj", Toast.LENGTH_SHORT).show()
            return
        }

        val rssiThreshold = rssiStr.toIntOrNull() ?: -85

        val intent = Intent(this, SignalMonitorService::class.java).apply {
            putExtra("EXTRA_MAC_ADDRESS", mac)
            putExtra("EXTRA_RSSI_THRESHOLD", rssiThreshold)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        Toast.makeText(this, "Monitoreo iniciado para $mac", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val listPermissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (listPermissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toTypedArray(), 100)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startMonitoringService()
            } else {
                Toast.makeText(this, "Se requieren permisos para escanear Bluetooth", Toast.LENGTH_LONG).show()
            }
        }
    }
}
