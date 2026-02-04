package com.example.robotcontroller;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.robotcontroller.databinding.FragmentDashboardBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public class DashboardFragment extends Fragment implements BluetoothSPPClient.Callback {
    private FragmentDashboardBinding binding;
    private BluetoothSPPClient client;
    private SharedPreferences preferences;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private long lastTelemetryTimeMs = 0L;
    private char lastCommand = '\0';
    private StatsViewModel statsViewModel;

    private final Runnable updateTicker = new Runnable() {
        @Override
        public void run() {
            if (binding != null && lastTelemetryTimeMs > 0) {
                long diffSeconds = (System.currentTimeMillis() - lastTelemetryTimeMs) / 1000;
                binding.textLastUpdate.setText(String.format(Locale.getDefault(),
                        "Son güncelleme: %d sn önce", diffSeconds));
            }
            timerHandler.postDelayed(this, 1000);
        }
    };

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    attemptConnect();
                } else if (binding != null) {
                    binding.chipStatus.setText("DISCONNECTED");
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = requireContext().getSharedPreferences("robot_prefs", Context.MODE_PRIVATE);
        statsViewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        client = BluetoothManager.getInstance(requireContext()).getClient();
        BluetoothManager.getInstance(requireContext()).setListener(this);
        client.setAutoReconnect(preferences.getBoolean("auto_reconnect", true));

        binding.joystickView.setListener(new JoystickView.JoystickListener() {
            @Override
            public void onDirection(char command) {
                if (command != lastCommand) {
                    client.sendLine(String.valueOf(command));
                    lastCommand = command;
                }
            }

            @Override
            public void onStop() {
                client.sendLine("S");
                lastCommand = '\0';
            }
        });

        binding.sliderSpeed.addOnSliderTouchListener(new com.google.android.material.slider.Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {
                // no-op
            }

            @Override
            public void onStopTrackingTouch(@NonNull com.google.android.material.slider.Slider slider) {
                int value = Math.round(slider.getValue());
                client.sendLine("V:" + value);
            }
        });

        binding.buttonConnect.setOnClickListener(v -> ensureBluetoothPermission());
        binding.buttonCollect.setOnClickListener(v -> {
            EventEntity entity = new EventEntity(System.currentTimeMillis(), "COLLECT", 1, "Aegean");
            statsViewModel.insert(entity);
        });

        updateStatusUi(BluetoothSPPClient.Status.DISCONNECTED);
        timerHandler.post(updateTicker);
    }

    private void ensureBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return;
            }
        }
        attemptConnect();
    }

    private void attemptConnect() {
        String address = preferences.getString("device_address", null);
        if (address == null) {
            binding.textTelemetry.setText("HC-06 seçilmedi. Ayarlar > Cihaz seçin.");
            return;
        }
        client.setAutoReconnect(preferences.getBoolean("auto_reconnect", true));
        client.connect(address);
    }

    @Override
    public void onStatus(BluetoothSPPClient.Status status) {
        updateStatusUi(status);
    }

    @Override
    public void onLineReceived(String line) {
        parseTelemetry(line);
    }

    @Override
    public void onError(String message) {
        if (binding != null) {
            binding.textTelemetry.setText(message);
        }
    }

    private void updateStatusUi(BluetoothSPPClient.Status status) {
        if (binding == null) {
            return;
        }
        binding.progressConnecting.setVisibility(status == BluetoothSPPClient.Status.CONNECTING ? View.VISIBLE : View.GONE);
        binding.chipStatus.setText(status.name());
    }

    private void parseTelemetry(String line) {
        if (binding == null) {
            return;
        }
        binding.textTelemetry.setText(line);
        try {
            JSONObject json = new JSONObject(line);
            String type = json.optString("t", "tele");
            if ("tele".equals(type)) {
                int bin = json.optInt("bin", 0);
                int bat = json.optInt("bat", 0);
                int up = json.optInt("up", 0);
                int rem = json.optInt("rem", 0);
                int fault = json.optInt("fault", 0);

                binding.textBinValue.setText(bin + "%");
                binding.progressBin.setProgress(bin);
                binding.textBatteryValue.setText(bat + "%");
                binding.progressBattery.setProgress(bat);
                binding.textUptime.setText(formatDuration(up));
                binding.textRemaining.setText(formatDuration(rem));
                lastTelemetryTimeMs = System.currentTimeMillis();

                if (fault != 0) {
                    NotificationUtils.showNotification(requireContext(), "Robot Arızası", "Fault code: " + fault);
                }
            } else if ("event".equals(type)) {
                String name = json.optString("name", "");
                if ("DOCKED".equals(name)) {
                    NotificationUtils.showNotification(requireContext(), "Robot", "Platforma geldi");
                } else if ("FAULT".equals(name)) {
                    int code = json.optInt("code", 0);
                    String msg = json.optString("msg", "");
                    NotificationUtils.showNotification(requireContext(), "Robot Arızası",
                            "Code " + code + ": " + msg);
                    statsViewModel.insert(new EventEntity(System.currentTimeMillis(), "FAULT", 0, ""));
                }
            }
        } catch (JSONException exception) {
            binding.textTelemetry.setText("JSON hata: " + exception.getMessage());
        }
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        timerHandler.removeCallbacks(updateTicker);
        BluetoothManager.getInstance(requireContext()).setListener(null);
        binding = null;
    }
}
