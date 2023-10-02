package com.example.cometmusic.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.CommandButton;
import androidx.media3.session.DefaultMediaNotificationProvider;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import androidx.media3.session.SessionCommand;
import androidx.media3.session.SessionCommands;
import androidx.media3.session.SessionResult;

import com.example.cometmusic.MainActivity;
import com.example.cometmusic.R;
import com.example.cometmusic.model.FetchAudioFiles;
import com.example.cometmusic.model.SharedData;
import com.example.cometmusic.model.Song;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.List;

@UnstableApi
public class PlaybackService extends MediaSessionService {

    private final String TAG = "MyTag";
    private MediaSession mediaSession;
    ImmutableList.Builder<CommandButton> commandButtons;
    SharedData sharedData;

    // player
    ExoPlayer player;
    private final String CUSTOM_COMMAND_PLAY_PAUSE = "play/pause";
    private final String CUSTOM_COMMAND_REPEAT_ALL = "repeatAll";
    private final String CUSTOM_COMMAND_REPEAT_ONE = "repeatOne";
    private final String CUSTOM_COMMAND_SHUFFLE = "randomShuffle";
    private final String CUSTOM_COMMAND_CLOSE_NOTIFICATION = "closeNotification";
    private int servicePlayerMode;

    @Nullable
    @Override
    public ComponentName startForegroundService(Intent service) {
        return super.startForegroundService(service);
    }

    // ensure that startForeground is always called by system,
    // regardless of whether the player is playing or not
    // https://github.com/androidx/media/issues/167#issuecomment-1668542213
    @Override
    public void onUpdateNotification(@NonNull MediaSession session, boolean startInForegroundRequired) {
        super.onUpdateNotification(session, true);
    }

    // Create your player and media library session in the onCreate lifecycle event
    @Override
    public void onCreate() {
        super.onCreate();

        sharedData = new SharedData(this);

        servicePlayerMode = sharedData.getPlayerMode();

        // assign variables
        player = new ExoPlayer.Builder(this)
                // auto handle audio focus
                .setAudioAttributes(AudioAttributes.DEFAULT, true)
                // auto pause when user disconnect bluetooth or earphone
                // auto lower voice when phone call or alarm...
                .setHandleAudioBecomingNoisy(true)
                // set extension renderer
                // player can adapt multiply extension renderer, if they are exist...
                .setRenderersFactory(
                        new DefaultRenderersFactory(this).setExtensionRendererMode(
                                DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        )
                )
                .build();

        player.prepare();

        addPlayerListener();

        // Create a MediaSession
        mediaSession = new MediaSession.Builder(this, player)
                .setCallback(new SessionCallback())
                .setSessionActivity(openMainActivityPendingIntent(1))
                .build();

        updateCommandButtons();

        // manually update notification response change
        mediaSession.setCustomLayout(commandButtons.build());

        setMediaNotificationProvider(new customNotificationProvider(this));
    }

    private void addPlayerListener() {
        player.addListener(new Player.Listener() {
            @Override
            public void onRepeatModeChanged(int repeatMode) {
                if(commandButtons == null)
                    return;
                Player.Listener.super.onRepeatModeChanged(repeatMode);
                updateCommandButtons();

                // manually update notification response change
                mediaSession.setCustomLayout(commandButtons.build());
            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
                if(commandButtons == null)
                    return;
                Player.Listener.super.onShuffleModeEnabledChanged(shuffleModeEnabled);
                updateCommandButtons();

                // manually update notification response change
                mediaSession.setCustomLayout(commandButtons.build());
            }
        });
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    List<MediaItem> updatedMediaItems = new ArrayList<>();

    private PendingIntent openMainActivityPendingIntent(int mode) {
        Intent notifyIntent = new Intent(this, MainActivity.class);

        notifyIntent.putExtra("ServicePlayerMode", mode);

        // set the Activity to start in a new, empty task
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // create the PendingIntent

        return PendingIntent.getActivity(
                this, 0, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private class SessionCallback implements MediaSession.Callback {

        @NonNull
        @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onPlaybackResumption(@NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller) {
            SettableFuture<MediaSession.MediaItemsWithStartPosition> settableFuture = SettableFuture.create();
            settableFuture.addListener(() -> {
                // Your app is responsible for storing the playlist and the start position
                // to use here
                MediaSession.MediaItemsWithStartPosition resumptionPlaylist = restorePlaylist();
                settableFuture.set(resumptionPlaylist);
            }, MoreExecutors.directExecutor());
            return settableFuture;
        }

        private MediaSession.MediaItemsWithStartPosition restorePlaylist() {
            List<Song> songs = FetchAudioFiles.getInstance(getApplicationContext()).getSongs();
            List<MediaItem> mediaItems = getMediaItems(songs);
            int startIndex = FetchAudioFiles.getInstance(getApplicationContext()).getSavedMediaItemIndex();
            long startPosition = sharedData.getSongPosition();

            return new MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPosition);
        }

        private List<MediaItem> getMediaItems(List<Song> songs) {
            // define a list of media items
            List<MediaItem> mediaItems = new ArrayList<>();

            for (Song song : songs) {
                MediaMetadata mediaMetadata = new MediaMetadata.Builder()
                        .setTitle(song.getTitle())
                        .build();
                MediaItem mediaItem = new MediaItem.Builder()
                        .setMediaMetadata(mediaMetadata)
                        .setMediaId(String.valueOf(song.getUri()))
                        .build();

                // add the media item to media item list
                mediaItems.add(mediaItem);
            }
            return mediaItems;
        }


        @NonNull
        @Override
        public ListenableFuture<SessionResult> onCustomCommand(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller, SessionCommand customCommand, @NonNull Bundle args) {
            switch (customCommand.customAction) {
                case CUSTOM_COMMAND_PLAY_PAUSE:
                    updatePlayPauseButtonBroadcast();
                // repeat all -> repeat one
                case CUSTOM_COMMAND_REPEAT_ALL:
                    changeModeActionBroadcast(2);
                    player.setRepeatMode(Player.REPEAT_MODE_ONE);
                    sharedData.setPlayerMode(2);
                    break;
                // repeat one -> shuffle mode
                case CUSTOM_COMMAND_REPEAT_ONE:
                    changeModeActionBroadcast(3);
                    player.setRepeatMode(Player.REPEAT_MODE_OFF);
                    player.setShuffleModeEnabled(true);
                    sharedData.setPlayerMode(3);
                    break;
                // shuffle mode -> repeat all
                case CUSTOM_COMMAND_SHUFFLE:
                    changeModeActionBroadcast(1);
                    player.setShuffleModeEnabled(false);
                    player.setRepeatMode(Player.REPEAT_MODE_ALL);
                    sharedData.setPlayerMode(1);
                    break;
                case CUSTOM_COMMAND_CLOSE_NOTIFICATION:
                    closeNotificationActionBroadcast();
                    return Futures.immediateFuture(
                            new SessionResult(SessionResult.RESULT_SUCCESS));
            }
            mediaSession.setSessionActivity(openMainActivityPendingIntent(servicePlayerMode));

            return Futures.immediateFuture(
                    new SessionResult(SessionResult.RESULT_SUCCESS));
        }

        private void closeNotificationActionBroadcast() {
            player.pause();
            player.stop();
            saveCurrentSongStatus();
            Intent intent = new Intent(MainActivity.REQUEST_CLOSE_MAIN_ACTIVITY_ACTION);

            sendBroadcast(intent);
            stopSelf();
        }

        private void updatePlayPauseButtonBroadcast() {
            Intent intent = new Intent(MainActivity.REQUEST_CLOSE_MAIN_ACTIVITY_ACTION);
            sendBroadcast(intent);
        }

        private void changeModeActionBroadcast(int mode) {
            Intent intent = new Intent(MainActivity.REQUEST_CHANGE_PLAYER_MODE_ACTION);
            intent.putExtra(MainActivity.CHANGE_PLAYER_MODE_KEY, mode);
            sendBroadcast(intent);
        }


        @NonNull
        @Override
        public MediaSession.ConnectionResult onConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
            MediaSession.ConnectionResult connectionResult =
                    MediaSession.Callback.super.onConnect(session, controller);

            SessionCommands sessionCommands = connectionResult.availableSessionCommands
                    .buildUpon()
                    .add(new SessionCommand(CUSTOM_COMMAND_REPEAT_ALL, new Bundle()))
                    .add(new SessionCommand(CUSTOM_COMMAND_REPEAT_ONE, new Bundle()))
                    .add(new SessionCommand(CUSTOM_COMMAND_SHUFFLE, new Bundle()))
                    .add(new SessionCommand(CUSTOM_COMMAND_CLOSE_NOTIFICATION, new Bundle()))
                    .build();

            return MediaSession.ConnectionResult.accept(
                    sessionCommands, connectionResult.availablePlayerCommands);
        }

        @Override
        public void onPostConnect(@NonNull MediaSession session, @NonNull MediaSession.ControllerInfo controller) {
            // let the controller now about the custom layout right after it connected.
            if (!commandButtons.build().isEmpty()) {
                // initialize notification command button
                mediaSession.setCustomLayout(controller, commandButtons.build());
            }
        }

        @NonNull
        @Override
        public ListenableFuture<MediaSession.MediaItemsWithStartPosition> onSetMediaItems(@NonNull MediaSession mediaSession, @NonNull MediaSession.ControllerInfo controller, List<MediaItem> mediaItems, int startIndex, long startPositionMs) {
            updatedMediaItems = new ArrayList<>();

            for(MediaItem item : mediaItems)
                // safely create clone from item
                // ensure the original item's value not are not accidentally modified
                updatedMediaItems.add(item.buildUpon().setUri(item.mediaId).build());

            // ListenableFuture may block the current thread until the result is available.
            MediaSession.MediaItemsWithStartPosition mediaItemsWithStartPosition = new MediaSession.MediaItemsWithStartPosition(updatedMediaItems, startIndex, startPositionMs);

            return Futures.immediateFuture(mediaItemsWithStartPosition);
        }
    }

    private class customNotificationProvider extends DefaultMediaNotificationProvider {

        public customNotificationProvider(Context context) {
            super(context);
        }

        @NonNull
        @Override
        protected ImmutableList<CommandButton> getMediaButtons(@NonNull MediaSession session, @NonNull Player.Commands playerCommands, @NonNull ImmutableList<CommandButton> customLayout, boolean showPauseButton) {
            updateCommandButtons();

            return commandButtons.build();
        }
    }

    private void updateCommandButtons() {
        commandButtons = new ImmutableList.Builder<>();

        CommandButton repeatAllButton = new CommandButton.Builder()
                .setDisplayName(CUSTOM_COMMAND_REPEAT_ALL)
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_REPEAT_ALL, new Bundle()))
                .setIconResId(R.drawable.ic_repeat_all)
                .build();

        CommandButton repeatOneButton = new CommandButton.Builder()
                .setDisplayName(CUSTOM_COMMAND_REPEAT_ONE)
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_REPEAT_ONE, new Bundle()))
                .setIconResId(R.drawable.ic_repeat_one)
                .build();

        CommandButton shuffleButton = new CommandButton.Builder()
                .setDisplayName(CUSTOM_COMMAND_SHUFFLE)
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_SHUFFLE, new Bundle()))
                .setIconResId(R.drawable.ic_shuffle)
                .build();

        CommandButton skipPreviousCommandButton = new CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                .setEnabled(true)
                .setIconResId(R.drawable.ic_skip_previous)
                .build();

        CommandButton playPausesCommandButton = new CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_PLAY_PAUSE)
                .setDisplayName(CUSTOM_COMMAND_PLAY_PAUSE)
                .setEnabled(true)
                .setIconResId(mediaSession.getPlayer().isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play)
                .build();

        CommandButton skipNextCommandButton = new CommandButton.Builder()
                .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
                .setEnabled(true)
                .setIconResId(R.drawable.ic_skip_next)
                .build();

        CommandButton closeButton = new CommandButton.Builder()
                .setDisplayName(CUSTOM_COMMAND_CLOSE_NOTIFICATION)
                .setSessionCommand(new SessionCommand(CUSTOM_COMMAND_CLOSE_NOTIFICATION, new Bundle()))
                .setIconResId(R.drawable.ic_close_notification)
                .build();

        // according the player mode to generate the notification button
        if(player.getRepeatMode() == Player.REPEAT_MODE_ALL)
            commandButtons.add(repeatAllButton);
        else if(player.getRepeatMode() == Player.REPEAT_MODE_ONE)
            commandButtons.add(repeatOneButton);
        else if(player.getRepeatMode() == Player.REPEAT_MODE_OFF)
            commandButtons.add(shuffleButton);
        // for safety
        else
            commandButtons.add(repeatAllButton);

        // add the others command button
        commandButtons.add(skipPreviousCommandButton);
        commandButtons.add(playPausesCommandButton);
        commandButtons.add(skipNextCommandButton);
        commandButtons.add(closeButton);
    }

    private void saveCurrentSongStatus() {
        if(player == null || player.getCurrentMediaItem() == null)
            return;
        int playingCurrentSecond = getCurrentSecond();
        String currentSongId = player.getCurrentMediaItem().mediaId;

        int playerMode;

        if(player.getRepeatMode() == Player.REPEAT_MODE_ONE)
            playerMode = 2;
        else if(player.getRepeatMode() == Player.REPEAT_MODE_OFF &&
                player.getShuffleModeEnabled())
            playerMode = 3;
        else
            playerMode = 1;

        sharedData.setSongMediaId(currentSongId);
        sharedData.setSongPosition(playingCurrentSecond);
        sharedData.setPlayerMode(playerMode);
    }

    private int getCurrentSecond() {
        long position = player.getCurrentPosition();
        return getSecond(position);
    }

    public int getSecond(long duration) {
        final int oneSec = 1000;
        return Math.toIntExact(duration / oneSec);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved: ");
        saveCurrentSongStatus();
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        saveCurrentSongStatus();

        player.stop();
        mediaSession.getPlayer().release();
        mediaSession.release();
        mediaSession = null;

        super.onDestroy();
    }
}