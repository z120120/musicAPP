package com.example.myapptest;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.EditText; // Import EditText for search bar
import android.text.Editable;
import android.text.TextWatcher;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment implements FavoriteToggleListener { // 实现接口

    private static final String TAG = "PlaylistFragment";
    private ListView playlistView;
    private List<Music> playlistSongs;
    private PlaylistAdapter adapter;
    private EditText searchBar; // 添加搜索框

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        playlistView = view.findViewById(R.id.playlist_view);
        searchBar = view.findViewById(R.id.search_bar); // 初始化搜索框
        playlistSongs = new ArrayList<>();

        loadPlaylistFromDatabase();

        playlistView.setOnItemClickListener((parent, view1, position, id) -> {
            Music selectedMusic = adapter.getItem(position);
            int originalPosition = playlistSongs.indexOf(selectedMusic);
            playMusic(originalPosition);
        });

        // 添加搜索框的监听器
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {}
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

    @Override
    public void toggleFavorite(int position) { // 实现接口的方法
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