package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Music {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String filePath;
    public String artist;  // 添加这行
    public String album;   // 添加这行
    public boolean isFavorite; // 新增字段

    public Music(String title, String filePath, String artist, String album) {
        this.title = title;
        this.filePath = filePath;
        this.artist = artist;
        this.album = album;
        this.isFavorite = false; // 默认不是喜爱
    }
}