package com.example.robotcontroller;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface EventDao {
    @Insert
    long insert(EventEntity event);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM events WHERE type = 'COLLECT' AND date(ts / 1000, 'unixepoch') = date('now')")
    LiveData<Integer> getTodayTotal();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM events WHERE type = 'COLLECT' AND strftime('%Y-%m', ts / 1000, 'unixepoch') = strftime('%Y-%m', 'now')")
    LiveData<Integer> getMonthTotal();

    @Query("SELECT COALESCE(SUM(amount), 0) FROM events WHERE type = 'COLLECT' AND strftime('%Y', ts / 1000, 'unixepoch') = strftime('%Y', 'now')")
    LiveData<Integer> getYearTotal();
}
