package com.example.ghostlink.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages")
public class Message {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String text;
    public long timestamp;
    public boolean isMine;

    public Message(String text, long timestamp, boolean isMine) {
        this.text = text;
        this.timestamp = timestamp;
        this.isMine = isMine;
    }
}