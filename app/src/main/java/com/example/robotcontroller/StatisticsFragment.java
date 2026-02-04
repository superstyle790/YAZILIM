package com.example.robotcontroller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.robotcontroller.databinding.FragmentStatisticsBinding;
import com.google.android.material.tabs.TabLayout;

public class StatisticsFragment extends Fragment {
    private FragmentStatisticsBinding binding;
    private StatsViewModel viewModel;
    private int dayTotal = 0;
    private int monthTotal = 0;
    private int yearTotal = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatsViewModel.class);

        viewModel.getDayTotal().observe(getViewLifecycleOwner(), total -> {
            dayTotal = total == null ? 0 : total;
            updateTotalDisplay();
        });
        viewModel.getMonthTotal().observe(getViewLifecycleOwner(), total -> {
            monthTotal = total == null ? 0 : total;
            updateTotalDisplay();
        });
        viewModel.getYearTotal().observe(getViewLifecycleOwner(), total -> {
            yearTotal = total == null ? 0 : total;
            updateTotalDisplay();
        });

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Gün"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Ay"));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Yıl"));

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTotalDisplay();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // no-op
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateTotalDisplay();
            }
        });

        updateTotalDisplay();
    }

    private void updateTotalDisplay() {
        if (binding == null) {
            return;
        }
        int position = binding.tabLayout.getSelectedTabPosition();
        int total;
        if (position == 1) {
            total = monthTotal;
        } else if (position == 2) {
            total = yearTotal;
        } else {
            total = dayTotal;
        }
        binding.textTotal.setText(String.valueOf(total));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
