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

public class FavoriteFragment extends Fragment {

    private static final String TAG = "FavoriteFragment";
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

        return view;
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
        
        adapter = new PlaylistAdapter(getContext(), favoriteSongs, null);
        favoriteListView.setAdapter(adapter);

        if (favoriteSongs.isEmpty()) {
            Toast.makeText(getContext(), "暂无喜欢的音乐", Toast.LENGTH_SHORT).show();
        }
    }

    public void toggleFavorite(int position) {
        Music music = favoriteSongs.get(position);
        music.isFavorite = !music.isFavorite;
        
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.updateMusic(music);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!music.isFavorite) {
                        favoriteSongs.remove(position);
                    }
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "已从喜爱中移除", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}