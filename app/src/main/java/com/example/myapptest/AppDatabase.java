package com.example.myapptest;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Music.class, MusicCollection.class, MusicCollectionSong.class, PlayQueue.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MusicDao musicDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "music_database")
                            .fallbackToDestructiveMigration() // 添加这行
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}