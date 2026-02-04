package com.example.robotcontroller;

import android.app.Application;

public class RobotControllerApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificationUtils.createChannels(this);
    }
}
