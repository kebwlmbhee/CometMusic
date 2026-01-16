package com.example.cometmusic.data;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedData {

    private final Context context;

    private final String MAIN_PREF = "CometMusic";
    private final String DENIED_KEY = "DeniedTimes";
    private final String CHOOSE_DIR_KEY = "ChooseDir";
    private final String SONG_ID_KEY = "SongId";
    private final String SONG_POSITION_KEY = "SongPosition";

    private final String PLAYER_MODE_KEY = "PlayerMode";

    public SharedData(Context context) {
        this.context = context;
    }

    public int getDeniedTimes() {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        return shp.getInt(DENIED_KEY, 0);
    }

    public void setDeniedTimes(int deniedTimes) {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(DENIED_KEY, deniedTimes);
        editor.apply();
    }

    public String getChooseDir() {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        return shp.getString(CHOOSE_DIR_KEY, null);
    }

    public void setChooseDir(String chooseDir) {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putString(CHOOSE_DIR_KEY, chooseDir);
        editor.apply();
    }

    public String getSongMediaId() {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        return shp.getString(SONG_ID_KEY, null);
    }

    public void setSongMediaId(String songId) {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putString(SONG_ID_KEY, songId);
        editor.apply();
    }

    // second
    public int getSongPosition() {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        return shp.getInt(SONG_POSITION_KEY, 0);
    }

    // second
    public void setSongPosition(int songPosition) {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(SONG_POSITION_KEY, songPosition);
        editor.apply();
    }

    public int getPlayerMode() {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        return shp.getInt(PLAYER_MODE_KEY, 1);
    }

    public void setPlayerMode(int playerMode) {
        SharedPreferences shp = context.getSharedPreferences(MAIN_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = shp.edit();
        editor.putInt(PLAYER_MODE_KEY, playerMode);
        editor.apply();
    }
}