package com.example.robotcontroller;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "events")
public class EventEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long ts;

    public String type;

    public int amount;

    public String sea;

    public EventEntity(long ts, String type, int amount, String sea) {
        this.ts = ts;
        this.type = type;
        this.amount = amount;
        this.sea = sea;
    }
}
