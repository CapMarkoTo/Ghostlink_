package com.example.ghostlink.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface MessageDao {

    // Сохранить сообщение
    @Insert
    void insert(Message message);

    // Получить все сообщения из таблицы 'messages'
    @Query("SELECT * FROM messages")
    List<Message> getAllMessages();
}