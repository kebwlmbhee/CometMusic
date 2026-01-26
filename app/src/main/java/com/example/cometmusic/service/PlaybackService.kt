package com.example.cometmusic.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.example.cometmusic.R
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.model.Song
import com.example.cometmusic.repository.FetchAudioFiles.fetchSongs
import com.example.cometmusic.repository.FetchAudioFiles.savedMediaItemIndex
import com.example.cometmusic.repository.FetchAudioFiles.songs
import com.example.cometmusic.ui.MainActivity
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

@UnstableApi
class PlaybackService : MediaSessionService() {
    companion object {
        private val TAG: String = PlaybackService::class.java.simpleName

        private const val CUSTOM_COMMAND_REPEAT_ALL = "repeatAll"
        private const val CUSTOM_COMMAND_REPEAT_ONE = "repeatOne"
        private const val CUSTOM_COMMAND_SHUFFLE = "randomShuffle"
        private const val CUSTOM_COMMAND_CLOSE_NOTIFICATION = "closeNotification"
    }
    private lateinit var mediaSession: MediaSession
    private var commandButtons = ImmutableList.Builder<CommandButton?>()
    private lateinit var sharedData: SharedData
    // player
    private lateinit var player: ExoPlayer

    private var servicePlayerMode = 0

    override fun startForegroundService(service: Intent?): ComponentName? {
        return super.startForegroundService(service)
    }

    // ensure that startForeground is always called by system,
    // regardless of whether the player is playing or not
    // https://github.com/androidx/media/issues/167#issuecomment-1668542213
    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        super.onUpdateNotification(session, true)
    }

    // Create your player and media library session in the onCreate lifecycle event
    override fun onCreate() {
        super.onCreate()

        sharedData = SharedData(this)

        servicePlayerMode = sharedData.playerMode

        // assign variables
        player = ExoPlayer.Builder(this) // auto handle audio focus
            .setAudioAttributes(
                AudioAttributes.DEFAULT,
                true
            ) // auto pause when user disconnect bluetooth or earphone
            // auto lower voice when phone call or alarm...
            .setHandleAudioBecomingNoisy(true) // set extension renderer
            // player can adapt multiply extension renderer, if they are exist...
            .setRenderersFactory(
                DefaultRenderersFactory(this).setExtensionRendererMode(
                    DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                )
            ).build()

        player.apply {
            prepare()
            addPlayerListener()
        }

        player.let {
            // Create a MediaSession
            mediaSession = MediaSession.Builder(this, it).run {
                setCallback(SessionCallback())
                openMainActivityPendingIntent(1)?.let { sessionIntent ->
                    setSessionActivity(sessionIntent)
                } ?: Log.w(TAG, "onCreate: Failed, sessionIntent is null")
                build()
            }
        }

        updateCommandButtons()

        updateMediaSessionCustomLayout()

        setMediaNotificationProvider(CustomNotificationProvider(this))
    }

    // manually update notification response change
    private fun updateMediaSessionCustomLayout() {
        val buttons = commandButtons.build().filterNotNull()

        buttons.let {
            mediaSession.setCustomLayout(buttons)
        }
    }

    private fun addPlayerListener() {
        player.addListener(object : Player.Listener {
            override fun onRepeatModeChanged(repeatMode: Int) {
                super.onRepeatModeChanged(repeatMode)
                updateCommandButtons()

                updateMediaSessionCustomLayout()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                super.onShuffleModeEnabledChanged(shuffleModeEnabled)
                updateCommandButtons()

                updateMediaSessionCustomLayout()
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    var updatedMediaItems: MutableList<MediaItem> = ArrayList()

    private fun openMainActivityPendingIntent(mode: Int): PendingIntent? {
        val notifyIntent = Intent(this, MainActivity::class.java)

        notifyIntent.putExtra("ServicePlayerMode", mode)

        // set the Activity to start in a new, empty task
        notifyIntent.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )

        // create the PendingIntent
        return PendingIntent.getActivity(
            this, 0, notifyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaItemsWithStartPosition> {
            val settableFuture = SettableFuture.create<MediaItemsWithStartPosition>()
            val resumptionPlaylist = restorePlaylist()
            resumptionPlaylist.let {
                settableFuture.set(resumptionPlaylist)
            }
            return settableFuture
        }

        fun restorePlaylist(): MediaItemsWithStartPosition {
            fetchSongs()
            val songs: List<Song> = songs
            val mediaItems = getMediaItems(songs)
            val startIndex = savedMediaItemIndex
            val startPosition = sharedData.songPosition.toLong()

            return MediaItemsWithStartPosition(mediaItems, startIndex, startPosition)
        }

        fun getMediaItems(songs: List<Song>): List<MediaItem> {
            // define a list of media items
            val mediaItems: MutableList<MediaItem> = ArrayList()

            for (song in songs) {
                val mediaMetadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .build()
                val mediaItem = MediaItem.Builder()
                    .setMediaMetadata(mediaMetadata)
                    .setMediaId(song.uri.toString())
                    .build()

                // add the media item to media item list
                mediaItems.add(mediaItem)
            }
            return mediaItems
        }


        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                CUSTOM_COMMAND_REPEAT_ALL -> {
                    changeModeActionBroadcast(2)
                    player.repeatMode = Player.REPEAT_MODE_ONE
                    sharedData.playerMode = 2
                }

                CUSTOM_COMMAND_REPEAT_ONE -> {
                    changeModeActionBroadcast(3)
                    player.repeatMode = Player.REPEAT_MODE_OFF
                    player.shuffleModeEnabled = true
                    sharedData.playerMode = 3
                }

                CUSTOM_COMMAND_SHUFFLE -> {
                    changeModeActionBroadcast(1)
                    player.shuffleModeEnabled = false
                    player.repeatMode = Player.REPEAT_MODE_ALL
                    sharedData.playerMode = 1
                }

                CUSTOM_COMMAND_CLOSE_NOTIFICATION -> closeNotificationActionBroadcast()
            }
            openMainActivityPendingIntent(servicePlayerMode)?.let { sessionIntent ->
                mediaSession.setSessionActivity(sessionIntent)
            } ?: Log.w(TAG, "onCustomCommand: Failed, sessionIntent in $servicePlayerMode is null")

            return Futures.immediateFuture<SessionResult>(
                SessionResult(SessionResult.RESULT_SUCCESS)
            )
        }

        fun closeNotificationActionBroadcast() {
            player.pause()
            player.stop()
            saveCurrentSongStatus()
            val intent = Intent(MainActivity.REQUEST_CLOSE_MAIN_ACTIVITY_ACTION)

            sendBroadcast(intent)
            stopSelf()
        }

        fun changeModeActionBroadcast(mode: Int) {
            val intent = Intent(MainActivity.REQUEST_CHANGE_PLAYER_MODE_ACTION)
            intent.putExtra(MainActivity.CHANGE_PLAYER_MODE_KEY, mode)
            sendBroadcast(intent)
        }


        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ConnectionResult {
            val connectionResult =
                super.onConnect(session, controller)

            val sessionCommands = connectionResult.availableSessionCommands
                .buildUpon()
                .add(SessionCommand(CUSTOM_COMMAND_REPEAT_ALL, Bundle()))
                .add(SessionCommand(CUSTOM_COMMAND_REPEAT_ONE, Bundle()))
                .add(SessionCommand(CUSTOM_COMMAND_SHUFFLE, Bundle()))
                .add(SessionCommand(CUSTOM_COMMAND_CLOSE_NOTIFICATION, Bundle()))
                .build()

            return ConnectionResult.accept(
                sessionCommands, connectionResult.availablePlayerCommands
            )
        }

        override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
            // let the controller now about the custom layout right after it connected.
            if (commandButtons.build().isNotEmpty()) {
                // initialize notification command button
                updateMediaSessionCustomLayout()
            }
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long
        ): ListenableFuture<MediaItemsWithStartPosition> {
            updatedMediaItems = ArrayList()

            for (item in mediaItems)  // safely create clone from item
            // ensure the original item's value not are not accidentally modified
                updatedMediaItems.add(item.buildUpon().setUri(item.mediaId).build())

            // ListenableFuture may block the current thread until the result is available.
            val mediaItemsWithStartPosition =
                MediaItemsWithStartPosition(updatedMediaItems, startIndex, startPositionMs)

            return Futures.immediateFuture(mediaItemsWithStartPosition)
        }
    }

    private inner class CustomNotificationProvider(context: Context) :
        DefaultMediaNotificationProvider(context) {
        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> {
            updateCommandButtons()

            val safeList = commandButtons.build().filterNotNull()

            return ImmutableList.copyOf(safeList)
        }
    }

    private fun updateCommandButtons() {
        commandButtons = ImmutableList.Builder<CommandButton?>()

        val repeatAllButton = CommandButton.Builder()
            .setDisplayName(CUSTOM_COMMAND_REPEAT_ALL)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_ALL, Bundle()))
            .setIconResId(R.drawable.ic_repeat_all)
            .build()

        val repeatOneButton = CommandButton.Builder()
            .setDisplayName(CUSTOM_COMMAND_REPEAT_ONE)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_REPEAT_ONE, Bundle()))
            .setIconResId(R.drawable.ic_repeat_one)
            .build()

        val shuffleButton = CommandButton.Builder()
            .setDisplayName(CUSTOM_COMMAND_SHUFFLE)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_SHUFFLE, Bundle()))
            .setIconResId(R.drawable.ic_shuffle)
            .build()

        val skipPreviousCommandButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
            .setEnabled(true)
            .setIconResId(R.drawable.ic_skip_previous)
            .build()

        val playPausesCommandButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
            .setEnabled(true)
            .setIconResId(
                if (mediaSession.player.isPlaying)
                    R.drawable.ic_pause else R.drawable.ic_play
            )
            .build()

        val skipNextCommandButton = CommandButton.Builder()
            .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
            .setEnabled(true)
            .setIconResId(R.drawable.ic_skip_next)
            .build()

        val closeButton = CommandButton.Builder()
            .setDisplayName(CUSTOM_COMMAND_CLOSE_NOTIFICATION)
            .setSessionCommand(SessionCommand(CUSTOM_COMMAND_CLOSE_NOTIFICATION, Bundle()))
            .setIconResId(R.drawable.ic_close_notification)
            .build()

        // according the player mode to generate the notification button
        when (player.repeatMode) {
            Player.REPEAT_MODE_ALL -> commandButtons.add(repeatAllButton)
            Player.REPEAT_MODE_ONE -> commandButtons.add(
                repeatOneButton
            )
            Player.REPEAT_MODE_OFF -> commandButtons.add(
                shuffleButton
            )
            else -> commandButtons.add(repeatAllButton)
        }

        // add the others command button
        commandButtons.add(skipPreviousCommandButton)
        commandButtons.add(playPausesCommandButton)
        commandButtons.add(skipNextCommandButton)
        commandButtons.add(closeButton)
    }

    private fun saveCurrentSongStatus() {
        val item = player.currentMediaItem ?: return
        sharedData.apply {
            songPosition = currentSecond
            songMediaId = item.mediaId
            playerMode = when (player.repeatMode) {
                Player.REPEAT_MODE_ONE -> 2
                Player.REPEAT_MODE_OFF if player.shuffleModeEnabled -> 3
                else -> 1
            }
        }
    }

    private val currentSecond: Int
        get() {
            val position = player.currentPosition
            return getSecond(position)
        }

    fun getSecond(duration: Long): Int {
        val oneSec = 1000
        return Math.toIntExact(duration / oneSec)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        saveCurrentSongStatus()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        saveCurrentSongStatus()

        player.stop()
        mediaSession.player.release()
        mediaSession.release()
        player.release()

        super.onDestroy()
    }
}