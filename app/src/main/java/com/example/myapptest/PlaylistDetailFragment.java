package com.example.myapptest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment implements FavoriteToggleListener, PlaylistAdapter.RemoveSongListener {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_IMPORT_FILE = 113;
    private static final int REQUEST_EXPORT_FILE = 114;

    private int playlistId;
    private String playlistName;
    private ListView songListView;
    private List<Music> playlistSongs;
    private PlaylistAdapter adapter;

    public static PlaylistDetailFragment newInstance(int playlistId, String playlistName) {
        PlaylistDetailFragment fragment = new PlaylistDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PLAYLIST_ID, playlistId);
        args.putString(ARG_PLAYLIST_NAME, playlistName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            playlistId = getArguments().getInt(ARG_PLAYLIST_ID);
            playlistName = getArguments().getString(ARG_PLAYLIST_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist_detail, container, false);

        TextView playlistNameTextView = view.findViewById(R.id.playlist_name);
        playlistNameTextView.setText(playlistName);

        songListView = view.findViewById(R.id.song_list_view);
        playlistSongs = new ArrayList<>();

        loadPlaylistSongs();

        songListView.setOnItemClickListener((parent, view1, position, id) -> {
            playMusic(position);
        });

        Button exportButton = view.findViewById(R.id.btn_export_playlist);
        exportButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
            } else {
                exportPlaylist();
            }
        });

        Button importButton = view.findViewById(R.id.btn_import_playlist);
        importButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("text/plain");
            startActivityForResult(intent, REQUEST_IMPORT_FILE);
        });

        Button clearPlaylistButton = view.findViewById(R.id.btn_clear_playlist);
        clearPlaylistButton.setOnClickListener(v -> showClearPlaylistConfirmation());

        songListView.setOnItemLongClickListener((parent, view1, position, id) -> {
            onRemoveSong(position);
            return true;
        });

        return view;
    }

    private void loadPlaylistSongs() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Music> songs = musicDao.getMusicInPlaylist(playlistId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    playlistSongs.clear();
                    playlistSongs.addAll(songs);
                    updateSongListView();
                });
            }
        }).start();
    }

    private void updateSongListView() {
        if (getContext() == null) return;

        adapter = new PlaylistAdapter(getContext(), playlistSongs, this, this);
        songListView.setAdapter(adapter);

        if (playlistSongs.isEmpty()) {
            Toast.makeText(getContext(), "歌单为空", Toast.LENGTH_SHORT).show();
        }
    }

    private void playMusic(int position) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.playMusic(playlistSongs, position);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMPORT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                importPlaylist(uri);
            }
        } else if (requestCode == REQUEST_EXPORT_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                writePlaylistToFile(uri);
            }
        }
    }

    private void exportPlaylist() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, playlistName + ".txt");
        startActivityForResult(intent, REQUEST_EXPORT_FILE);
    }

    private void writePlaylistToFile(Uri uri) {
        try {
            OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                for (Music music : playlistSongs) {
                    String songName = stripFileExtension(music.title) + "\n";
                    outputStream.write(songName.getBytes());
                }
                outputStream.close();
                Toast.makeText(getContext(), "歌单已导出", Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String stripFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? filename : filename.substring(0, dotIndex);
    }

    private void importPlaylist(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<String> importedSongs = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                importedSongs.add(line.trim());
            }
            reader.close();
            inputStream.close();

            addImportedSongsToPlaylist(importedSongs);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addImportedSongsToPlaylist(List<String> importedSongs) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Music> allMusic = musicDao.getAllMusic();
            List<Music> existingSongs = musicDao.getMusicInPlaylist(playlistId);
            List<Music> songsToAdd = new ArrayList<>();
            List<String> notFoundSongs = new ArrayList<>();

            for (String importedSong : importedSongs) {
                String importedSongWithoutExtension = stripFileExtension(importedSong).trim().toLowerCase();
                boolean songFound = false;
                for (Music music : allMusic) {
                    String musicTitle = stripFileExtension(music.title).trim().toLowerCase();
                    if (musicTitle.equals(importedSongWithoutExtension)) {
                        // 检查歌曲是否已经存在于歌单中
                        boolean alreadyExists = false;
                        for (Music existingSong : existingSongs) {
                            if (existingSong.id == music.id) {
                                alreadyExists = true;
                                break;
                            }
                        }
                        if (!alreadyExists) {
                            songsToAdd.add(music);
                        }
                        songFound = true;
                        break;
                    }
                }
                if (!songFound) {
                    notFoundSongs.add(importedSong);
                }
            }

            for (Music music : songsToAdd) {
                PlaylistSong playlistSong = new PlaylistSong(playlistId, music.id);
                musicDao.insertPlaylistSong(playlistSong);
            }

            requireActivity().runOnUiThread(() -> {
                String message = "已导入 " + songsToAdd.size() + " 首歌曲";
                if (!notFoundSongs.isEmpty()) {
                    message += "，未找到 " + notFoundSongs.size() + " 首歌曲";
                    for (String notFoundSong : notFoundSongs) {
                        message += "\n- " + notFoundSong;
                    }
                }
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                loadPlaylistSongs();
            });
        }).start();
    }

    private void showClearPlaylistConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("清空歌单");
        builder.setMessage("确定要清空这个歌单吗？此操作不可撤销。");
        builder.setPositiveButton("确定", (dialog, which) -> clearPlaylist());
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void clearPlaylist() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.deletePlaylistSongs(playlistId);
            requireActivity().runOnUiThread(() -> {
                playlistSongs.clear();
                updateSongListView();
                Toast.makeText(getContext(), "歌单已清空", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void onRemoveSong(int position) {
        removeSongFromPlaylist(position);
    }

    private void removeSongFromPlaylist(int position) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            Music musicToRemove = playlistSongs.get(position);
            musicDao.deletePlaylistSong(playlistId, musicToRemove.id);
            requireActivity().runOnUiThread(() -> {
                playlistSongs.remove(position);
                updateSongListView();
                Toast.makeText(getContext(), "歌曲已从歌单中移除", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    @Override
    public void toggleFavorite(int position) {
        Music music = playlistSongs.get(position);
        music.isFavorite = !music.isFavorite;

        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.updateMusic(music);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), music.isFavorite ? "已添加到喜爱" : "已从喜爱中移除", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}