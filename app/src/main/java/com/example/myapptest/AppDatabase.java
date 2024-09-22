package com.example.myapptest;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Music.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract MusicDao musicDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "music_database")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}