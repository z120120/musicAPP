package com.example.myapptest.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.myapptest.AppDatabase;
import com.example.myapptest.Music;
import com.example.myapptest.MusicDao;

import java.util.List;

public class DatabaseUtils {
    private static final String TAG = "DatabaseUtils";

    public static void loadMusicFromDatabase(Context context, OnMusicLoadedListener listener) {
        AppDatabase db = AppDatabase.getDatabase(context);
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                List<Music> music = musicDao.getAllMusic();
                listener.onMusicLoaded(music);
            } catch (Exception e) {
                Log.e(TAG, "loadMusicFromDatabase: 加载音乐失败", e);
                listener.onError(e);
            }
        }).start();
    }

    public interface OnMusicLoadedListener {
        void onMusicLoaded(List<Music> music);
        void onError(Exception e);
    }
}