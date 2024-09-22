package com.example.myapptest;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface MusicDao {
    @Insert
    void insert(Music music);

    @Query("SELECT * FROM music")
    List<Music> getAllMusic();

    @Query("DELETE FROM music")
    void deleteAll();

    @Insert
    long insertMusicCollection(MusicCollection collection);

    @Query("SELECT * FROM MusicCollection WHERE isDefault = 1 LIMIT 1")
    MusicCollection getDefaultCollection();

    @Insert
    void insertMusicCollectionSong(MusicCollectionSong collectionSong);

    @Query("SELECT m.* FROM Music m INNER JOIN MusicCollectionSong mcs ON m.id = mcs.musicId WHERE mcs.collectionId = :collectionId ORDER BY mcs.`order`")
    List<Music> getMusicInCollection(int collectionId);

    @Insert
    void insertPlayQueue(PlayQueue playQueue);

    @Query("SELECT * FROM PlayQueue LIMIT 1")
    PlayQueue getPlayQueue();

    @Query("UPDATE PlayQueue SET currentIndex = :index")
    void updatePlayQueueIndex(int index);
}