package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Music {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String filePath;
    // 可以添加其他属性，如艺术家、专辑等

    public Music(String title, String filePath) {
        this.title = title;
        this.filePath = filePath;
    }
}