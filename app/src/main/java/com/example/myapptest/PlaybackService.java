package com.example.myapptest;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;  // 添加这行
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class PlaybackService extends Service {

    private static final String TAG = "PlaybackService";  // 添加这行

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private List<Music> playlist;
    private int currentIndex = 0;

    public class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setPlaylist(List<Music> playlist) {
        this.playlist = playlist;
    }

    public void play() {
        if (playlist != null && !playlist.isEmpty()) {
            playMusic(currentIndex);
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void next() {
        if (playlist != null && !playlist.isEmpty()) {
            currentIndex = (currentIndex + 1) % playlist.size();
            playMusic(currentIndex);
        }
    }

    public void previous() {
        if (playlist != null && !playlist.isEmpty()) {
            currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
            playMusic(currentIndex);
        }
    }

    private void playMusic(int index) {
        if (playlist != null && index >= 0 && index < playlist.size()) {
            Music music = playlist.get(index);
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(music.filePath);
                mediaPlayer.prepare();
                mediaPlayer.start();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error playing music: " + e.getMessage());
            }
        }
    }

    // 添加这个方法
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}