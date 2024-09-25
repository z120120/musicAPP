package com.example.myapptest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log; // 添加这行
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;

import java.util.ArrayList;
import java.util.List;

import com.example.myapptest.utils.PermissionUtils;

public class MusicScanActivity extends AppCompatActivity {

    private static final String TAG = "MusicScanActivity";

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_DIRECTORY = 2;
    public static final String ACTION_MUSIC_SCAN_COMPLETED = "com.example.myapptest.MUSIC_SCAN_COMPLETED";

    private Button selectFolderButton;
    private TextView scanStatusTextView;
    private TextView scannedFilesTextView;
    private List<Music> musicFiles;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_scan);

        selectFolderButton = findViewById(R.id.select_folder_button);
        scanStatusTextView = findViewById(R.id.scan_status_text_view);
        scannedFilesTextView = findViewById(R.id.scanned_files_text_view);

        selectFolderButton.setOnClickListener(v -> checkPermissionAndSelectFolder());
        mainHandler = new Handler(Looper.getMainLooper());
        musicFiles = new ArrayList<>();
    }

    private void checkPermissionAndSelectFolder() {
        if (PermissionUtils.checkAndRequestPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE, REQUEST_PERMISSION_CODE)) {
            selectFolder();
        }
    }

    private void selectFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_DIRECTORY);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFolder();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_DIRECTORY && resultCode == RESULT_OK) {
            Uri treeUri = data.getData();
            // 申请持久URI权限
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            scanMusicFiles(treeUri);
        }
    }

    private void scanMusicFiles(Uri treeUri) {
        // 清空之前的扫描结果
        musicFiles.clear();
        mainHandler.post(() -> {
            scanStatusTextView.setText("开始扫描...");
            scannedFilesTextView.setText(""); // 清空文件列表显示
        });

        new Thread(() -> {
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            scanDirectory(pickedDir);
            mainHandler.post(() -> {
                scanStatusTextView.setText("扫描完成，共找到 " + musicFiles.size() + " 个音乐文件");
                saveMusicToDatabase();
            });
        }).start();
    }

    private void saveMusicToDatabase() {
        AppDatabase db = AppDatabase.getDatabase(this);
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                musicDao.deleteAll(); // 清空之前的数据
                for (Music music : musicFiles) {
                    musicDao.insert(music);
                }
                runOnUiThread(() -> {
                    Toast.makeText(this, "音乐信息已保存到数据库", Toast.LENGTH_SHORT).show();
                    // 发送广播通知扫描完成
                    Intent intent = new Intent(ACTION_MUSIC_SCAN_COMPLETED);
                    sendBroadcast(intent);
                });
            } catch (Exception e) {
                Log.e(TAG, "saveMusicToDatabase: 保存音乐信息失败", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "保存音乐信息失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void scanDirectory(DocumentFile directory) {
        if (directory == null || !directory.exists()) return;

        for (DocumentFile file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (isMusicFile(file.getName())) {
                String fileName = file.getName();
                String filePath = file.getUri().toString(); // 获取文件的 URI
                String artist = "未知艺术家"; // 默认值
                String album = "未知专辑"; // 默认值

                // 尝试获取艺术家和专辑信息
                try {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(this, file.getUri());
                    String retrievedArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                    String retrievedAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                    
                    if (retrievedArtist != null && !retrievedArtist.isEmpty()) {
                        artist = retrievedArtist;
                    }
                    if (retrievedAlbum != null && !retrievedAlbum.isEmpty()) {
                        album = retrievedAlbum;
                    }
                    
                    retriever.release();
                } catch (Exception e) {
                    Log.e(TAG, "Error retrieving metadata for " + fileName, e);
                }

                Log.d(TAG, "scanDirectory: 发现音乐文件 " + fileName + ", 路径: " + filePath + ", 艺术家: " + artist + ", 专辑: " + album);
                musicFiles.add(new Music(fileName, filePath, artist, album));
                mainHandler.post(() -> {
                    scanStatusTextView.setText("已扫描到 " + musicFiles.size() + " 个音乐文件");
                    scannedFilesTextView.append(fileName + "\n");
                });
            }
        }
    }

    private boolean isMusicFile(String fileName) {
        return fileName != null && (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || 
               fileName.endsWith(".ogg") || fileName.endsWith(".m4a") || fileName.endsWith(".aac"));
    }
}