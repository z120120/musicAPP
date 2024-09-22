package com.example.myapptest;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface MusicDao {
    @Insert
    void insert(Music music);

    @Query("SELECT * FROM music")
    List<Music> getAllMusic();

    @Query("DELETE FROM music")
    void deleteAll();
}