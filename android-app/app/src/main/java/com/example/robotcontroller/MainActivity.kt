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
import android.view.View
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
    private var lastCommand: String = "S"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)

        val connectButton: Button = findViewById(R.id.connectButton)
        connectButton.setOnClickListener {
            connectToHc06()
        }

        val joystickArea: View = findViewById(R.id.joystickArea)
        joystickArea.setOnTouchListener { view, event ->
            handleJoystickTouch(view, event)
        }
    }

    private fun handleJoystickTouch(view: View, event: MotionEvent): Boolean {
        val centerX = view.width / 2f
        val centerY = view.height / 2f
        val dx = event.x - centerX
        val dy = event.y - centerY

        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val deadZone = (view.width.coerceAtMost(view.height) * 0.15f)

        return when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val command = if (distance < deadZone) {
                    "S"
                } else if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) {
                    if (dx > 0) "R" else "L"
                } else {
                    if (dy > 0) "B" else "F"
                }
                sendCommandIfChanged(command)
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                sendCommandIfChanged("S")
                true
            }
            else -> false
        }
    }

    private fun sendCommandIfChanged(command: String) {
        if (lastCommand == command) {
            return
        }
        lastCommand = command
        sendCommand(command)
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
