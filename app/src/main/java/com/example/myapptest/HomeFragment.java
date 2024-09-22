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

        return view;
    }
}