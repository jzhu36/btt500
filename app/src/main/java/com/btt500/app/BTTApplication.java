package com.btt500.app;

import android.app.Application;

import com.btt500.app.data.AppDatabase;

public class BTTApplication extends Application {

    private static BTTApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static BTTApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return AppDatabase.getInstance(this);
    }
}
