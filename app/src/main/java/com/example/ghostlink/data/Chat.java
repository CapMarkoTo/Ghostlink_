package com.example.ghostlink.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chats") // Room должна знать, что это таблица
public class Chat {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String chatName;
    public String lastMessage;
    public long timestamp;

    public Chat(String chatName, String lastMessage, long timestamp) {
        this.chatName = chatName;
        this.lastMessage = lastMessage;
        this.timestamp = timestamp;
    }
}