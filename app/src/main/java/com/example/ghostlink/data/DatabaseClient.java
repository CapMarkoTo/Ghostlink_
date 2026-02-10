package com.example.ghostlink.data;

import android.content.Context;
import androidx.room.Room;

public class DatabaseClient {
    private Context mCtx;
    private static DatabaseClient mInstance;

    // Объект нашей базы данных
    private AppDatabase appDatabase;

    private DatabaseClient(Context mCtx) {
        this.mCtx = mCtx;

        // Создаем базу данных GhostLinkDB
        appDatabase = Room.databaseBuilder(mCtx, AppDatabase.class, "GhostLinkDB")
                .fallbackToDestructiveMigration() // Поможет избежать вылетов при обновлении таблиц
                .build();
    }

    public static synchronized DatabaseClient getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new DatabaseClient(mCtx);
        }
        return mInstance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}