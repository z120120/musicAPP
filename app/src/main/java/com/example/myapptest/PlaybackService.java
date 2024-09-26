package com.example.myapptest;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import androidx.annotation.Nullable;
import android.content.ContentResolver;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import android.os.Handler;
import android.os.Looper;

import com.example.myapptest.utils.FileUtils;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener {

    private static final String TAG = "PlaybackService";

    private final IBinder binder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private List<Music> playlist;
    private int currentIndex = 0;
    private int playMode = 0; // 0: 列表循环, 1: 单曲循环, 2: 随机播放
    private Random random = new Random();
    private int lastPosition = 0; // 添加这个字段来保存暂停时的位置
    private boolean isChangingSong = false;
    private boolean isPrepared = false;
    private OnCurrentSongChangeListener currentSongChangeListener;

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
            isPrepared = false;
            return false;
        });
        mediaPlayer.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.start();
            Log.d(TAG, "MediaPlayer prepared and started");
            if (songChangeListener != null) {
                songChangeListener.onSongChange(getCurrentSongTitle());
            }
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

    public synchronized void next() {
        if (isChangingSong) return;
        isChangingSong = true;
        Log.d(TAG, "开始切换到下一首歌曲");
        if (playlist != null && !playlist.isEmpty()) {
            if (playMode == 2) { // 随机播放
                currentIndex = random.nextInt(playlist.size());
            } else {
                currentIndex = (currentIndex + 1) % playlist.size();
            }
            playMusic(currentIndex);
        }
        isChangingSong = false;
    }

    public synchronized void previous() {
        if (isChangingSong) return;
        isChangingSong = true;
        Log.d(TAG, "开始切换到上一首歌曲");
        if (playlist != null && !playlist.isEmpty()) {
            if (playMode == 2) { // 随机播放
                currentIndex = random.nextInt(playlist.size());
            } else {
                currentIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
            }
            playMusic(currentIndex);
        }
        isChangingSong = false;
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
        void onFavoriteStatusChanged(boolean isFavorite); // 添加这个方法
    }

    private OnSongChangeListener songChangeListener;

    public void setOnSongChangeListener(OnSongChangeListener listener) {
        this.songChangeListener = listener;
    }

    private void notifySongChange(String title) {
        if (songChangeListener != null) {
            songChangeListener.onSongChange(FileUtils.stripFileExtension(title));
        }
    }

    private void playMusic(int index) {
        Log.d(TAG, "playMusic: 尝试播放音乐，索引: " + index);
        if (playlist != null && index >= 0 && index < playlist.size()) {
            Music music = playlist.get(index);
            Log.d(TAG, "playMusic: 准备播放 " + music.title + ", 文件路径: " + music.filePath);
            try {
                mediaPlayer.reset();
                isPrepared = false;
                Uri uri = Uri.parse(music.filePath);
                mediaPlayer.setDataSource(getApplicationContext(), uri);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    isPrepared = true;
                    mp.start();
                    Log.d(TAG, "playMusic: 音乐开始播放，duration: " + mp.getDuration());
                    notifySongChange(music.title); // 通知歌曲变化
                    notifyCurrentSongChange(music);
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
        if (isPrepared && mediaPlayer != null) {
            try {
                int position = mediaPlayer.getCurrentPosition();
                Log.d(TAG, "getCurrentPosition: " + position);
                return position;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error getting current position: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "getCurrentPosition: MediaPlayer not prepared or null");
        }
        return 0;
    }

    public int getDuration() {
        if (isPrepared && mediaPlayer != null) {
            try {
                int duration = mediaPlayer.getDuration();
                Log.d(TAG, "getDuration: " + duration);
                return duration;
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error getting duration: " + e.getMessage());
            }
        } else {
            Log.d(TAG, "getDuration: MediaPlayer not prepared or null");
        }
        return 0;
    }

    public void seekTo(int position) {
        if (isPrepared && mediaPlayer != null) {
            mediaPlayer.seekTo(position);
        }
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

    // 添加获取专辑图片的方法
    public Bitmap getAlbumArt() {
        if (playlist != null && currentIndex >= 0 && currentIndex < playlist.size()) {
            Music currentMusic = playlist.get(currentIndex);
            Log.d(TAG, "尝试获取专辑图片: " + currentMusic.filePath);
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            try {
                Uri uri = Uri.parse(currentMusic.filePath);
                if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                    // 处理 content URI
                    retriever.setDataSource(getApplicationContext(), uri);
                } else {
                    // 处理普通文件路径
                    retriever.setDataSource(currentMusic.filePath);
                }
                byte[] art = retriever.getEmbeddedPicture();
                if (art != null) {
                    Log.d(TAG, "成功获取到嵌入的专辑图片，大小: " + art.length + " bytes");
                    Bitmap bitmap = BitmapFactory.decodeByteArray(art, 0, art.length);
                    if (bitmap != null) {
                        Log.d(TAG, "成功解码专辑图片，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                        return bitmap;
                    } else {
                        Log.e(TAG, "无法解码专辑图片");
                    }
                } else {
                    Log.d(TAG, "没有找到嵌入的专辑图片，尝试获取外部专辑图片");
                    String imagePath = getAlbumArtFromFolder(currentMusic.filePath);
                    if (imagePath != null) {
                        Log.d(TAG, "找到外部专辑图片: " + imagePath);
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                        if (bitmap != null) {
                            Log.d(TAG, "成功解码外部专辑图片，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
                            return bitmap;
                        } else {
                            Log.e(TAG, "无法解码外部专辑图片");
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取专辑图片失败", e);
            } finally {
                try {
                    retriever.release();
                } catch (Exception e) {
                    Log.e(TAG, "释放 MediaMetadataRetriever 失败", e);
                }
            }
        }
        Log.d(TAG, "无法获取专辑图片，返回null");
        return null;
    }

    private String getAlbumArtFromFolder(String musicFilePath) {
        Uri uri = Uri.parse(musicFilePath);
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            // 处理 content URI
            DocumentFile documentFile = DocumentFile.fromSingleUri(getApplicationContext(), uri);
            if (documentFile != null && documentFile.getParentFile() != null) {
                DocumentFile parentFolder = documentFile.getParentFile();
                String fileName = documentFile.getName();
                if (fileName != null) {
                    String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                    String[] extensions = {".jpg", ".png", ".jpeg", ".webp"};
                    for (String ext : extensions) {
                        DocumentFile imageFile = parentFolder.findFile(fileNameWithoutExtension + ext);
                        if (imageFile != null && imageFile.exists()) {
                            Log.d(TAG, "找到外部专辑图片: " + imageFile.getUri());
                            return imageFile.getUri().toString();
                        }
                    }
                }
            }
        } else {
            // 处理普通文件路径
            File musicFile = new File(musicFilePath);
            File folder = musicFile.getParentFile();
            if (folder != null && folder.isDirectory()) {
                String[] extensions = {".jpg", ".png", ".jpeg", ".webp"};
                String fileName = musicFile.getName();
                String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));
                for (String ext : extensions) {
                    File imageFile = new File(folder, fileNameWithoutExtension + ext);
                    if (imageFile.exists()) {
                        Log.d(TAG, "找到外部专辑图片: " + imageFile.getAbsolutePath());
                        return imageFile.getAbsolutePath();
                    }
                }
            }
        }
        return null;
    }

    public interface OnCurrentSongChangeListener {
        void onCurrentSongChange(Music currentSong);
    }

    public void setOnCurrentSongChangeListener(OnCurrentSongChangeListener listener) {
        this.currentSongChangeListener = listener;
    }

    private void notifyCurrentSongChange(Music currentSong) {
        if (currentSongChangeListener != null) {
            currentSongChangeListener.onCurrentSongChange(currentSong);
        }
    }

    public void toggleFavorite() {
        Music currentMusic = getCurrentMusic();
        if (currentMusic != null) {
            currentMusic.isFavorite = !currentMusic.isFavorite;
            // 更新数据库
            AppDatabase db = AppDatabase.getDatabase(this);
            MusicDao musicDao = db.musicDao();
            new Thread(() -> {
                musicDao.updateMusic(currentMusic);
                // 通知监听器
                if (songChangeListener != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        songChangeListener.onFavoriteStatusChanged(currentMusic.isFavorite)
                    );
                }
            }).start();
        }
    }
}