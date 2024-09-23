package com.example.myapptest;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;  // 添加这行
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;  // 添加这行
import androidx.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Random;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    private static final String TAG = "PlaybackService";  // 添加这行

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private List<Music> playlist;
    private int currentIndex = 0;
    private int playMode = 0; // 0: 列表循环, 1: 单曲循环, 2: 随机播放
    private Random random = new Random();
    private int lastPosition = 0; // 添加这个字段来保存暂停时的位置

    public class LocalBinder extends Binder {
        PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
            return false;
        });
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
            if (lastPosition > 0) {
                mediaPlayer.seekTo(lastPosition); // 从暂停的位置继续播放
                mediaPlayer.start();
                lastPosition = 0; // 重置 lastPosition
            } else {
                playMusic(currentIndex);
            }
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            lastPosition = mediaPlayer.getCurrentPosition(); // 保存当前播放位置
            mediaPlayer.pause();
        }
    }

    public void next() {
        if (playlist != null && !playlist.isEmpty()) {
            if (playMode == 2) { // 随机播放
                currentIndex = random.nextInt(playlist.size());
            } else {
                currentIndex = (currentIndex + 1) % playlist.size();
            }
            playMusic(currentIndex);
        }
    }

    public void previous() {
        if (playlist != null && !playlist.isEmpty()) {
            if (playMode == 2) { // 随机播放
                currentIndex = random.nextInt(playlist.size());
            } else {
                currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
            }
            playMusic(currentIndex);
        }
    }

    public void setPlayMode(int mode) {
        this.playMode = mode;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        switch (playMode) {
            case 0: // 列表循环
                next();
                break;
            case 1: // 单曲循环
                playMusic(currentIndex);
                break;
            case 2: // 随机播放
                int nextIndex = random.nextInt(playlist.size());
                playMusic(nextIndex);
                break;
        }
        if (songChangeListener != null) {
            songChangeListener.onAutoPlayNext(getCurrentSongTitle());
        }
    }

    // 添加 OnSongChangeListener 接口
    public interface OnSongChangeListener {
        void onSongChange(String title);
        void onAutoPlayNext(String title); // 添加这个新方法
    }

    private OnSongChangeListener songChangeListener;

    public void setOnSongChangeListener(OnSongChangeListener listener) {
        this.songChangeListener = listener;
    }

    private void notifySongChange(String title) {
        if (songChangeListener != null) {
            songChangeListener.onSongChange(stripFileExtension(title)); // 修改这一行，去除后缀
        }
    }

    private String stripFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private void playMusic(int index) {
        Log.d(TAG, "playMusic: 尝试播放音乐，索引: " + index);
        if (playlist != null && index >= 0 && index < playlist.size()) {
            Music music = playlist.get(index);
            Log.d(TAG, "playMusic: 准备播放 " + music.title + ", 文件路径: " + music.filePath);
            try {
                mediaPlayer.reset();
                Uri uri = Uri.parse(music.filePath);
                mediaPlayer.setDataSource(getApplicationContext(), uri);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    Log.d(TAG, "playMusic: 音乐开始播放");
                    notifySongChange(music.title); // 通知歌曲变化
                });
                // 移除这里的 setOnCompletionListener，因为我们已经在 onCreate 中设置了全局的 OnCompletionListener
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Error playing music: " + e.getMessage());
            }
        } else {
            Log.e(TAG, "playMusic: 无效的播放列表或索引");
        }
    }

    // 添加设置当前索引的方法
    public void setCurrentIndex(int index) {
        this.currentIndex = index;
    }

    // 添加这个方法
    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public String getCurrentSongTitle() {
        if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex).title;
        }
        return "";
    }

    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public void seekTo(int position) {
        mediaPlayer.seekTo(position);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public int getPlayMode() {
        return playMode;
    }

    // 添加获取当前播放音乐信息的方法
    public Music getCurrentMusic() {
        if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
            return playlist.get(currentIndex);
        }
        return null;
    }
}