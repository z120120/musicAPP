package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class MusicCollection {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public boolean isDefault;

    public MusicCollection(String name, boolean isDefault) {
        this.name = name;
        this.isDefault = isDefault;
    }
}