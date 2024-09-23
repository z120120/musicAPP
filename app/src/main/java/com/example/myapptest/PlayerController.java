package com.example.myapptest;

import android.content.Context;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.widget.SeekBar;

public class PlayerController {
    private PlaybackService playbackService;
    private Context context;
    private TextView songTitleView;
    private ImageButton playPauseButton;
    private ImageButton playModeButton;
    private int playMode = 0; // 0: 列表循环, 1: 单曲循环, 2: 随机播放
    private SeekBar seekBar;
    private TextView currentTimeView;
    private TextView totalTimeView;
    private Handler handler = new Handler();
    private Runnable progressUpdateRunnable;
    private TextView artistView;
    private TextView albumView;

    public PlayerController(Context context, PlaybackService playbackService, TextView songTitleView, 
                            ImageButton playPauseButton, ImageButton playModeButton,
                            SeekBar seekBar, TextView currentTimeView, TextView totalTimeView,
                            TextView artistView, TextView albumView) {
        this.context = context;
        this.playbackService = playbackService;
        this.songTitleView = songTitleView;
        this.playPauseButton = playPauseButton;
        this.playModeButton = playModeButton;
        this.seekBar = seekBar;
        this.currentTimeView = currentTimeView;
        this.totalTimeView = totalTimeView;
        this.artistView = artistView;
        this.albumView = albumView;
    }

    public void setPlaybackService(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    public void togglePlayPause() {
        if (playbackService == null) return;
        if (playbackService.isPlaying()) {
            playbackService.pause();
        } else {
            playbackService.play();
        }
        updatePlayPauseButton();
    }

    public void playPrevious() {
        if (playbackService == null) return;
        playbackService.previous();
        updateSongTitle();
    }

    public void playNext() {
        if (playbackService == null) return;
        playbackService.next();
        updateSongTitle();
    }

    public void changePlayMode() {
        if (playbackService == null || playModeButton == null) return;
        int currentPlayMode = playbackService.getPlayMode();
        int newPlayMode = (currentPlayMode + 1) % 3;
        playbackService.setPlayMode(newPlayMode);
        updatePlayModeButton();
        switch (newPlayMode) {
            case 0:
                Toast.makeText(context, "列表循环", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(context, "单曲循环", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(context, "随机播放", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void updateSongTitle() {
        if (playbackService == null || songTitleView == null) return;
        String title = playbackService.getCurrentSongTitle();
        String strippedTitle = stripFileExtension(title);
        songTitleView.setText(strippedTitle);
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

    public void startProgressUpdate() {
        progressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                if (playbackService != null) {
                    int currentPosition = playbackService.getCurrentPosition();
                    int duration = playbackService.getDuration();
                    updateProgressBar(currentPosition, duration);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(progressUpdateRunnable, 1000);
    }

    public void stopProgressUpdate() {
        handler.removeCallbacks(progressUpdateRunnable);
    }

    private void updateProgressBar(int currentPosition, int duration) {
        seekBar.setMax(duration);
        seekBar.setProgress(currentPosition);
        currentTimeView.setText(formatTime(currentPosition));
        totalTimeView.setText(formatTime(duration));
    }

    private String formatTime(int milliseconds) {
        int seconds = milliseconds / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void updatePlayPauseButton() {
        if (playbackService == null || playPauseButton == null) return;
        if (playbackService.isPlaying()) {
            playPauseButton.setImageResource(R.drawable.ic_pause);
        } else {
            playPauseButton.setImageResource(R.drawable.ic_play);
        }
    }

    public void updatePlayModeButton() {
        if (playbackService == null || playModeButton == null) return;
        int currentPlayMode = playbackService.getPlayMode();
        switch (currentPlayMode) {
            case 0:
                playModeButton.setImageResource(R.drawable.ic_repeat);
                break;
            case 1:
                playModeButton.setImageResource(R.drawable.ic_repeat_one);
                break;
            case 2:
                playModeButton.setImageResource(R.drawable.ic_shuffle);
                break;
        }
    }

    public void updateUIForNewSong() {
        updateSongTitle();
        updateArtistAndAlbum();
        updatePlayPauseButton();
        updatePlayModeButton();
    }

    private void updateArtistAndAlbum() {
        if (playbackService == null || artistView == null || albumView == null) return;
        Music currentMusic = playbackService.getCurrentMusic();
        if (currentMusic != null) {
            artistView.setText(currentMusic.artist);
            albumView.setText(currentMusic.album);
        }
    }
}