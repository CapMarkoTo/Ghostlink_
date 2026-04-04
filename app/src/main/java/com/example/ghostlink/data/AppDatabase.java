package com.example.ghostlink.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Message.class, Chat.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MessageDao messageDao();
    public abstract ChatDao chatDao();

    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ghostlink_db")
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }
}