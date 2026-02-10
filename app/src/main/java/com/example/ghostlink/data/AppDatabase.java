package com.example.ghostlink.data;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.ghostlink.data.Message;
import com.example.ghostlink.data.MessageDao;

// Убедись, что список сущностей (entities) указан верно
@Database(entities = {Message.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {

    public abstract MessageDao messageDao();

    // --- Добавь этот блок для исправления ошибки ---
    private static AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "ghostlink_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    // ------------------------------------------------
}