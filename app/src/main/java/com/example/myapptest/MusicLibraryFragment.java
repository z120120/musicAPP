package com.example.myapptest;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class MusicLibraryFragment extends Fragment {

    private static final String TAG = "MusicLibraryFragment";
    private ListView musicListView;
    private List<Music> musicList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_library, container, false);

        musicListView = view.findViewById(R.id.music_list_view);
        musicList = new ArrayList<>();

        loadMusicFromDatabase();

        musicListView.setOnItemClickListener((parent, view1, position, id) -> {
            addAllMusicToPlaylist();
        });

        return view;
    }

    private void loadMusicFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                List<Music> music = musicDao.getAllMusic();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        musicList.clear();
                        musicList.addAll(music);
                        updateMusicList();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading music from database", e);
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "加载音乐列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    private void updateMusicList() {
        try {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,
                    musicList.stream().map(m -> m.title).toArray(String[]::new));
            musicListView.setAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error updating music list", e);
            Toast.makeText(getContext(), "更新音乐列表失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void addAllMusicToPlaylist() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                MusicCollection defaultCollection = musicDao.getDefaultCollection();
                if (defaultCollection == null) {
                    defaultCollection = new MusicCollection("Default Collection", true);
                    long collectionId = musicDao.insertMusicCollection(defaultCollection);
                    defaultCollection.id = (int) collectionId;
                }

                List<Music> allMusic = musicDao.getAllMusic();

                // 将所有音乐添加到默认音乐集合
                for (int i = 0; i < allMusic.size(); i++) {
                    Music music = allMusic.get(i);
                    MusicCollectionSong collectionSong = new MusicCollectionSong(defaultCollection.id, music.id, i);
                    musicDao.insertMusicCollectionSong(collectionSong);
                }

                // 更新或创建播放队列
                PlayQueue playQueue = musicDao.getPlayQueue();
                if (playQueue == null) {
                    playQueue = new PlayQueue();
                    musicDao.insertPlayQueue(playQueue);
                }
                musicDao.updatePlayQueueIndex(0);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "已将所有音乐添加到播放列表", Toast.LENGTH_SHORT).show();
                        // TODO: 启动 PlaybackService 并开始播放
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding music to playlist", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "添加音乐到播放列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }
}