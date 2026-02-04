package com.example.robotcontroller;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class StatsViewModel extends AndroidViewModel {
    private final EventRepository repository;
    private final LiveData<Integer> dayTotal;
    private final LiveData<Integer> monthTotal;
    private final LiveData<Integer> yearTotal;

    public StatsViewModel(@NonNull Application application) {
        super(application);
        repository = new EventRepository(application);
        dayTotal = repository.getTodayTotal();
        monthTotal = repository.getMonthTotal();
        yearTotal = repository.getYearTotal();
    }

    public LiveData<Integer> getDayTotal() {
        return dayTotal;
    }

    public LiveData<Integer> getMonthTotal() {
        return monthTotal;
    }

    public LiveData<Integer> getYearTotal() {
        return yearTotal;
    }

    public void insert(EventEntity entity) {
        repository.insert(entity);
    }
}
