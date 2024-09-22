package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Music {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String filePath;

    public Music(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }
}