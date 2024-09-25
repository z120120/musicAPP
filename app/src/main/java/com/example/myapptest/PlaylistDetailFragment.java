package com.example.myapptest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PlaylistDetailFragment extends Fragment implements FavoriteToggleListener {

    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PLAYLIST_NAME = "playlist_name";

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