package com.example.myapptest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PlaylistAdapter extends ArrayAdapter<Music> {

    private final Context context;
    private final List<Music> songs;
    private final List<Music> filteredSongs; // 添加过滤后的歌曲列表
    private final FavoriteToggleListener listener; // 使用接口

    public PlaylistAdapter(Context context, List<Music> songs, FavoriteToggleListener listener) { // 修改构造函数
        super(context, R.layout.list_item_playlist, songs);
        this.context = context;
        this.songs = songs;
        this.filteredSongs = new ArrayList<>(songs); // 初始化过滤后的歌曲列表
        this.listener = listener; // 赋值接口
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

        Music music = filteredSongs.get(position);
        holder.titleTextView.setText(stripFileExtension(music.title)); // 修改这一行，去除后缀

        holder.favoriteIcon.setImageResource(music.isFavorite ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);

        holder.favoriteIcon.setOnClickListener(v -> {
            int originalPosition = songs.indexOf(music);
            if (listener != null) {
                listener.toggleFavorite(originalPosition);
            }
            notifyDataSetChanged(); // 确保在点击喜欢图标后刷新列表
        });

        return convertView;
    }

    private String stripFileExtension(String filename) { // 确保此方法存在
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(0, dotIndex);
        }
        return filename;
    }

    static class ViewHolder {
        TextView titleTextView;
        ImageView favoriteIcon;
    }

    @Override
    public int getCount() {
        return filteredSongs.size();
    }

    @Nullable
    @Override
    public Music getItem(int position) {
        return filteredSongs.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<Music> filteredList = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    filteredList.addAll(songs);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (Music music : songs) {
                        if (stripFileExtension(music.title).toLowerCase().contains(filterPattern)) {
                            filteredList.add(music);
                        }
                    }
                }

                results.values = filteredList;
                results.count = filteredList.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                filteredSongs.clear();
                filteredSongs.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }
}