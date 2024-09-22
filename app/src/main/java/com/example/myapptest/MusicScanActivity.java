package com.example.myapptest;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

public class MusicScanActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSION_CODE = 1;
    private static final int REQUEST_DIRECTORY = 2;

    private Button selectFolderButton;
    private TextView scanStatusTextView;
    private TextView scannedFilesTextView;
    private List<String> musicFiles;
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_CODE);
        } else {
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
                // TODO: 将音乐文件信息保存到数据库
            });
        }).start();
    }

    private void scanDirectory(DocumentFile directory) {
        if (directory == null || !directory.exists()) return;

        for (DocumentFile file : directory.listFiles()) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (isMusicFile(file.getName())) {
                String fileName = file.getName();
                musicFiles.add(fileName);
                mainHandler.post(() -> {
                    scanStatusTextView.setText("已扫描到 " + musicFiles.size() + " 个音乐文件");
                    scannedFilesTextView.append(fileName + "\n");
                });
            }
        }
    }

    private boolean isMusicFile(String fileName) {
        return fileName != null && (fileName.endsWith(".mp3") || fileName.endsWith(".wav") || fileName.endsWith(".flac"));
    }
}