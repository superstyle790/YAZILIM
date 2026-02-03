package com.example.robotcontroller

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var statusText: TextView
    private lateinit var logText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)

        val connectButton: Button = findViewById(R.id.connectButton)
        val forwardButton: Button = findViewById(R.id.forwardButton)
        val backButton: Button = findViewById(R.id.backButton)
        val leftButton: Button = findViewById(R.id.leftButton)
        val rightButton: Button = findViewById(R.id.rightButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        connectButton.setOnClickListener {
            connectToHc06()
        }

        forwardButton.setOnTouchListener { _, event -> handleDirectionalTouch(event, "F") }
        backButton.setOnTouchListener { _, event -> handleDirectionalTouch(event, "B") }
        leftButton.setOnTouchListener { _, event -> handleDirectionalTouch(event, "L") }
        rightButton.setOnTouchListener { _, event -> handleDirectionalTouch(event, "R") }

        stopButton.setOnClickListener {
            sendCommand("S")
        }
    }

    private fun handleDirectionalTouch(event: MotionEvent, command: String): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                sendCommand(command)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                sendCommand("S")
                true
            }
            else -> false
        }
    }

    private fun connectToHc06() {
        if (bluetoothAdapter == null) {
            showToast("Bluetooth desteklenmiyor")
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            showToast("Bluetooth'u açtıktan sonra tekrar deneyin")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            if (!hasPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, 101)
                return
            }
        }

        val device = findPairedHc06()
        if (device == null) {
            showToast("Eşleşmiş HC-06 bulunamadı")
            return
        }

        statusText.text = "Durum: Bağlanıyor..."
        Thread {
            try {
                val uuid = UUID.fromString(SPP_UUID)
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothAdapter.cancelDiscovery()
                bluetoothSocket?.connect()

                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream

                mainHandler.post {
                    statusText.text = "Durum: Bağlandı (${device.name})"
                    showToast("Bağlantı kuruldu")
                }

                startListening()
            } catch (e: Exception) {
                mainHandler.post {
                    statusText.text = "Durum: Bağlantı başarısız"
                    showToast("Bağlantı hatası: ${e.message}")
                }
            }
        }.start()
    }

    private fun findPairedHc06(): BluetoothDevice? {
        val pairedDevices = bluetoothAdapter?.bondedDevices ?: return null
        return pairedDevices.firstOrNull { it.name?.contains("HC-06", ignoreCase = true) == true }
            ?: pairedDevices.firstOrNull()
    }

    private fun sendCommand(command: String) {
        try {
            outputStream?.write(command.toByteArray())
        } catch (e: Exception) {
            showToast("Komut gönderilemedi: ${e.message}")
        }
    }

    private fun startListening() {
        val buffer = ByteArray(1024)
        Thread {
            while (true) {
                try {
                    val count = inputStream?.read(buffer) ?: -1
                    if (count > 0) {
                        val message = String(buffer, 0, count)
                        mainHandler.post {
                            logText.append(message)
                        }
                    }
                } catch (e: Exception) {
                    mainHandler.post {
                        statusText.text = "Durum: Bağlantı koptu"
                    }
                    break
                }
            }
        }.start()
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        private const val SPP_UUID = "00001101-0000-1000-8000-00805F9B34FB"
    }
}
