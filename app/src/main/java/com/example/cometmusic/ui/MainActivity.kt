package com.example.cometmusic.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.media3.common.util.UnstableApi
import com.example.cometmusic.ui.theme.CometMusicTheme

@UnstableApi
class MainActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CLOSE_MAIN_ACTIVITY_ACTION = "close_main_activity_action"
        const val REQUEST_CHANGE_PLAYER_MODE_ACTION = "change_player_mode_action"
        const val CHANGE_PLAYER_MODE_KEY = "change_player_mode"
    }

    private val playerViewModel: PlayerViewModel by viewModels()

    private val closeActivityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == REQUEST_CLOSE_MAIN_ACTIVITY_ACTION) finish()
        }
    }

    private val changePlayerModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == REQUEST_CHANGE_PLAYER_MODE_ACTION) {
                val mode = intent.getIntExtra(CHANGE_PLAYER_MODE_KEY, 1)
                playerViewModel.setPlayerMode(mode)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            CometMusicTheme {
                AppNavHost(playerViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        playerViewModel.triggerScrollToCurrentSong()
        ContextCompat.registerReceiver(
            this,
            closeActivityReceiver,
            IntentFilter(REQUEST_CLOSE_MAIN_ACTIVITY_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            changePlayerModeReceiver,
            IntentFilter(REQUEST_CHANGE_PLAYER_MODE_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(closeActivityReceiver)
        unregisterReceiver(changePlayerModeReceiver)
    }
}
