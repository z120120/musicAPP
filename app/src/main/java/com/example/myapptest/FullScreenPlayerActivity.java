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
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.graphics.drawable.GradientDrawable;
import java.util.Random;
import java.util.List;
import android.widget.ArrayAdapter;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;

import com.example.myapptest.model.Playlist;
import com.example.myapptest.model.PlaylistSong;

import com.example.myapptest.Music; // 如果 Music 类在主包中
// 或者
// import com.example.myapptest.model.Music; // 如果 Music 类在 model 包中

import com.example.myapptest.database.AppDatabase; // 确保这个导入语句存在
import com.example.myapptest.database.MusicDao; // 确保这个导入语句存在

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
    private ImageView albumArtView;
    private ImageButton fullScreenFavoriteButton;
    private ImageButton fullScreenAddToPlaylistButton;

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
            updateAlbumArt(); // 在服务连接后立即更新专辑图片
            
            // 设置歌曲变化监听器
            playbackService.setOnSongChangeListener(FullScreenPlayerActivity.this);
            
            // 初始化时更新喜欢按钮状态
            updateFavoriteButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private Handler handler = new Handler();
    private Runnable progressUpdateRunnable;

    private long lastClickTime = 0;
    private static final long CLICK_TIME_INTERVAL = 500; // 增加到 500 毫秒

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_player);

        // 设置随机渐变背景
        setRandomGradientBackground();

        try {
            // 初始化视图
            initializeViews();

            // 初始化 PlayerController (但不要设置 PlaybackService)
            playerController = new PlayerController(this, null, songTitleView, 
                                                    playPauseButton, playModeButton, 
                                                    seekBar, currentTimeView, totalTimeView,
                                                    artistView, albumView, albumArtView);

            // 绑定服务
            Intent intent = new Intent(this, PlaybackService.class);
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

            // 启动进度更新任务
            playerController.startProgressUpdate();

            // 初始化时更新播放/暂停按钮状态
            playerController.updatePlayPauseButton();

            // 初始化时更新专辑图片
            updateAlbumArt();

            fullScreenAddToPlaylistButton = findViewById(R.id.full_screen_add_to_playlist_button);
            fullScreenAddToPlaylistButton.setOnClickListener(v -> showAddToPlaylistDialog());

        } catch (Exception e) {
            Log.e("FullScreenPlayerActivity", "初始化全屏播放器时出错", e);
            Toast.makeText(this, "初始化全屏播放器失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish(); // 关闭活动
        }
    }

    private void setRandomGradientBackground() {
        Random random = new Random();
        int color1 = generateRandomColor(random);
        int color2 = generateRandomColor(random);

        GradientDrawable gradientDrawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{color1, color2}
        );

        findViewById(android.R.id.content).setBackground(gradientDrawable);
    }

    private int generateRandomColor(Random random) {
        return android.graphics.Color.argb(255,
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256));
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
        albumArtView = findViewById(R.id.full_screen_album_art);
        fullScreenFavoriteButton = findViewById(R.id.full_screen_favorite_button);

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
        previousButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < CLICK_TIME_INTERVAL) {
                return;
            }
            lastClickTime = System.currentTimeMillis();
            Log.d("FullScreenPlayerActivity", "点击上一首按钮");
            playerController.playPrevious();
        });
        nextButton.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < CLICK_TIME_INTERVAL) {
                return;
            }
            lastClickTime = System.currentTimeMillis();
            Log.d("FullScreenPlayerActivity", "点击下一首按钮");
            playerController.playNext();
        });
        playModeButton.setOnClickListener(v -> playerController.changePlayMode());

        fullScreenFavoriteButton.setOnClickListener(v -> toggleFavorite());

        // 设置进度条监听器
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && playbackService != null) {
                    playbackService.seekTo(progress);
                    Log.d("FullScreenPlayerActivity", "User seeked to: " + progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 可以选择暂停进度更新
                playerController.stopProgressUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 恢复进度更新
                playerController.startProgressUpdate();
            }
        });

        // 更新UI
        playerController.updateSongTitle();
        playerController.updatePlayPauseButton();
        playerController.updatePlayModeButton();
        playerController.startProgressUpdate();
    }

    private void updateAlbumArt() {
        if (playbackService != null) {
            Bitmap albumArt = playbackService.getAlbumArt();
            if (albumArt != null) {
                albumArtView.setImageBitmap(albumArt);
            } else {
                albumArtView.setImageResource(R.drawable.default_album_art);
            }
        }
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
            Log.d("FullScreenPlayerActivity", "歌曲变化，更新UI: " + title);
            playerController.updateUIForNewSong();
            updateAlbumArt();
            updateFavoriteButton(); // 添加这行
        });
    }

    @Override
    public void onAutoPlayNext(String title) {
        runOnUiThread(() -> {
            Log.d("FullScreenPlayerActivity", "自动播放下一首，更新UI: " + title);
            playerController.updateUIForNewSong();
            updateAlbumArt();
            updateFavoriteButton(); // 添加这行
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerController != null) {
            playerController.startProgressUpdate();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerController != null) {
            playerController.stopProgressUpdate();
        }
    }

    private void toggleFavorite() {
        if (playbackService != null) {
            playbackService.toggleFavorite();
        }
    }

    private void updateFavoriteButton() {
        if (playbackService != null) {
            Music currentMusic = playbackService.getCurrentMusic();
            if (currentMusic != null) {
                fullScreenFavoriteButton.setImageResource(currentMusic.isFavorite ? 
                    R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            }
        }
    }

    private void updateUIForNewSong() {
        // ... 现有代码 ...
        updateFavoriteButton();
    }

    @Override
    public void onFavoriteStatusChanged(boolean isFavorite) {
        runOnUiThread(this::updateFavoriteButton);
    }

    private void showAddToPlaylistDialog() {
        if (playbackService == null) return;

        Music currentMusic = playbackService.getCurrentMusic();
        if (currentMusic == null) return;

        AppDatabase db = AppDatabase.getDatabase(this);
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Playlist> playlists = musicDao.getAllPlaylists();
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("添加到歌单");

                ArrayAdapter<String> playlistAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
                for (Playlist playlist : playlists) {
                    playlistAdapter.add(playlist.name);
                }

                builder.setAdapter(playlistAdapter, (dialog, which) -> {
                    Playlist selectedPlaylist = playlists.get(which);
                    addMusicToPlaylist(currentMusic, selectedPlaylist.id);
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        }).start();
    }

    private void addMusicToPlaylist(Music music, int playlistId) {
        AppDatabase db = AppDatabase.getDatabase(this);
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            PlaylistSong playlistSong = new PlaylistSong(playlistId, music.id);
            musicDao.insertPlaylistSong(playlistSong);
            runOnUiThread(() -> {
                Toast.makeText(this, "已添加到歌单", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}