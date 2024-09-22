package com.example.myapptest;

import android.os.Bundle;
import android.view.MenuItem;
import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;
import android.os.IBinder;
import android.content.ServiceConnection;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast; // 添加这行
import android.widget.SeekBar;  // 添加这行
import android.os.Handler;  // 添加这行
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;  // 添加这行

import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener, PlaybackService.OnSongChangeListener {

    private static final String TAG = "MainActivity";

    private PlaybackService playbackService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
            Log.d(TAG, "onServiceConnected: PlaybackService 已绑定");
            
            // 设置歌曲变化监听器
            playbackService.setOnSongChangeListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            Log.d(TAG, "onServiceDisconnected: PlaybackService 连接断开");
        }
    };

    private View miniPlayer;
    private TextView songTitleView;
    private ImageButton playPauseButton;
    private ImageButton previousButton;
    private ImageButton nextButton;
    private ImageButton playModeButton;
    private int playMode = 0; // 0: 列表循环, 1: 单曲循环, 2: 随机播放

    private SeekBar seekBar;
    private TextView currentTimeView;
    private TextView totalTimeView;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnNavigationItemSelectedListener(this);

        // 默认显示首页
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();

        // 启动并绑定 PlaybackService
        Intent intent = new Intent(this, PlaybackService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "onCreate: 尝试绑定 PlaybackService");

        miniPlayer = findViewById(R.id.mini_player);
        songTitleView = findViewById(R.id.song_title);
        playPauseButton = findViewById(R.id.play_pause_button);
        previousButton = findViewById(R.id.previous_button);
        nextButton = findViewById(R.id.next_button);
        playModeButton = findViewById(R.id.play_mode_button);
        seekBar = findViewById(R.id.seek_bar);
        currentTimeView = findViewById(R.id.current_time);
        totalTimeView = findViewById(R.id.total_time);

        playPauseButton.setOnClickListener(v -> togglePlayPause());
        previousButton.setOnClickListener(v -> playPrevious());
        nextButton.setOnClickListener(v -> playNext());
        playModeButton.setOnClickListener(v -> changePlayMode());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && serviceBound && playbackService != null) {
                    playbackService.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 启动进度更新任务
        startProgressUpdate();

        // ... 其他现有的代码 ...
    }

    private void startProgressUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (serviceBound && playbackService != null) {
                    int currentPosition = playbackService.getCurrentPosition();
                    int duration = playbackService.getDuration();
                    updateProgressBar(currentPosition, duration);
                }
                handler.postDelayed(this, 1000);
            }
        }, 1000);
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment selectedFragment = null;

        int itemId = item.getItemId();
        if (itemId == R.id.nav_home) {
            selectedFragment = new HomeFragment();
        } else if (itemId == R.id.nav_playlist) {
            selectedFragment = new PlaylistFragment();
        }

        if (selectedFragment != null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selectedFragment).commit();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    public void playMusic(List<Music> playlist, int position) {
        Log.d(TAG, "playMusic: 尝试播放音乐，播放列表大小: " + playlist.size() + ", 位置: " + position);
        if (serviceBound && playbackService != null) {
            playbackService.setPlaylist(playlist);
            playbackService.setCurrentIndex(position); // 设置当前播放索引
            playbackService.play();
            updateMiniPlayer(playlist.get(position).title);
            Log.d(TAG, "playMusic: 已调用 PlaybackService 的 play 方法");
        } else {
            Log.e(TAG, "PlaybackService not bound or null");
        }
    }

    private void togglePlayPause() {
        if (serviceBound) {
            if (playbackService.isPlaying()) {
                playbackService.pause();
                playPauseButton.setImageResource(R.drawable.ic_play);
            } else {
                playbackService.play();
                playPauseButton.setImageResource(R.drawable.ic_pause);
            }
        }
    }

    private void updateMiniPlayer(String songTitle) {
        runOnUiThread(() -> {
            songTitleView.setText(songTitle);
            miniPlayer.setVisibility(View.VISIBLE);
            playPauseButton.setImageResource(R.drawable.ic_pause);
        });
    }

    private void playPrevious() {
        if (serviceBound && playbackService != null) {
            playbackService.previous();
            updateMiniPlayer(playbackService.getCurrentSongTitle());
        }
    }

    private void playNext() {
        if (serviceBound && playbackService != null) {
            playbackService.next();
            updateMiniPlayer(playbackService.getCurrentSongTitle());
        }
    }

    private void changePlayMode() {
        playMode = (playMode + 1) % 3;
        switch (playMode) {
            case 0:
                playModeButton.setImageResource(R.drawable.ic_repeat);
                Toast.makeText(this, "列表循环", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                playModeButton.setImageResource(R.drawable.ic_repeat_one);
                Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                playModeButton.setImageResource(R.drawable.ic_shuffle);
                Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show();
                break;
        }
        if (serviceBound && playbackService != null) {
            playbackService.setPlayMode(playMode);
        }
    }

    @Override
    public void onSongChange(String title) {
        updateMiniPlayer(title);
    }
}