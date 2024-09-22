package com.example.myapptest;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;

public class MusicLibraryFragment extends Fragment {

    private ListView musicListView;
    private List<Music> musicList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_library, container, false);

        musicListView = view.findViewById(R.id.music_list_view);
        musicList = new ArrayList<>();

        loadMusicFromDatabase();

        return view;
    }

    private void loadMusicFromDatabase() {
        AppDatabase db = AppDatabase.getDatabase(getContext());
        MusicDao musicDao = db.musicDao();

        new Thread(() -> {
            List<Music> music = musicDao.getAllMusic();
            getActivity().runOnUiThread(() -> {
                musicList.clear();
                musicList.addAll(music);
                updateMusicList();
            });
        }).start();
    }

    private void updateMusicList() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1,
                musicList.stream().map(m -> m.title).toArray(String[]::new));
        musicListView.setAdapter(adapter);
    }
}