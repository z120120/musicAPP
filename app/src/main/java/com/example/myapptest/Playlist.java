package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;

    public Playlist(String name) {
        this.name = name;
    }
}