package com.example.cometmusic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedData(val context: Context) {
    companion object {
        private const val PREF_NAME = "CometMusic"
        private const val DENIED_KEY = "DeniedTimes"
        private const val CHOOSE_DIR_KEY = "ChooseDir"
        private const val SONG_ID_KEY = "SongId"
        private const val SONG_POSITION_KEY = "SongPosition"
        private const val PLAYER_MODE_KEY = "PlayerMode"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var deniedTimes: Int
        get() = prefs.getInt(DENIED_KEY, 0)
        set(value) = prefs.edit { putInt(DENIED_KEY, value) }

    var chooseDir: String?
        get() = prefs.getString(CHOOSE_DIR_KEY, null)
        set(value) = prefs.edit { putString(CHOOSE_DIR_KEY, value) }

    var songMediaId: String?
        get() = prefs.getString(SONG_ID_KEY, null)
        set(value) = prefs.edit { putString(SONG_ID_KEY, value) }

    var songPosition: Int
        get() = prefs.getInt(SONG_POSITION_KEY, 0)
        set(value) = prefs.edit { putInt(SONG_POSITION_KEY, value) }

    var playerMode: Int
        get() = prefs.getInt(PLAYER_MODE_KEY, 1)
        set(value) = prefs.edit { putInt(PLAYER_MODE_KEY, value) }
}