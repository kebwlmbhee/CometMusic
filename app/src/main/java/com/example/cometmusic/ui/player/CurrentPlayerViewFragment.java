package com.example.cometmusic.ui.player;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.NonNullApi;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.cometmusic.R;
import com.example.cometmusic.data.SharedData;
import com.example.cometmusic.databinding.FragmentCurrentPlayerViewBinding;
import com.example.cometmusic.model.Song;
import com.example.cometmusic.model.SongKt;
import com.example.cometmusic.ui.PlayerViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class CurrentPlayerViewFragment extends Fragment {

    private static final String TAG = "MyTag";
    private FragmentCurrentPlayerViewBinding playerViewBinding;

    private PlayerViewModel playerViewModel;
    private boolean isSeekbarBeingTouched = false;
    private boolean isUpdatingProgress = false;

    private SharedData sharedData;

    MediaController player;
    private final Handler mCalHandler = new Handler(Looper.getMainLooper());
    private Player.Listener playerListener;
    private SessionToken sessionToken;
    private ListenableFuture<MediaController> mediaControllerFuture;
    List<Song> songs;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedData = new SharedData(requireContext());
        handleBackPressedCallback();
    }

    private void handleBackPressedCallback() {

        // handle the back button event
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitPlayerView();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the layout for this fragment
        playerViewBinding = FragmentCurrentPlayerViewBinding.inflate(inflater, container, false);

        return playerViewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        player = playerViewModel.getPlayer().getValue();

        playerViewBinding.setPlayerViewModel(playerViewModel);
        playerViewBinding.setPlayerViewFragment(this);

        playerViewBinding.setLifecycleOwner(getViewLifecycleOwner());

        // song name marquee
        playerViewBinding.songNameView.setSelected(true);

        songs = Objects.requireNonNull(playerViewModel.getSongs().getValue());

        initializeObservers();

        setClickAndTouchEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(playerViewModel.getPlayerOrNull().getValue() != null)
            isPlayingOrNot(playerViewModel.getIsPlaying());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (player != null && playerViewModel.isPlayerExistMediaItem())
            playerControls();
        else
            initializePlayer();
    }

    private void initializePlayer() {
        sessionToken = playerViewModel.getSessionToken().getValue();

        mediaControllerFuture =
                new MediaController.Builder(requireContext(), sessionToken).buildAsync();

        mediaControllerFuture.addListener(() -> {
            try {
                // store the player to the viewModel
                player = mediaControllerFuture.get();
                playerViewModel.setPlayer(player);

                playerViewModel.setPlayerMediaItems(playerViewModel.getPlayerMediaItems().getValue());
                isPlayingOrNot(player.isPlaying());

                int index = playerViewModel.getPlayerCurrentIndex();
                long position = sharedData.getSongPosition() * 1000L;
                playerViewModel.seekToSongIndexAndPosition(index, position);

                playerControls();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            // using current (main) thread
        }, MoreExecutors.directExecutor());
    }

    private void initializeObservers() {
        playerViewModel.getPlayerMode().observe(getViewLifecycleOwner(), playerMode -> {
            switch (playerMode) {
                case 2:
                    playerViewBinding.playerModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one, 0, 0, 0);
                    break;
                case 3:
                    playerViewBinding.playerModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle, 0, 0, 0);
                    break;
                default:
                    playerViewBinding.playerModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_all, 0, 0, 0);
                    break;
            }
        });
    }

    private void playerControls() {

        playerViewModel.setDurationSecond();
        playerViewModel.setCurrentSecond();

        // make sure seekbar update initially
        playerViewBinding.seekbar.setMax(Objects.requireNonNull(playerViewModel.getDurationSecond().getValue()));
        playerViewBinding.seekbar.setProgress(Objects.requireNonNull(playerViewModel.getCurrentSecond().getValue()));

        updateCurrentPlayingCover(playerViewModel.getPlayerCurrentIndex());

        initializePlayerListener();

        player.addListener(playerListener);
    }

    private void initializePlayerListener() {
        playerListener = new Player.Listener() {

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Player.Listener.super.onPlayerError(error);
                Toast.makeText(requireContext(), error.toString(), Toast.LENGTH_SHORT).show();
                error.printStackTrace();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Player.Listener.super.onIsPlayingChanged(isPlaying);
                isPlayingOrNot(isPlaying);
            }

            // just for update UI, cuz player is buffering
            @Override
            public void onMediaItemTransition(@NonNullApi MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);

                onSongTransition();
            }

            // handle play status
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);
            }
        };
    }

    private void isPlayingOrNot(boolean isPlaying) {
        if(isPlaying) {
            startUpdatingProgress();
            updatePauseToPlayUI();
        }
        else {
            stopUpdatingProgress();
            updatePlayToPauseUI();
        }
    }

    private void onSongTransition() {

        if(!playerViewModel.isPlayerExistMediaItem())
            return;

        handlePlayerUI();

        // if it is after onAttach and before onDetach
        if(isAdded()) {
            updateCurrentPlayingCover(playerViewModel.getPlayerCurrentIndex());
        }
    }

    private void handlePlayerUI() {

        // set the song duration in seconds
        playerViewModel.setDurationSecond();
        // set the song current position in seconds
        playerViewModel.setCurrentSecond();

        playerViewModel.setCurrentSongName();
    }

    public void updateCurrentPlayingCover(int position) {
        Bitmap currentCover = getCurrentPlayingCover(position);
        if (currentCover != null) {
            playerViewBinding.artworkView.setImageBitmap(currentCover);
        }
        // default artwork
        else {
            playerViewBinding.artworkView.setImageResource(R.drawable.ic_music_artwork);
        }
    }

    public Bitmap getCurrentPlayingCover(int position) {
        if (position >= 0 && position < songs.size()) {
            Song song = songs.get(position);
            return SongKt.getCoverImage(song, requireContext());
        }
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setClickAndTouchEvent() {

        playerViewBinding.seekbar.setOnTouchListener(new View.OnTouchListener() {

            int cancelMarkerRawLeftX = 0;
            int cancelMarkerRawTopY = 0;
            int cancelMarkerRawRightX = 0;
            int cancelMarkerRawBottomY = 0;
            float eventX = 0;
            float eventY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Drawable cancelDrawable = ContextCompat.getDrawable(v.getContext(), R.drawable.ic_cancel);
                int action = event.getAction();
                // get absolute position
                int[] location = new int[2];
                playerViewBinding.cancelMarker.getLocationOnScreen(location);
                cancelMarkerRawLeftX = location[0];
                cancelMarkerRawTopY = location[1];
                cancelMarkerRawRightX = location[0] + playerViewBinding.cancelMarker.getWidth();
                cancelMarkerRawBottomY = location[1] + playerViewBinding.cancelMarker.getHeight();
                eventX = event.getRawX();
                eventY = event.getRawY();

                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        // set cancelMarker visibility
                        playerViewBinding.cancelMarker.setVisibility(View.VISIBLE);
                        isSeekbarBeingTouched = true;
                        break;

                    case MotionEvent.ACTION_MOVE:

                        // get visible timeline
                        int progress = calculateProgressFromTouchEvent(event);
                        playerViewModel.setSecondAndStringWhenMoving(progress);

                        playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);

                        // determine whether the cancelMarker should turn red or remain green
                        if (touchCancelMarker()) {
                            Objects.requireNonNull(cancelDrawable).setTint(Color.RED);
                            playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);
                        }
                        else {
                            Objects.requireNonNull(cancelDrawable).setTint(getResources().getColor(R.color.light_green, requireContext().getTheme()));
                            playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        isSeekbarBeingTouched = false;
                        // cancel user timeline manipulation
                        if (touchCancelMarker()) {
                            Objects.requireNonNull(cancelDrawable).setTint(Color.RED);
                            playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);

                            playerViewModel.setCurrentSecond();
                            // reset to green color
                            playerViewBinding.cancelMarker.setVisibility(View.GONE);
                            Objects.requireNonNull(cancelDrawable).setTint(getResources().getColor(R.color.light_green, requireContext().getTheme()));
                            playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);
                        }
                        // set current song to user timeline
                        else {
                            progress = calculateProgressFromTouchEvent(event);
                            // second to millisecond
                            playerViewModel.seekToPosition(progress * 1000L);
                            playerViewBinding.cancelMarker.setVisibility(View.GONE);
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        isSeekbarBeingTouched = false;

                        Objects.requireNonNull(cancelDrawable).setTint(Color.RED);
                        playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);

                        playerViewModel.setCurrentSecond();

                        playerViewBinding.cancelMarker.setVisibility(View.GONE);
                        Objects.requireNonNull(cancelDrawable).setTint(getResources().getColor(R.color.light_green, requireContext().getTheme()));
                        playerViewBinding.cancelMarker.setCompoundDrawablesRelativeWithIntrinsicBounds(cancelDrawable, null, null, null);
                        return true;
                }
                return false;
            }

            // set progressView
            private int calculateProgressFromTouchEvent(MotionEvent event) {
                int max = playerViewBinding.seekbar.getMax();
                int leftEdge = playerViewBinding.seekbar.getPaddingLeft();
                int rightEdge = playerViewBinding.seekbar.getWidth() - playerViewBinding.seekbar.getPaddingRight();

                // adjust touchX by considering leftEdge & rightEdge
                float touchX = event.getX();
                touchX = Math.max(touchX, leftEdge);
                touchX = Math.min(touchX, rightEdge);

                // calculate progress based on the adjusted touchX
                float progressPercentage = (touchX - leftEdge) / (rightEdge - leftEdge);
                return (int) (progressPercentage * max);
            }

            private boolean touchCancelMarker() {
                return eventX >= cancelMarkerRawLeftX && eventX <= cancelMarkerRawRightX &&
                        eventY >= cancelMarkerRawTopY && eventY <= cancelMarkerRawBottomY;
            }
        });
    }

    public void exitPlayerView() {
        NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
        navController.navigateUp();
    }

    public void updatePauseToPlayUI() {
        playerViewBinding.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline, 0, 0, 0);
    }

    public void updatePlayToPauseUI() {
        playerViewBinding.playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline, 0, 0, 0);
    }

    private final Runnable updateProgressTask = new Runnable() {
        public void run() {
            // if user scroll song seekBar, then don't update progress and progressView text
            // let user feel free to scroll
            if (playerViewModel.getIsPlaying() && !isSeekbarBeingTouched) {
                playerViewModel.setCurrentSecond();
            }
            // schedule the next update task
            scheduleNextUpdate();
        }
    };

    private void scheduleNextUpdate() {
        long now = SystemClock.uptimeMillis();
        long next = now + (500 - now % 500);
        mCalHandler.postAtTime(updateProgressTask, next);
    }

    private void startUpdatingProgress() {
        // schedule the first update task
        if (!isUpdatingProgress) {
            isUpdatingProgress = true;
            scheduleNextUpdate();
        }
    }

    private void stopUpdatingProgress() {
        // remove the update task from the handler
        isUpdatingProgress = false;
        mCalHandler.removeCallbacks(updateProgressTask);
    }

    @Override
    public void onStop() {
        stopUpdatingProgress();
        playerViewModel.saveCurrentSongStatus();
        if (player != null)
            player.removeListener(playerListener);
        super.onStop();
    }

    @Override
    public void onDestroy() {
        mediaControllerFuture = null;
        sessionToken = null;
        super.onDestroy();
    }
}