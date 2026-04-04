package com.example.ghostlink.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface ChatDao {
    @Query("SELECT * FROM chats")
    List<Chat> getAllChats();

    @Insert
    void insert(Chat chat);

    @Query("SELECT * FROM chats WHERE chatName = :name LIMIT 1")
    Chat getChatByName(String name);
}