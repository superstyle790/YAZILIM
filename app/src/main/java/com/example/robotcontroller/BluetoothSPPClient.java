package com.example.robotcontroller;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothSPPClient {
    public enum Status {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    public interface Callback {
        void onStatus(Status status);
        void onLineReceived(String line);
        void onError(String message);
    }

    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Callback callback;

    private BluetoothSocket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Status status = Status.DISCONNECTED;
    private boolean autoReconnect = true;
    private String lastAddress;
    private int reconnectAttempts = 0;

    public BluetoothSPPClient(BluetoothAdapter adapter, Callback callback) {
        this.adapter = adapter;
        this.callback = callback;
    }

    public void setAutoReconnect(boolean enabled) {
        this.autoReconnect = enabled;
    }

    public Status getStatus() {
        return status;
    }

    public void connect(String address) {
        lastAddress = address;
        updateStatus(Status.CONNECTING);
        executor.execute(() -> {
            try {
                if (adapter == null) {
                    postError("Bluetooth adapter yok");
                    updateStatus(Status.DISCONNECTED);
                    return;
                }
                adapter.cancelDiscovery();
                BluetoothDevice device = adapter.getRemoteDevice(address);
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                socket.connect();
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                reconnectAttempts = 0;
                updateStatus(Status.CONNECTED);
                readLoop();
            } catch (IOException exception) {
                postError("Bağlantı hatası: " + exception.getMessage());
                disconnectInternal();
                scheduleReconnect();
            }
        });
    }

    public void disconnect() {
        autoReconnect = false;
        executor.execute(this::disconnectInternal);
    }

    public void sendLine(String line) {
        executor.execute(() -> {
            if (writer == null) {
                return;
            }
            try {
                writer.write(line + "\n");
                writer.flush();
            } catch (IOException exception) {
                postError("Gönderim hatası: " + exception.getMessage());
                disconnectInternal();
                scheduleReconnect();
            }
        });
    }

    private void readLoop() {
        try {
            String line;
            while (status == Status.CONNECTED && (line = reader.readLine()) != null) {
                String finalLine = line;
                mainHandler.post(() -> callback.onLineReceived(finalLine));
            }
        } catch (IOException exception) {
            postError("Okuma hatası: " + exception.getMessage());
        } finally {
            disconnectInternal();
            scheduleReconnect();
        }
    }

    private void disconnectInternal() {
        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (IOException ignored) {
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        reader = null;
        writer = null;
        socket = null;
        updateStatus(Status.DISCONNECTED);
    }

    private void scheduleReconnect() {
        if (!autoReconnect || lastAddress == null) {
            return;
        }
        reconnectAttempts++;
        long delayMs = Math.min(30000, (long) Math.pow(2, reconnectAttempts) * 1000L);
        mainHandler.postDelayed(() -> connect(lastAddress), delayMs);
    }

    private void updateStatus(Status newStatus) {
        status = newStatus;
        mainHandler.post(() -> callback.onStatus(newStatus));
    }

    private void postError(String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
