package com.example.cometmusic.fragment;

import static android.app.Activity.RESULT_OK;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.util.Consumer;
import androidx.core.view.MenuHost;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.NonNullApi;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cometmusic.R;
import com.example.cometmusic.databinding.FragmentCurrentListBinding;
import com.example.cometmusic.model.SharedData;
import com.example.cometmusic.model.Song;
import com.example.cometmusic.service.PlaybackService;
import com.example.cometmusic.utils.ItemSpacingDecoration;
import com.example.cometmusic.utils.SongAdapter;
import com.example.cometmusic.viewmodel.PlayerViewModel;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import me.zhanghai.android.fastscroll.FastScrollerBuilder;
import me.zhanghai.android.fastscroll.PopupStyles;
import me.zhanghai.android.fastscroll.PopupTextProvider;

@UnstableApi public class CurrentListFragment extends Fragment
        implements SongAdapter.PlayerControlListener {


    private static final String TAG = "MyTag";
    private int savedMediaItemIndex = 0;

    private boolean firstLoadSavedSong = true;

    private boolean isBackPressedToCloseTwice = false;

    SongAdapter songAdapter;

    Drawable thumbDrawable, trackDrawable;

    List<Song> allSongs = new ArrayList<>();

    SearchView searchView;

    int songsSize = 0;

    int lastVisibleItemPosition = 0;

    private boolean isServiceAlreadyRunning = false;

    private MenuItem searchMenuItem;

    private FragmentCurrentListBinding mainBinding;

    private final boolean DEV_MODE = false;
    MediaController player;

    private PlayerViewModel playerViewModel;
    private SharedData sharedData;

    Player.Listener playerListener;
    private ListenableFuture<MediaController> mediaControllerFuture;
    private SessionToken sessionToken;
    private boolean isReloadFolder = false;

    private boolean isKeyboardVisible = false;

    public CurrentListFragment() {
        super(R.layout.fragment_current_list);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleBackPressedCallback();
    }

    private void handleBackPressedCallback() {

        // handle the back button event
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isBackPressedToCloseTwice) {
                    requireActivity().finish();
                }
                else {
                    isBackPressedToCloseTwice = true;
                    Toast.makeText(requireContext(), R.string.click_double_back_message, Toast.LENGTH_SHORT).show();

                    new Handler().postDelayed(() -> isBackPressedToCloseTwice = false, 2000);
                }
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // inflate the layout for this fragment
        mainBinding = FragmentCurrentListBinding.inflate(inflater, container, false);

        mainBinding.getRoot().setFocusableInTouchMode(true);
        mainBinding.getRoot().requestFocus();

        return mainBinding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        initializeMenuToolbar(view);


        // spacing between items (adjust as needed)
        ItemSpacingDecoration itemSpacingDecoration = new ItemSpacingDecoration();
        mainBinding.recyclerview.addItemDecoration(itemSpacingDecoration);

        thumbDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.scroll_thumb);
        trackDrawable = AppCompatResources.getDrawable(requireContext(), R.drawable.scroll_track);

        if (DEV_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyDialog()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        sharedData = new SharedData(requireContext());


        playerViewModel = new ViewModelProvider(requireActivity()).get(PlayerViewModel.class);

        sessionToken = playerViewModel.getSessionToken().getValue();

        if(sessionToken == null) {
            sessionToken = new SessionToken(requireContext(),
                    new ComponentName(requireContext(), PlaybackService.class));
        }

        mediaControllerFuture =
                new MediaController.Builder(requireContext(), sessionToken).buildAsync();

        playerViewModel.setSessionToken(sessionToken);

        // set layout xml variable
        mainBinding.setPlayerViewModel(playerViewModel);
        mainBinding.setCurrentListFragment(this);


        mainBinding.setLifecycleOwner(getViewLifecycleOwner());

        // recyclerview same height
        mainBinding.recyclerview.setHasFixedSize(true);

        // layoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        mainBinding.recyclerview.setLayoutManager(layoutManager);

        mainBinding.recyclerview.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition());

        // recyclerView pool
        RecyclerView.RecycledViewPool recycledViewPool = new RecyclerView.RecycledViewPool();
        mainBinding.recyclerview.setRecycledViewPool(recycledViewPool);

        initializePlayer();

        initializeObservers();
    }

    private void initializePlayer() {
        mediaControllerFuture.addListener(() -> {
            try {
                // store the player to the viewModel
                player = mediaControllerFuture.get();
                playerViewModel.setPlayer(player);
                fetchSongs();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            // using current (main) thread
        }, MoreExecutors.directExecutor());
    }

    private void initializeMenuToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar_list_fragment);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        MenuHost menuHost = requireActivity();
        menuHost.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // inflate the menu and add the search button to the menu, positioning it as defined in the xml file.
                menuInflater.inflate(R.menu.toolbar_layout, menu);
                // get search button item
                searchMenuItem = menu.findItem(R.id.searchBtn);
                searchView = (SearchView) searchMenuItem.getActionView();
            }
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                int id = menuItem.getItemId();
                // when click the choose main folder, trigger asking user's folder access permission
                if(id == R.id.action_choose_main_folder) {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    folderPickerLauncher.launch(intent);
                    return true;
                }
                else if(id == R.id.reset_main_folder) {
                    reloadAll(null);
                    return true;
                }
                else if(id == R.id.searchBtn) {
                    isKeyboardVisible = true;
                    searchSongs(searchView);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void searchSongs(SearchView searchView) {
        
        searchMenuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true; // KEEP IT TO TRUE OR IT DOESN'T OPEN !!
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if(isKeyboardVisible) {
                    hideKeyboard();
                    item.getActionView().requestFocus();
                    // if keyboardVisible, do NOT collapse SearchView
                    return false;
                }

                return true;// OR FALSE IF YOU DIDN'T WANT IT TO CLOSE!
            }
        });

        // search view listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                // filter songs
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        View currentFocus = requireActivity().getCurrentFocus();
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
        isKeyboardVisible = false;
    }

    private void initializeObservers() {
        playerViewModel.getIsPlaying().observe(getViewLifecycleOwner(), isPlaying -> {
            if(isPlaying) {
                updatePauseToPlayUI();
            }
            else {
                updatePlayToPauseUI();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();

        // postponing the execution until activity enters onStart() state
        requireActivity().getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner lifecycleOwner, @NonNull Lifecycle.Event event) {
                if(event.getTargetState() == Lifecycle.State.STARTED) {
                    Objects.requireNonNull(((AppCompatActivity) requireActivity()).
                            getSupportActionBar()).setDisplayShowTitleEnabled(false);

                    requireActivity().getLifecycle().removeObserver(this);
                }
            }
        });

        if (player != null) {
            playerControls();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        playerViewModel.checkIsPlaying();
    }

    private void filterSongs(String query) {

        songAdapter.setIsSearch(!query.isEmpty());

        songAdapter.startCountdownTimer();

        List<Song> filteredList = new ArrayList<>();

        if (allSongs.size() > 0) {
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(query)) {
                    filteredList.add(song);
                }
            }

            if (songAdapter != null) {
                songAdapter.clearImageCache();

                songsSize = filteredList.size();
                songAdapter.filterSongs(filteredList);
            }
        }
    }

    private void playerControls() {

        initializePlayerListener();

        player.addListener(playerListener);
    }

    public void showCurrentPlayerView() {
        // execute action
        Navigation.findNavController(requireView())
                .navigate(R.id.action_currentListFragment_to_currentPlayerViewFragment);

        Log.d(TAG, "showCurrentPlayerView: 1");
        scrollToPosition(playerViewModel.getPlayerCurrentIndex());
    }

    private void scrollToPosition(int scrollPosition) {
        if (scrollPosition < allSongs.size()) {
            mainBinding.recyclerview.scrollToPosition(scrollPosition);
        }
        if(songAdapter != null) {
            Log.d(TAG, "scrollToPosition: self");
            songAdapter.setViewBorder(scrollPosition);
            // make sure load image
            songAdapter.notifyItemChanged(scrollPosition);
        }
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
                playerViewModel.checkIsPlaying();
            }

            // just for update UI, cuz player is buffering
            @Override
            public void onMediaItemTransition(@NonNullApi MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);

                int currentSongIndex = Objects.requireNonNull(playerViewModel.getCurrentSongIndex().getValue());

                if(currentSongIndex != player.getCurrentMediaItemIndex()) {

                    playerViewModel.setCurrentSongIndex(player.getCurrentMediaItemIndex());

                    onSongTransition();
                }

            }

            // handle play status
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                Player.Listener.super.onPlaybackStateChanged(playbackState);

                if (playbackState == ExoPlayer.STATE_READY) {

                    handlePlayerUI();
                }
            }
        };
    }


    private void onSongTransition() {

        if(!playerViewModel.isPlayerExistMediaItem())
            return;

        playerViewModel.setCurrentSongName();

        scrollToPosition(player.getCurrentMediaItemIndex());

        if (songAdapter != null) {
            songAdapter.clearViewBorder(false);
        }

        playerViewModel.checkIsPlaying();
    }



    private void handlePlayerUI() {

        // set the song duration in seconds
        playerViewModel.setDurationSecond();

        // set the song current position in seconds
        playerViewModel.setCurrentSecond();

        if (firstLoadSavedSong) {
            // set song name
            playerViewModel.setCurrentSongName();

            // set current border
            // first load not trigger onMediaItemTransition
            songAdapter.setViewBorder(playerViewModel.getPlayerCurrentIndex());
            firstLoadSavedSong = false;
        }

        playerViewModel.checkIsPlaying();
    }

    public void updatePauseToPlayUI() {
        mainBinding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause, 0, 0, 0);
    }

    public void updatePlayToPauseUI() {
        mainBinding.homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play, 0, 0, 0);
    }

    private void fetchSongs() {

        savedMediaItemIndex = 0;

        allSongs = playerViewModel.getSongs().getValue();

        if (allSongs != null) {
            savedMediaItemIndex = playerViewModel.getSavedMediaItemIndex();
        }

        showSongs();
    }

    private void showSongs() {

        playerControls();

        // set playerMode
        if(player != null) {
            int mode = Objects.requireNonNull(playerViewModel.getPlayerMode().getValue());
            playerViewModel.setPlayerMode(mode);
        }

        String toolbarTitle = getResources().getString(R.string.app_name);


        songsSize = allSongs.size();
        if (songsSize == 0) {
            mainBinding.toolbarTitle.setText(toolbarTitle);
            mainBinding.homeControlWrapper.setVisibility(View.GONE);
            Toast.makeText(requireContext(), R.string.no_songs, Toast.LENGTH_SHORT).show();
            return;
        }
        // update the toolbar title
        else {
            toolbarTitle += " - " + songsSize;
        }
        mainBinding.toolbarTitle.setText(toolbarTitle);

        // check if the player holds a mediaItem before calling the setMediaItems method,
        // If it does, it indicates that the service had been running even after the activity was destroyed
        if(playerViewModel.isPlayerExistMediaItem()) {
            isServiceAlreadyRunning = true;
        }

        // open player view when click home control wrapper

        // song name marquee
        mainBinding.homeSongNameView.setSelected(true);

        // songs adapter
        songAdapter = new SongAdapter(requireContext(), player, allSongs, mainBinding.recyclerview);

        mainBinding.recyclerview.setItemViewCacheSize(100);

        // implement PlayerControllerListener interface
        songAdapter.setPlayerControlListener(this);

        // set the adapter to recyclerview
        mainBinding.recyclerview.setAdapter(songAdapter);

        /*
          if the users open the app without the service running
          or if the user click reload folder
          re-setMediaItems and load stored song status
         */
        if(!isServiceAlreadyRunning || isReloadFolder) {
            playerViewModel.setPlayerMediaItems(songAdapter.getMediaItems());
            isReloadFolder = false;

            // scroll to target position
            Log.d(TAG, "showSongs: 3 --- " + savedMediaItemIndex);
            scrollToPosition(savedMediaItemIndex);

            int savedPosition = 0;

            // if finding the song in current path, then get stored position
            if(Boolean.FALSE.equals(playerViewModel.getSavedSongNotFound().getValue())) {
                savedPosition = sharedData.getSongPosition();
            }

            // set the MediaItem and position to the player
            playerViewModel.seekToSongIndexAndPosition(savedMediaItemIndex, savedPosition * 1000L);

            playerViewModel.preparePlayer();

            // set the song duration in seconds
            playerViewModel.setDurationSecond();

            // set the song current position in seconds
            playerViewModel.setCurrentSecond();
        }
        /*
          if the users open the app with the service running (without reload folder)
          load current player status
         */
        else {
            playerViewModel.preparePlayer();

            // scroll to target position
            scrollToPosition(playerViewModel.getPlayerCurrentIndex());

            handlePlayerUI();
        }

        setFastScroller();
    }

    private void setFastScroller() {
        // record previous position
        AtomicInteger previous_position = new AtomicInteger();

        // every 2 second or position change will call this method
        // find current view song index and allSong size
        PopupTextProvider popupTextProvider = position -> {
            // it only set the timer when the user trigger scrolling
            if(position != previous_position.get()) {
                songAdapter.startCountdownTimer();
            }
            previous_position.set(position);
            // the top of current view or the bottom song
            int pos = lastVisibleItemPosition + 1 == songsSize ? songsSize : position + 1;
            return String.format(Locale.getDefault(), "%d / %d", pos, songsSize);
        };

        // by PopupStyles.java
        Consumer<TextView> popupStyleConsumer = textView -> {
            // let default style apply to textView style
            PopupStyles.DEFAULT.accept(textView);
            // text size
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);

            // popup wrapper layout
            ViewGroup.LayoutParams layoutParams = textView.getLayoutParams();
            // set layout width and height and apply to textView style
            layoutParams.width = Math.min(ViewGroup.LayoutParams.WRAP_CONTENT, 500);
            layoutParams.height = 50;
            textView.setLayoutParams(layoutParams);
        };

        if (thumbDrawable == null || trackDrawable == null) {
            return;
        }

        // create fast scroller
        FastScrollerBuilder fastScrollerBuilder = new FastScrollerBuilder(mainBinding.recyclerview)
                .setThumbDrawable(thumbDrawable)
                .setTrackDrawable(trackDrawable)
                .setPopupTextProvider(popupTextProvider)
                .setPopupStyle(popupStyleConsumer);
        fastScrollerBuilder.disableScrollbarAutoHide();
        fastScrollerBuilder.build();
    }

    // create an ActivityResultLauncher to handle the folder selection
    private final ActivityResultLauncher<Intent> folderPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedFolderUri = data.getData();
                        // Take persistable URI permission
                        requireContext().getContentResolver().takePersistableUriPermission(
                                selectedFolderUri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );
                        reloadAll(selectedFolderUri.toString());
                    }
                }
            });

    private void reloadAll(String uriString) {

        playerViewModel.setSongs(null);

        if(Objects.equals(uriString, sharedData.getChooseDir())) {
            Toast.makeText(requireContext(), R.string.same_as_the_current_main_music_folder, Toast.LENGTH_SHORT).show();
            return;
        }
        // save URI
        sharedData.setChooseDir(uriString);

        isReloadFolder = true;

        playerViewModel.saveCurrentSongStatus();

        // clear previous and current border
        if(songAdapter != null)
            songAdapter.clearViewBorder(true);

        // release and prepare to reload player
        playerViewModel.clearPlayer();

        fetchSongs();
    }


    // click the same item
    // if it is not playing, then play
    @Override
    public void onSameItemClicked() {
        if(Boolean.FALSE.equals((playerViewModel.getIsPlaying()).getValue())) {
            playerViewModel.clickPlayPauseBtn();
        }
    }

    @Override
    public void collapseActionView() {
        // close SearchView
        if(searchMenuItem != null) {
            hideKeyboard();
            searchMenuItem.collapseActionView();
        }
    }

    @Override
    public void onStop() {
        if (player != null) {
            player.removeListener(playerListener);
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        if(songAdapter != null) {
            songAdapter.clearImageCache();
            songAdapter = null;
        }
        if(player != null)
            playerViewModel.saveCurrentSongStatus();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mediaControllerFuture = null;
        sessionToken = null;
        super.onDestroy();
    }
}
