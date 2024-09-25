package com.example.myapptest;

import android.Manifest; // 添加这行
import android.app.Activity; // 添加这行
import android.content.Intent; // 添加这行
import android.content.pm.PackageManager; // 添加这行
import android.net.Uri; // 添加这行
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton; // 添加这行
import android.widget.ListView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream; // 添加这行
import java.util.ArrayList;
import java.util.List;

public class FavoriteFragment extends Fragment implements FavoriteToggleListener, PlaylistAdapter.RemoveSongListener { // 实现接口

    private static final String TAG = "FavoriteFragment";
    private static final int REQUEST_CREATE_FILE = 2; // 添加这行
    private static final int REQUEST_IMPORT_FILE = 3; // 新增请求码
    private ListView favoriteListView;
    private List<Music> favoriteSongs;
    private PlaylistAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);

        favoriteListView = view.findViewById(R.id.favorite_list_view);
        favoriteSongs = new ArrayList<>();

        loadFavoriteSongsFromDatabase();

        favoriteListView.setOnItemClickListener((parent, view1, position, id) -> {
            playMusic(position);
        });

        Button exportButton = view.findViewById(R.id.btn_export_favorites);
        exportButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            } else {
                exportFavoritesToTxt();
            }
        });

        Button importButton = view.findViewById(R.id.btn_import_favorites); // 初始化导入按钮
        importButton.setOnClickListener(v -> {
            importFavoritesFromTxt();
        });

        ImageButton refreshButton = view.findViewById(R.id.btn_refresh_favorites); // 初始化刷新按钮
        refreshButton.setOnClickListener(v -> {
            loadFavoriteSongsFromDatabase(); // 手动刷新喜欢的音乐列表
        });

        Button addAllToPlaylistButton = view.findViewById(R.id.btn_add_all_to_playlist);
        addAllToPlaylistButton.setOnClickListener(v -> showAddAllToPlaylistDialog());

        Button clearFavoritesButton = view.findViewById(R.id.btn_clear_favorites);
        clearFavoritesButton.setOnClickListener(v -> showClearFavoritesConfirmation());

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportFavoritesToTxt();
            } else {
                Toast.makeText(getContext(), "需要写入权限来导出文件", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadFavoriteSongsFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                List<Music> songs = musicDao.getFavoriteSongs();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        favoriteSongs.clear();
                        favoriteSongs.addAll(songs);
                        updateFavoriteListView();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading favorite songs from database", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "加载喜欢的音乐失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    private void playMusic(int position) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.playMusic(favoriteSongs, position);
        }
    }

    private void updateFavoriteListView() {
        if (getContext() == null) return;
        
        adapter = new PlaylistAdapter(getContext(), favoriteSongs, this, this);
        favoriteListView.setAdapter(adapter);

        if (favoriteSongs.isEmpty()) {
            Toast.makeText(getContext(), "喜欢列表为空", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void toggleFavorite(int position) { // 实现接口的方法
        Music music = favoriteSongs.get(position);
        music.isFavorite = !music.isFavorite;
        
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.updateMusic(music);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.notifyDataSetChanged(); // 只刷新列表，不移除音乐
                    Toast.makeText(getContext(), "已从喜爱中移除", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void exportFavoritesToTxt() {
        if (favoriteSongs.isEmpty()) {
            Toast.makeText(getContext(), "没有喜欢的音乐可以导出", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "favorite_songs.txt");
        startActivityForResult(intent, REQUEST_CREATE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CREATE_FILE && resultCode == Activity.RESULT_OK) { // 修改这一行
            Uri uri = data.getData();
            if (uri != null) {
                writeFavoritesToFile(uri);
            }
        } else if (requestCode == REQUEST_IMPORT_FILE && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            if (uri != null) {
                readFavoritesFromFile(uri);
            }
        }
    }

    private void writeFavoritesToFile(Uri uri) {
        StringBuilder data = new StringBuilder();
        for (Music music : favoriteSongs) {
            data.append(stripFileExtension(music.title)).append("\n"); // 使用 stripFileExtension 去除后缀
        }

        try {
            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(data.toString().getBytes());
                outputStream.close();
                Toast.makeText(getContext(), "导出成功", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "导出喜欢的音乐失败", e);
            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void readFavoritesFromFile(Uri uri) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(requireContext().getContentResolver().openInputStream(uri)))) {
                String line;
                List<Music> importedFavorites = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    String songName = line.trim();
                    if (!songName.isEmpty()) {
                        Music music = findMusicByName(songName);
                        if (music != null && !music.isFavorite) {
                            music.isFavorite = true;
                            importedFavorites.add(music);
                        }
                    }
                }

                if (!importedFavorites.isEmpty()) {
                    AppDatabase db = AppDatabase.getDatabase(getContext());
                    MusicDao musicDao = db.musicDao();
                    musicDao.updateMusicList(importedFavorites); // 批量更新
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "导入成功，共导入 " + importedFavorites.size() + " 首歌曲", Toast.LENGTH_SHORT).show();
                        loadFavoriteSongsFromDatabase();
                    });
                } else {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "没有匹配的歌曲被导入", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (IOException e) {
                Log.e(TAG, "导入喜欢的音乐失败", e);
                getActivity().runOnUiThread(() -> 
                    Toast.makeText(getContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private Music findMusicByName(String name) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();
        List<Music> allMusic = musicDao.getAllMusic();
        for (Music music : allMusic) {
            String strippedTitle = stripFileExtension(music.title);
            Log.d(TAG, "Comparing imported name: " + name + " with database title: " + strippedTitle);
            if (strippedTitle.equalsIgnoreCase(name) || music.title.equalsIgnoreCase(name)) { // 支持有无后缀的匹配
                return music;
            }
        }
        return null;
    }

    private String stripFileExtension(String filename) { // 完整定义 stripFileExtension 方法
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    private void importFavoritesFromTxt() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        startActivityForResult(intent, REQUEST_IMPORT_FILE);
    }

    @Override
    public void onRemoveSong(int position) {
        // 实现移除歌曲的逻辑
        Music music = favoriteSongs.get(position);
        music.isFavorite = false;
        
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.updateMusic(music);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    favoriteSongs.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "已从喜爱中移除", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showAddAllToPlaylistDialog() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Playlist> playlists = musicDao.getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("选择要添加到的歌单");

                ArrayAdapter<String> playlistAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
                for (Playlist playlist : playlists) {
                    playlistAdapter.add(playlist.name);
                }

                builder.setAdapter(playlistAdapter, (dialog, which) -> {
                    Playlist selectedPlaylist = playlists.get(which);
                    addAllFavoritesToPlaylist(selectedPlaylist.id);
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        }).start();
    }

    private void addAllFavoritesToPlaylist(int playlistId) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<PlaylistSong> playlistSongs = new ArrayList<>();
            for (Music music : favoriteSongs) {
                PlaylistSong playlistSong = new PlaylistSong(playlistId, music.id);
                playlistSongs.add(playlistSong);
            }
            musicDao.insertPlaylistSongs(playlistSongs); // 假设 MusicDao 中有批量插入的方法

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已将所有喜欢的音乐添加到歌单", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void showClearFavoritesConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("清空喜欢列表");
        builder.setMessage("确定要清空所有喜欢的音乐吗？此操作不可撤销。");
        builder.setPositiveButton("确定", (dialog, which) -> clearFavorites());
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void clearFavorites() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Music> allMusic = musicDao.getAllMusic();
            for (Music music : allMusic) {
                if (music.isFavorite) {
                    music.isFavorite = false;
                    musicDao.updateMusic(music);
                }
            }
            requireActivity().runOnUiThread(() -> {
                favoriteSongs.clear();
                updateFavoriteListView();
                Toast.makeText(getContext(), "已清空喜欢列表", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}