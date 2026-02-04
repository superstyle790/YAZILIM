package com.example.robotcontroller;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.robotcontroller.databinding.FragmentSettingsBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;
    private final Map<String, String> deviceMap = new HashMap<>();

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted) {
                    loadBondedDevices();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        preferences = requireContext().getSharedPreferences("robot_prefs", Context.MODE_PRIVATE);

        binding.switchReconnect.setChecked(preferences.getBoolean("auto_reconnect", true));
        binding.switchReconnect.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferences.edit().putBoolean("auto_reconnect", isChecked).apply());

        binding.buttonDisconnect.setOnClickListener(v ->
                BluetoothManager.getInstance(requireContext()).getClient().disconnect());

        ensurePermissionAndLoad();
    }

    private void ensurePermissionAndLoad() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
                return;
            }
        }
        loadBondedDevices();
    }

    private void loadBondedDevices() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        List<String> names = new ArrayList<>();
        deviceMap.clear();
        if (adapter != null) {
            for (BluetoothDevice device : adapter.getBondedDevices()) {
                String name = device.getName() + " (" + device.getAddress() + ")";
                names.add(name);
                deviceMap.put(name, device.getAddress());
            }
        }

        names.sort((a, b) -> {
            boolean aIsHC = a.toLowerCase().contains("hc-06");
            boolean bIsHC = b.toLowerCase().contains("hc-06");
            if (aIsHC && !bIsHC) {
                return -1;
            }
            if (!aIsHC && bIsHC) {
                return 1;
            }
            return a.compareTo(b);
        });

        android.widget.ArrayAdapter<String> adapterList = new android.widget.ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, names);
        binding.autoCompleteDevices.setAdapter(adapterList);

        String savedAddress = preferences.getString("device_address", null);
        if (savedAddress != null) {
            for (String display : names) {
                if (savedAddress.equals(deviceMap.get(display))) {
                    binding.autoCompleteDevices.setText(display, false);
                    break;
                }
            }
        }

        binding.autoCompleteDevices.setOnItemClickListener((parent, view, position, id) -> {
            String selection = adapterList.getItem(position);
            if (selection != null) {
                String address = deviceMap.get(selection);
                preferences.edit().putString("device_address", address).apply();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
