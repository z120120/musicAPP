package com.example.myapptest;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {
    private List<Playlist> playlists;
    private ArrayAdapter<String> playlistAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        Button scanMusicButton = view.findViewById(R.id.btn_scan_music);
        scanMusicButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MusicScanActivity.class);
            startActivity(intent);
        });

        Button musicLibraryButton = view.findViewById(R.id.btn_music_library);
        musicLibraryButton.setOnClickListener(v -> {
            Fragment fragment = new MusicLibraryFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 添加"我的喜欢"按钮
        Button favoriteButton = view.findViewById(R.id.btn_favorite);
        favoriteButton.setOnClickListener(v -> {
            Fragment fragment = new FavoriteFragment();
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        Button createPlaylistButton = view.findViewById(R.id.btn_create_playlist);
        createPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());

        ListView playlistListView = view.findViewById(R.id.playlist_list_view);
        playlists = new ArrayList<>();
        playlistAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, new ArrayList<>());
        playlistListView.setAdapter(playlistAdapter);

        playlistListView.setOnItemClickListener((parent, view1, position, id) -> {
            // 这里可以添加点击歌单时的操作，比如打开歌单详情页
        });

        playlistListView.setOnItemLongClickListener((parent, view1, position, id) -> {
            showPlaylistOptionsDialog(position);
            return true;
        });

        loadPlaylists();

        return view;
    }

    private void showCreatePlaylistDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("创建新歌单");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String playlistName = input.getText().toString();
            if (!playlistName.isEmpty()) {
                createPlaylist(playlistName);
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void createPlaylist(String name) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            Playlist newPlaylist = new Playlist(name);
            long playlistId = musicDao.insertPlaylist(newPlaylist);
            newPlaylist.id = (int) playlistId;
            playlists.add(newPlaylist);
            requireActivity().runOnUiThread(() -> {
                playlistAdapter.add(name);
                playlistAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void loadPlaylists() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            playlists = musicDao.getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                playlistAdapter.clear();
                for (Playlist playlist : playlists) {
                    playlistAdapter.add(playlist.name);
                }
                playlistAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void showPlaylistOptionsDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("歌单操作");
        String[] options = {"编辑名称", "删除歌单"};

        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    showEditPlaylistDialog(position);
                    break;
                case 1:
                    showDeletePlaylistConfirmation(position);
                    break;
            }
        });

        builder.show();
    }

    private void showEditPlaylistDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("编辑歌单名称");

        final EditText input = new EditText(getContext());
        input.setText(playlists.get(position).name);
        builder.setView(input);

        builder.setPositiveButton("确定", (dialog, which) -> {
            String newName = input.getText().toString();
            if (!newName.isEmpty()) {
                updatePlaylistName(position, newName);
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void updatePlaylistName(int position, String newName) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            Playlist playlist = playlists.get(position);
            playlist.name = newName;
            musicDao.updatePlaylistName(playlist.id, newName);
            requireActivity().runOnUiThread(() -> {
                playlistAdapter.remove(playlistAdapter.getItem(position));
                playlistAdapter.insert(newName, position);
                playlistAdapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void showDeletePlaylistConfirmation(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("删除歌单");
        builder.setMessage("确定要删除这个歌单吗？");

        builder.setPositiveButton("确定", (dialog, which) -> deletePlaylist(position));
        builder.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void deletePlaylist(int position) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            Playlist playlist = playlists.get(position);
            musicDao.deletePlaylist(playlist.id);
            playlists.remove(position);
            requireActivity().runOnUiThread(() -> {
                playlistAdapter.remove(playlistAdapter.getItem(position));
                playlistAdapter.notifyDataSetChanged();
            });
        }).start();
    }
}