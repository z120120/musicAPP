package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(primaryKeys = {"playlistId", "musicId"},
        foreignKeys = {
                @ForeignKey(entity = Playlist.class,
                        parentColumns = "id",
                        childColumns = "playlistId"),
                @ForeignKey(entity = Music.class,
                        parentColumns = "id",
                        childColumns = "musicId")
        },
        indices = {@Index("musicId")})  // 添加这一行来创建索引
public class PlaylistSong {
    public int playlistId;
    public int musicId;

    public PlaylistSong(int playlistId, int musicId) {
        this.playlistId = playlistId;
        this.musicId = musicId;
    }
}