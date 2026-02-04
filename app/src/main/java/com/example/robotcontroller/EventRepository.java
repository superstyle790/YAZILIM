package com.example.robotcontroller;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventRepository {
    private final EventDao eventDao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public EventRepository(Context context) {
        eventDao = AppDatabase.getInstance(context).eventDao();
    }

    public LiveData<Integer> getTodayTotal() {
        return eventDao.getTodayTotal();
    }

    public LiveData<Integer> getMonthTotal() {
        return eventDao.getMonthTotal();
    }

    public LiveData<Integer> getYearTotal() {
        return eventDao.getYearTotal();
    }

    public void insert(EventEntity entity) {
        executor.execute(() -> eventDao.insert(entity));
    }
}
