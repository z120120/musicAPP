package com.example.myapptest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class PlaylistAdapter extends ArrayAdapter<Music> {

    private final Context context;
    private final List<Music> songs;
    private final PlaylistFragment fragment;

    public PlaylistAdapter(Context context, List<Music> songs, PlaylistFragment fragment) {
        super(context, R.layout.list_item_playlist, songs);
        this.context = context;
        this.songs = songs;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_playlist, parent, false);
            holder = new ViewHolder();
            holder.titleTextView = convertView.findViewById(R.id.song_title);
            holder.favoriteIcon = convertView.findViewById(R.id.favorite_icon);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Music music = songs.get(position);
        holder.titleTextView.setText(music.title);
        holder.favoriteIcon.setImageResource(music.isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        holder.favoriteIcon.setOnClickListener(v -> fragment.toggleFavorite(position));

        return convertView;
    }

    private static class ViewHolder {
        TextView titleTextView;
        ImageView favoriteIcon;
    }
}