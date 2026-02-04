package com.example.robotcontroller;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.robotcontroller.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            switchToFragment(new DashboardFragment());
        }

        binding.bottomNavigation.setOnItemSelectedListener(navListener);
    }

    private final NavigationBarView.OnItemSelectedListener navListener = item -> {
        Fragment fragment;
        int itemId = item.getItemId();
        if (itemId == R.id.menu_dashboard) {
            fragment = new DashboardFragment();
        } else if (itemId == R.id.menu_stats) {
            fragment = new StatisticsFragment();
        } else {
            fragment = new SettingsFragment();
        }
        switchToFragment(fragment);
        return true;
    };

    private void switchToFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
