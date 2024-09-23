package com.example.myapptest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;

public class FullScreenPlayerActivity extends AppCompatActivity implements PlaybackService.OnSongChangeListener {

    private TextView songTitleView;
    private TextView artistView;
    private TextView albumView;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton playModeButton;
    private SeekBar seekBar;
    private TextView currentTimeView;
    private TextView totalTimeView;

    private PlaybackService playbackService;
    private boolean serviceBound = false;
    private PlayerController playerController;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
            playerController.setPlaybackService(playbackService);
            initializePlayerController();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private Handler handler = new Handler();
    private Runnable progressUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_player);

        try {
            // 初始化视图
            initializeViews();

            // 初始化 PlayerController (但不要设置 PlaybackService)
            playerController = new PlayerController(this, null, songTitleView, 
                                                    playPauseButton, playModeButton, 
                                                    seekBar, currentTimeView, totalTimeView,
                                                    artistView, albumView);

            // 绑定服务
            Intent intent = new Intent(this, PlaybackService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            // 启动进度更新任务
            playerController.startProgressUpdate();

            // 初始化时更新播放/暂停按钮状态
            playerController.updatePlayPauseButton();

        } catch (Exception e) {
            Log.e("FullScreenPlayerActivity", "初始化全屏播放器时出错", e);
            Toast.makeText(this, "初始化全屏播放器失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish(); // 关闭活动
        }
    }

    private void initializeViews() {
        songTitleView = findViewById(R.id.full_screen_song_title);
        artistView = findViewById(R.id.full_screen_artist);
        albumView = findViewById(R.id.full_screen_album);
        playPauseButton = findViewById(R.id.full_screen_play_pause_button);
        previousButton = findViewById(R.id.full_screen_previous_button);
        nextButton = findViewById(R.id.full_screen_next_button);
        playModeButton = findViewById(R.id.full_screen_play_mode_button);
        seekBar = findViewById(R.id.full_screen_seek_bar);
        currentTimeView = findViewById(R.id.full_screen_current_time);
        totalTimeView = findViewById(R.id.full_screen_total_time);

        // 检查是否所有视图都正确初始化
        if (songTitleView == null || artistView == null || albumView == null ||
            playPauseButton == null || previousButton == null || nextButton == null ||
            playModeButton == null || seekBar == null ||
            currentTimeView == null || totalTimeView == null) {
            throw new IllegalStateException("一个或多个视图未能正确初始化");
        }

        // 从 Intent 中获取歌曲信息并显示
        String songTitle = getIntent().getStringExtra("songTitle");
        String artist = getIntent().getStringExtra("artist");
        String album = getIntent().getStringExtra("album");
        if (songTitle != null && !songTitle.isEmpty()) {
            songTitleView.setText(songTitle);
        }
        if (artist != null && !artist.isEmpty()) {
            artistView.setText(artist);
        }
        if (album != null && !album.isEmpty()) {
            albumView.setText(album);
        }
    }

    private void initializePlayerController() {
        if (playbackService == null) {
            Log.e("FullScreenPlayerActivity", "PlaybackService 为 null");
            Toast.makeText(this, "播放服务未就绪", Toast.LENGTH_SHORT).show();
            return;
        }

        playerController.setPlaybackService(playbackService);
        playbackService.setOnSongChangeListener(this);

        playPauseButton.setOnClickListener(v -> playerController.togglePlayPause());
        previousButton.setOnClickListener(v -> playerController.playPrevious());
        nextButton.setOnClickListener(v -> playerController.playNext());
        playModeButton.setOnClickListener(v -> playerController.changePlayMode());

        // 设置进度条监听器
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playbackService != null) {
                    playbackService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 更新UI
        playerController.updateSongTitle();
        playerController.updatePlayPauseButton();
        playerController.updatePlayModeButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        playerController.stopProgressUpdate();
        if (serviceBound && playbackService != null) {
            playbackService.setOnSongChangeListener(null);
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    @Override
    public void onSongChange(String title) {
        runOnUiThread(() -> {
            playerController.updateUIForNewSong();
        });
    }

    @Override
    public void onAutoPlayNext(String title) {
        runOnUiThread(() -> {
            playerController.updateUIForNewSong();
        });
    }
}