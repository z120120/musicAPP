package com.example.myapptest.utils;

import android.widget.TextView;

import com.example.myapptest.Music;
import com.example.myapptest.PlaybackService;

public class UIUtils {
    public static void updateSongTitle(PlaybackService playbackService, TextView songTitleView) {
        if (playbackService == null || songTitleView == null) return;
        Music currentMusic = playbackService.getCurrentMusic();
        if (currentMusic != null) {
            songTitleView.setText(currentMusic.title);
        }
    }
}