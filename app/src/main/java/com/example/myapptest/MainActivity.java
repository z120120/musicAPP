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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.util.Log;  // 添加这行

import java.util.List;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";

    private PlaybackService playbackService;
    private boolean serviceBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PlaybackService.LocalBinder binder = (PlaybackService.LocalBinder) service;
            playbackService = binder.getService();
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private View miniPlayer;
    private TextView songTitleView;
    private ImageButton playPauseButton;

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

        miniPlayer = findViewById(R.id.mini_player);
        songTitleView = findViewById(R.id.song_title);
        playPauseButton = findViewById(R.id.play_pause_button);

        playPauseButton.setOnClickListener(v -> togglePlayPause());
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
        if (serviceBound) {
            unbindService(serviceConnection);
            serviceBound = false;
        }
    }

    public void playMusic(List<Music> playlist, int position) {
        if (serviceBound && playbackService != null) {
            playbackService.setPlaylist(playlist);
            playbackService.play();
            updateMiniPlayer(playlist.get(position).title);
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
        songTitleView.setText(songTitle);
        miniPlayer.setVisibility(View.VISIBLE);
        playPauseButton.setImageResource(R.drawable.ic_pause);
    }
}