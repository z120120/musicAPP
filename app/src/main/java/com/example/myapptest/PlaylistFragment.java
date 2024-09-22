package com.example.myapptest;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment {

    private static final String TAG = "PlaylistFragment";
    private ListView playlistView;
    private List<Music> playlistSongs;
    private PlaylistAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        playlistView = view.findViewById(R.id.playlist_view);
        playlistSongs = new ArrayList<>();

        loadPlaylistFromDatabase();

        playlistView.setOnItemClickListener((parent, view1, position, id) -> {
            playMusic(position);
        });

        return view;
    }

    private void loadPlaylistFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                MusicCollection defaultCollection = musicDao.getDefaultCollection();
                if (defaultCollection != null) {
                    List<Music> songs = musicDao.getMusicInCollection(defaultCollection.id);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            playlistSongs.clear();
                            playlistSongs.addAll(songs);
                            updatePlaylistView();
                        });
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "没有找到默认播放列表", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading playlist from database", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "加载播放列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    private void playMusic(int position) {
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            activity.playMusic(playlistSongs, position);
        }
    }

    private void updatePlaylistView() {
        if (getContext() == null) return;
        
        adapter = new PlaylistAdapter(getContext(), playlistSongs, this);
        playlistView.setAdapter(adapter);

        if (playlistSongs.isEmpty()) {
            Toast.makeText(getContext(), "播放列表为空", Toast.LENGTH_SHORT).show();
        }
    }

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