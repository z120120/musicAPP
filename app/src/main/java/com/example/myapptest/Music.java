package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Objects;  // 添加这行

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Music music = (Music) o;
        return id == music.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}