package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Playlist {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public boolean isDefault;

    public Playlist(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }
}