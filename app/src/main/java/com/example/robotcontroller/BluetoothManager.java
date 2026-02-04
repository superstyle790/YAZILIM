package com.example.robotcontroller;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;

public class BluetoothManager {
    private static volatile BluetoothManager INSTANCE;
    private final BluetoothSPPClient client;

    private BluetoothManager(Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        client = new BluetoothSPPClient(adapter, new BluetoothSPPClient.Callback() {
            @Override
            public void onStatus(BluetoothSPPClient.Status status) {
                if (listener != null) {
                    listener.onStatus(status);
                }
            }

            @Override
            public void onLineReceived(String line) {
                if (listener != null) {
                    listener.onLineReceived(line);
                }
            }

            @Override
            public void onError(String message) {
                if (listener != null) {
                    listener.onError(message);
                }
            }
        });
    }

    private BluetoothSPPClient.Callback listener;

    public static BluetoothManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BluetoothManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluetoothManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public void setListener(BluetoothSPPClient.Callback callback) {
        this.listener = callback;
    }

    public BluetoothSPPClient getClient() {
        return client;
    }
}
