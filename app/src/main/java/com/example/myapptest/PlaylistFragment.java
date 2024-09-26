package com.example.myapptest;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.app.AlertDialog;
import android.widget.ArrayAdapter;
import android.view.MenuInflater;
import android.widget.PopupMenu;
import android.widget.ImageButton;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PlaylistFragment extends Fragment implements FavoriteToggleListener, PlaylistAdapter.RemoveSongListener, PlaybackService.OnCurrentSongChangeListener {

    private static final String TAG = "PlaylistFragment";
    private ListView playlistView;
    private List<Music> playlistSongs;
    private PlaylistAdapter adapter;
    private EditText searchBar;
    private int currentPlaylistId;
    private ImageButton locateCurrentSongButton;
    private PlaybackService playbackService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playlist, container, false);

        playlistView = view.findViewById(R.id.playlist_view);
        searchBar = view.findViewById(R.id.search_bar);
        playlistSongs = new ArrayList<>();

        loadPlaylistFromDatabase();

        playlistView.setOnItemClickListener((parent, view1, position, id) -> {
            Music selectedMusic = adapter.getItem(position);
            int originalPosition = playlistSongs.indexOf(selectedMusic);
            playMusic(originalPosition);
        });

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

        playlistView.setOnItemLongClickListener((parent, view1, position, id) -> {
            showAddToPlaylistDialog(position);
            return true;
        });

        ImageButton moreOptionsButton = view.findViewById(R.id.btn_more_options);
        moreOptionsButton.setOnClickListener(v -> showMoreOptions(v));

        locateCurrentSongButton = view.findViewById(R.id.btn_locate_current_song);
        locateCurrentSongButton.setOnClickListener(v -> locateCurrentSong());

        // 获取 PlaybackService 实例
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            playbackService = activity.getPlaybackService();
        }

        return view;
    }

    private void loadPlaylistFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                MusicCollection defaultCollection = musicDao.getDefaultCollection();
                if (defaultCollection != null) {
                    currentPlaylistId = defaultCollection.id;
                    List<Music> songs = musicDao.getMusicInCollection(currentPlaylistId);
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
        
        adapter = new PlaylistAdapter(getContext(), playlistSongs, this, this);
        playlistView.setAdapter(adapter);

        if (playlistSongs.isEmpty()) {
            Toast.makeText(getContext(), "播放列表为空", Toast.LENGTH_SHORT).show();
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

    private void showAddToPlaylistDialog(int position) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Playlist> playlists = musicDao.getAllPlaylists();
            requireActivity().runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("添加到歌单");

                ArrayAdapter<String> playlistAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1);
                for (Playlist playlist : playlists) {
                    playlistAdapter.add(playlist.name);
                }

                builder.setAdapter(playlistAdapter, (dialog, which) -> {
                    Playlist selectedPlaylist = playlists.get(which);
                    addMusicToPlaylist(position, selectedPlaylist.id);
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        }).start();
    }

    private void addMusicToPlaylist(int position, int playlistId) {
        Music selectedMusic = adapter.getItem(position);
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            PlaylistSong playlistSong = new PlaylistSong(playlistId, selectedMusic.id);
            musicDao.insertPlaylistSong(playlistSong);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已添加到歌单", Toast.LENGTH_SHORT).show();
            });
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
                    addAllMusicToPlaylist(selectedPlaylist.id);
                });

                builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
                builder.show();
            });
        }).start();
    }

    private void addAllMusicToPlaylist(int playlistId) {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            for (Music music : playlistSongs) {
                PlaylistSong playlistSong = new PlaylistSong(playlistId, music.id);
                musicDao.insertPlaylistSong(playlistSong);
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已将所有音乐添加到歌单", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void addAllToFavorite() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            for (Music music : playlistSongs) {
                if (!music.isFavorite) {
                    music.isFavorite = true;
                    musicDao.updateMusic(music);
                }
            }
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), "已将所有音乐添加到收藏", Toast.LENGTH_SHORT).show();
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    @Override
    public void onRemoveSong(int position) {
        Music music = playlistSongs.get(position);
        
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.deletePlaylistSong(currentPlaylistId, music.id);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    playlistSongs.remove(position);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "已从播放列表中移除", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showClearPlaylistConfirmation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("清空播放列表");
        builder.setMessage("确定要清空当前播放列表吗？此操作不可撤销。");
        builder.setPositiveButton("确定", (dialog, which) -> clearPlaylist());
        builder.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void clearPlaylist() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            musicDao.deletePlaylistSongs(currentPlaylistId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    playlistSongs.clear();
                    updatePlaylistView();
                    Toast.makeText(getContext(), "播放列表已清空", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showMoreOptions(View v) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.playlist_options_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_add_all_to_playlist) {
                showAddAllToPlaylistDialog();
                return true;
            } else if (itemId == R.id.menu_add_all_to_favorite) {
                addAllToFavorite();
                return true;
            } else if (itemId == R.id.menu_clear_playlist) {
                showClearPlaylistConfirmation();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void locateCurrentSong() {
        if (playbackService != null) {
            Music currentMusic = playbackService.getCurrentMusic();
            if (currentMusic != null) {
                int position = -1;
                for (int i = 0; i < playlistSongs.size(); i++) {
                    if (playlistSongs.get(i).id == currentMusic.id) {
                        position = i;
                        break;
                    }
                }
                if (position != -1) {
                    playlistView.smoothScrollToPosition(position);
                    Toast.makeText(getContext(), "已定位到当前播放歌曲", Toast.LENGTH_SHORT).show();
                } else {
                    String message = String.format("当前播放的歌曲 '%s' 不在列表中", currentMusic.title);
                    Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "locateCurrentSong: " + message);
                }
            } else {
                Toast.makeText(getContext(), "当前没有正在播放的歌曲", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "播放服务未连接", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCurrentSongChange(Music currentSong) {
        if (adapter != null) {
            int position = playlistSongs.indexOf(currentSong);
            adapter.setCurrentPlayingPosition(position);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            PlaybackService playbackService = activity.getPlaybackService();
            if (playbackService != null) {
                playbackService.setOnCurrentSongChangeListener(this);
                // 更新当前播放歌曲的高亮状态
                Music currentSong = playbackService.getCurrentMusic();
                if (currentSong != null) {
                    onCurrentSongChange(currentSong);
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity activity = (MainActivity) getActivity();
        if (activity != null) {
            PlaybackService playbackService = activity.getPlaybackService();
            if (playbackService != null) {
                playbackService.setOnCurrentSongChangeListener(null);
            }
        }
    }
}