package com.example.myapptest;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MusicLibraryFragment extends Fragment {

    private static final String TAG = "MusicLibraryFragment";
    private ListView musicListView;
    private List<Music> musicList;
    private BroadcastReceiver musicScanReceiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Fragment 创建");
        musicList = new ArrayList<>();
        musicScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (MusicScanActivity.ACTION_MUSIC_SCAN_COMPLETED.equals(intent.getAction())) {
                    Log.d(TAG, "onReceive: 收到音乐扫描完成广播");
                    loadMusicFromDatabase();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: 开始创建视图");
        View view = inflater.inflate(R.layout.fragment_music_library, container, false);
        musicListView = view.findViewById(R.id.music_list_view);
        if (musicListView == null) {
            Log.e(TAG, "onCreateView: musicListView 为空");
        } else {
            Log.d(TAG, "onCreateView: musicListView 成功初始化");
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: 视图创建完成");
        loadMusicFromDatabase();

        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 移除弹出音乐详情对话框
                // showMusicDetails(position);
                
                // 添加音乐到播放列表并播放
                addMusicToPlaylistAndPlay(position);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment 恢复");
        IntentFilter filter = new IntentFilter(MusicScanActivity.ACTION_MUSIC_SCAN_COMPLETED);
        requireActivity().registerReceiver(musicScanReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment 暂停");
        requireActivity().unregisterReceiver(musicScanReceiver);
    }

    private void loadMusicFromDatabase() {
        Log.d(TAG, "loadMusicFromDatabase: 开始从数据库加载音乐");
        if (!isAdded()) {
            Log.e(TAG, "loadMusicFromDatabase: Fragment 未附加到 Activity");
            return;
        }
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                List<Music> music = musicDao.getAllMusic();
                Log.d(TAG, "loadMusicFromDatabase: 从数据库加载了 " + music.size() + " 首歌曲");
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        musicList.clear();
                        musicList.addAll(music);
                        updateMusicList();
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "loadMusicFromDatabase: 加载音乐失败", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "加载音乐列表失败: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
                }
            }
        }).start();
    }

    private void updateMusicList() {
        Log.d(TAG, "updateMusicList: 开始更新音乐列表");
        if (!isAdded()) {
            Log.e(TAG, "updateMusicList: Fragment 未附加到 Activity");
            return;
        }
        if (musicListView == null) {
            Log.e(TAG, "updateMusicList: musicListView 为空");
            return;
        }
        try {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1,
                    musicList.stream().map(m -> m.title).toArray(String[]::new));
            musicListView.setAdapter(adapter);
            Log.d(TAG, "updateMusicList: 音乐列表更新成功，共 " + musicList.size() + " 首歌曲");
        } catch (Exception e) {
            Log.e(TAG, "updateMusicList: 更新音乐列表失败", e);
            Toast.makeText(requireContext(), "更新音乐列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMusicToPlaylistAndPlay(int position) {
        if (position < 0 || position >= musicList.size()) {
            Log.e(TAG, "addMusicToPlaylistAndPlay: 无效的位置");
            return;
        }

        Music selectedMusic = musicList.get(position);
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            try {
                // 获取默认的音乐集合
                MusicCollection defaultCollection = musicDao.getDefaultCollection();
                if (defaultCollection == null) {
                    defaultCollection = new MusicCollection("Default Collection", true);
                    long collectionId = musicDao.insertMusicCollection(defaultCollection);
                    defaultCollection.id = (int) collectionId;
                }

                // 创建并插入 MusicCollectionSong
                MusicCollectionSong collectionSong = new MusicCollectionSong(defaultCollection.id, selectedMusic.id, 0);
                musicDao.insertMusicCollectionSong(collectionSong);

                // 获取或创建 PlayQueue
                PlayQueue playQueue = musicDao.getPlayQueue();
                if (playQueue == null) {
                    playQueue = new PlayQueue();
                    musicDao.insertPlayQueue(playQueue);
                }
                musicDao.updatePlayQueueIndex(position); // 将 currentIndex 修改为 position

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "已将音乐添加到播放列表", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "addMusicToPlaylistAndPlay: 尝试播放音乐，位置: " + position);
                        ((MainActivity) getActivity()).playMusic(musicList, position);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "addMusicToPlaylistAndPlay: 添加音乐到播放列表失败", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "添加音乐到播放列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }

    private void addAllMusicToPlaylist() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
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

                for (int i = 0; i < allMusic.size(); i++) {
                    Music music = allMusic.get(i);
                    MusicCollectionSong collectionSong = new MusicCollectionSong(defaultCollection.id, music.id, i);
                    musicDao.insertMusicCollectionSong(collectionSong);
                }

                PlayQueue playQueue = musicDao.getPlayQueue();
                if (playQueue == null) {
                    playQueue = new PlayQueue();
                    musicDao.insertPlayQueue(playQueue);
                }
                musicDao.updatePlayQueueIndex(0);

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "已将所有音乐添加到播放列表", Toast.LENGTH_SHORT).show();
                        // TODO: 启动 PlaybackService 并开始播放
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error adding music to playlist", e);
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(requireContext(), "添加音乐到播放列表失败: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }).start();
    }
}