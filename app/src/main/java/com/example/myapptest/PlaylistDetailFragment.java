package com.example.myapptest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment implements FavoriteToggleListener {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_IMPORT_FILE = 113;

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

        adapter = new PlaylistAdapter(getContext(), playlistSongs, this);
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
        }
    }

    private void importPlaylist(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            List<String> importedSongs = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                importedSongs.add(line);
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
            List<Music> songsToAdd = new ArrayList<>();

            for (String importedSong : importedSongs) {
                for (Music music : allMusic) {
                    if (music.title.equals(importedSong)) {
                        songsToAdd.add(music);
                        break;
                    }
                }
            }

            for (Music music : songsToAdd) {
                PlaylistSong playlistSong = new PlaylistSong(playlistId, music.id);
                musicDao.insertPlaylistSong(playlistSong);
            }

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已导入 " + songsToAdd.size() + " 首歌曲", Toast.LENGTH_SHORT).show();
                loadPlaylistSongs();
            });
        }).start();
    }

    private void exportPlaylist() {
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "MyAppTest");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, playlistName + ".txt");

        try (FileWriter writer = new FileWriter(file)) {
            for (Music music : playlistSongs) {
                writer.write(music.title + "\n");
            }
            Toast.makeText(getContext(), "歌单已导出到 " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "导出失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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