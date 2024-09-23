package com.example.myapptest;

/**
 * 定义一个接口，用于处理喜欢状态的切换
 */
public interface FavoriteToggleListener {
    /**
     * 切换喜欢状态的方法
     *
     * @param position 歌曲在列表中的位置
     */
    void toggleFavorite(int position);
}