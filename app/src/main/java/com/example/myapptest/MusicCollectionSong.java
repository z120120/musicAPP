package com.example.myapptest;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
    primaryKeys = {"collectionId", "musicId"},
    foreignKeys = {
        @ForeignKey(
            entity = MusicCollection.class,
            parentColumns = "id",
            childColumns = "collectionId"
        ),
        @ForeignKey(
            entity = Music.class,
            parentColumns = "id",
            childColumns = "musicId",
            onDelete = ForeignKey.CASCADE // 添加这个以确保删除Music时相关联的MusicCollectionSong也被删除
        )
    },
    indices = {@Index("musicId")} // 添加这一行来创建索引
)
public class MusicCollectionSong {
    public int collectionId;
    public int musicId;
    public int order;

    public MusicCollectionSong(int collectionId, int musicId, int order) {
        this.collectionId = collectionId;
        this.musicId = musicId;
        this.order = order;
    }
}