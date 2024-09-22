package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class PlayQueue {
    @PrimaryKey
    public int id = 1; // 只有一个播放队列

    public int currentIndex;

    public PlayQueue() {
        this.currentIndex = 0;
    }
}