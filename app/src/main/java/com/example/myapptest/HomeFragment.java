package com.example.myapptest;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
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

        return view;
    }
}