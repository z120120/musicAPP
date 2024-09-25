package com.example.myapptest;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy; // 添加这行
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertMusicCollectionSong(MusicCollectionSong collectionSong);

    @Query("SELECT m.* FROM Music m INNER JOIN MusicCollectionSong mcs ON m.id = mcs.musicId WHERE mcs.collectionId = :collectionId ORDER BY mcs.`order`")
    List<Music> getMusicInCollection(int collectionId);

    @Insert
    void insertPlayQueue(PlayQueue playQueue);

    @Query("SELECT * FROM PlayQueue LIMIT 1")
    PlayQueue getPlayQueue();

    @Query("UPDATE PlayQueue SET currentIndex = :index")
    void updatePlayQueueIndex(int index);

    @Update
    void updateMusic(Music music);

    @Query("SELECT * FROM music WHERE isFavorite = 1")
    List<Music> getFavoriteSongs();

    @Update
    void updateMusicList(List<Music> musicList);

    @Insert
    long insertPlaylist(Playlist playlist);

    @Query("SELECT * FROM Playlist")
    List<Playlist> getAllPlaylists();

    @Query("UPDATE Playlist SET name = :newName WHERE id = :playlistId")
    void updatePlaylistName(int playlistId, String newName);

    @Query("DELETE FROM Playlist WHERE id = :playlistId")
    void deletePlaylist(int playlistId);

    @Insert
    long insertPlaylistSong(PlaylistSong playlistSong);

    @Query("SELECT m.* FROM Music m INNER JOIN PlaylistSong ps ON m.id = ps.musicId WHERE ps.playlistId = :playlistId")
    List<Music> getMusicInPlaylist(int playlistId);

    @Query("DELETE FROM PlaylistSong WHERE playlistId = :playlistId")
    void deletePlaylistSongs(int playlistId);

    @Query("DELETE FROM PlaylistSong WHERE playlistId = :playlistId AND musicId = :musicId")
    void deletePlaylistSong(int playlistId, int musicId);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertPlaylistSongs(List<PlaylistSong> playlistSongs);
}