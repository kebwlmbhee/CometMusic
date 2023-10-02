package com.example.musicplayer.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.musicplayer.R;
import com.example.musicplayer.databinding.SongRowItemBinding;
import com.example.musicplayer.model.Song;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MyTag";

    private boolean isScrolling = false;

    private MaterialCardView currentCardHolder = null;
    private MaterialCardView previousCardHolder = null;

    private int currentIndex = -1;
    private int previousIndex = -1;

    private final Handler viewBorderHolder = new Handler();

    private final Handler countTimeHandler = new Handler();

    // members
    Context context;
    List<Song> songs;
    LinearLayoutManager layoutManager;

    int start = 0, end = 0;
    RecyclerView recyclerView;

    MediaController player;

    private PlayerControlListener playerControlListener;

    boolean isSearchResultsClicked = false;

    // constructor
    public SongAdapter(Context context, MediaController player, List<Song> songs, RecyclerView recyclerView) {
        this.context = context;
        this.player = player;
        this.songs = songs;
        this.recyclerView = recyclerView;
        setHasStableIds(true);
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    }

    public void startCountdownTimer() {
        isScrolling = true;
        // remove previous count time handler
        countTimeHandler.removeCallbacksAndMessages(null);
        int loadCoverImageAfterStopScrollingMillisecond = 3000;
        countTimeHandler.postDelayed(() -> {
            isScrolling = false;
            showVisibleBitmaps();
        }, loadCoverImageAfterStopScrollingMillisecond);
    }

    private void showVisibleBitmaps() {
        // if scrolling has stopped, trigger image loading
        // calculate start and end
        start = Math.max(layoutManager.findFirstVisibleItemPosition(), 0);
        end = layoutManager.findLastVisibleItemPosition();
        for (int i = start; i <= end; ++i)
            notifyItemChanged(i);
    }

    public void clearImageCache() {
        Glide.get(context).clearMemory();
    }

    public interface PlayerControlListener {
        void onSameItemClicked();

        void collapseActionView(int position);
    }

    public void setPlayerControlListener(PlayerControlListener listener) {
        this.playerControlListener = listener;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        ((SimpleItemAnimator) Objects.requireNonNull(recyclerView.getItemAnimator())).setSupportsChangeAnimations(false);

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        SongRowItemBinding binding = DataBindingUtil.inflate(inflater, R.layout.song_row_item, parent, false);

        return new SongBindingHolder(binding);
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof SongBindingHolder) {
            SongBindingHolder viewHolder = (SongBindingHolder) holder;
            Glide.with(context).clear(viewHolder.binding.itemArtworkView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // current song and view holder
        Song song = songs.get(position);
        SongBindingHolder viewHolder = (SongBindingHolder) holder;
        MaterialCardView cardView = ((SongBindingHolder) holder).binding.card;

        // set values to views
        viewHolder.binding.titleView.setText(song.getTitle());
        viewHolder.binding.durationView.setText(getDuration(song.getDuration()));
        viewHolder.binding.sizeView.setText(getSize(song.getSize()));

        // set and clear border, slow but stable
        if(player.getCurrentMediaItem() != null &&
                player.getCurrentMediaItem().mediaId.equals(String.valueOf(song.getUri()))) {
            if(cardView.getStrokeWidth() == 0) {
                cardView.setStrokeWidth(8);
            }
        }
        else if(cardView.getStrokeWidth() == 8){
            cardView.setStrokeWidth(0);
        }

        // load image
        if (!isScrolling) {
            loadCoverImageWithGlide(viewHolder, song);
        }

        // on item click
        viewHolder.itemView.setOnClickListener(view -> {
            if (player == null) {
                return;
            }

            // when user click search result
            if (isSearchResultsClicked) {
                int targetPosition = song.getPlaylistPosition();
                // trigger close SearchView
                playerControlListener.collapseActionView(targetPosition);
                if (layoutManager != null) {
                    // scroll to make the clicked item appear at the top of the layout
                    layoutManager.scrollToPositionWithOffset(targetPosition, 0);
                }
                notifyItemChanged(targetPosition);
                waitForLayoutAndStartFlashing(targetPosition);
                isSearchResultsClicked = false;
            }
            // if the clicked viewHolder is the same as currentMetadata
            else if (player.getCurrentMediaItem() != null &&
                    player.getCurrentMediaItem().mediaId.equals(String.valueOf(song.getUri()))) {
                playerControlListener.onSameItemClicked();
            }
            // if a song is currently playing and the user switches to a different song
            else {
                player.pause();
                player.seekTo(position, 0);

//                setViewBorder(position);

                player.prepare();
                player.play();
            }
        });
    }
    // fast setting border
    public void setViewBorder(int viewPosition) {
        if(currentCardHolder != null)
            currentCardHolder.setStrokeWidth(8);

        if(viewPosition >= 0 && viewPosition <= songs.size()) {
            // wait until recyclerView finish computation

            recyclerView.post(() -> {
                // clear previous border
                previousCardHolder = currentCardHolder;
                previousIndex = currentIndex;
                if (previousCardHolder != null) {
                    clearViewBorder(false);
                }

                // set current border
                SongBindingHolder currentHolder = (SongBindingHolder) recyclerView.findViewHolderForAdapterPosition(viewPosition);
                if (currentHolder != null) {
                    currentCardHolder = currentHolder.binding.card;
                    currentIndex = viewPosition;
                    currentCardHolder.setStrokeWidth(8);
                }
            });
        }
    }
    public void clearViewBorder(boolean clearCurrent) {
        if(previousCardHolder != null && previousCardHolder.getStrokeWidth() != 0) {
            previousCardHolder.setStrokeWidth(0);
        }

        SongBindingHolder previousViewHolder = (SongBindingHolder) recyclerView.findViewHolderForAdapterPosition(previousIndex);
        if (previousViewHolder != null && previousViewHolder.binding.card.getStrokeWidth() != 0) {
            previousViewHolder.binding.card.setStrokeWidth(0);
        }

        if(clearCurrent && currentCardHolder != null) {
            currentCardHolder.setStrokeWidth(0);
            previousCardHolder = null;
            currentCardHolder = null;
            previousIndex = -1;
            currentIndex = -1;
        }
    }

    // for search result
    public void waitForLayoutAndStartFlashing(int position) {
        // attempt to retrieve the ViewHolder
        // if it doesn't exist yet, wait for the layout to be fully generated.
        if (recyclerView.getLayoutManager() != null) {
            recyclerView.post(() -> {
                SongBindingHolder flashHolder = (SongBindingHolder) recyclerView.findViewHolderForAdapterPosition(position);
                if (flashHolder != null) {
                    startFlashing(flashHolder, position);
                }
            });
        }
    }

    public void startFlashing(SongBindingHolder flashHolder, int position) {

        final int NUM_FLASHES = 7;
        final int FLASH_DELAY_MS = 500;
        for (int i = 0; i < NUM_FLASHES; i++) {
            MaterialCardView flashCardHolder = flashHolder.binding.card;
            int times = i;
            viewBorderHolder.postDelayed(() -> {
                // if the song clicked by the user is the current song, then should be set stroke
                if(times == NUM_FLASHES - 1 && position != player.getCurrentMediaItemIndex()) {
                    return;
                }
                if ((times & 1) == 0) {
                    flashCardHolder.setStrokeWidth(8);
                }
                else {
                    flashCardHolder.setStrokeWidth(0);
                }

            }, FLASH_DELAY_MS * i);
        }
    }

    // Load cover image using Glide
    private void loadCoverImageWithGlide(SongBindingHolder viewHolder, Song song) {

        Bitmap coverImage = song.getCoverImage(context);

        if (coverImage != null) {
            Glide.with(context)
                    .load(coverImage)
                    .priority(Priority.LOW)
                    .error(R.drawable.ic_music_artwork)
                    .into(viewHolder.binding.itemArtworkView);
        }
        else {
            Glide.with(context)
                    .load(R.drawable.ic_music_artwork)
                    .priority(Priority.LOW)
                    .error(R.drawable.ic_music_artwork)
                    .into(viewHolder.binding.itemArtworkView);
        }
    }

    // transform Song to MediaItem
    public List<MediaItem> getMediaItems() {
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


    // View Holder
    public static class SongBindingHolder extends RecyclerView.ViewHolder {
        private final SongRowItemBinding binding;

        public SongBindingHolder(SongRowItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
        }
    }



    // receive filter songs and return search results
    @SuppressLint("NotifyDataSetChanged")
    public void filterSongs(List<Song> filteredList) {
        songs = filteredList;
        isSearchResultsClicked = true;
        notifyDataSetChanged();
    }

    public String getDuration(int totalDuration) {

        String totalDurationText = "";

        int oneHr = 1000 * 60 * 60;
        int oneMin = 1000 * 60;
        int oneSec = 1000;

        int hrs = totalDuration / oneHr;
        int mins = (totalDuration % oneHr) / oneMin;
        int secs = (totalDuration % oneMin) / oneSec;
        // at least display 1 sec
        secs = Math.max(secs, 1);

        if (hrs >= 1) {
            totalDurationText += String.format(Locale.getDefault(), "%02d:", hrs);
        }
        totalDurationText += String.format(Locale.getDefault(), "%02d:%02d", mins, secs);

        return totalDurationText;
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @Override
    public long getItemId(int position) {
        return songs.get(position).getId();
    }

    // size
    private String getSize(long bytes) {

        String fileSize;

        double kb = bytes / 1024.0;
        double mb = bytes / Math.pow(1024.0, 2);
        double gb = bytes / Math.pow(1024.0, 3);
        double tb = bytes / Math.pow(1024.0, 4);

        // the format
        DecimalFormat dec = new DecimalFormat("0.00");

        if (tb > 0.8) {
            fileSize = dec.format(tb) + " TB";
        } else if (gb > 0.8) {
            fileSize = dec.format(gb) + " GB";
        } else if (mb > 0.8) {
            fileSize = dec.format(mb) + " MB";
        } else if (kb > 0.8) {
            fileSize = dec.format(kb) + " KB";
        } else {
            fileSize = dec.format(bytes) + "Bytes";
        }

        return fileSize;
    }

}