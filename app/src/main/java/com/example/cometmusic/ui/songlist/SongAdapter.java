package com.example.cometmusic.ui.songlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.databinding.DataBindingUtil;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.session.MediaController;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.example.cometmusic.R;
import com.example.cometmusic.databinding.SongRowItemBinding;
import com.example.cometmusic.model.Song;
import com.example.cometmusic.model.SongKt;
import com.example.cometmusic.utils.factory.GlideProvider;
import com.google.android.material.card.MaterialCardView;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int LOAD_COVER_IMAGE_AFTER_STOP_SCROLLING_MILLISECOND = 3000;

    public final static long DELAY_BETWEEN_ITEMS_MILLISECOND = 100L;

    public final static int STROKE_WIDTH = 8;


    private boolean isScrolling = false;

    private MaterialCardView currentCardHolder = null;
    private MaterialCardView previousCardHolder = null;

    private int currentIndex = -1;
    private int previousIndex = -1;

    private final Handler viewBorderHolder = new Handler();

    private Handler countTimeHandler = new Handler();

    private Handler backgroundHandler = new Handler();


    // members
    private final Context context;
    private List<Song> songs;
    private final LinearLayoutManager layoutManager;

    private final GlideProvider glideProvider;

    RecyclerView recyclerView;

    MediaController player;

    private PlayerControlListener playerControlListener;

    private SongBindingHolder previousViewHolder;

    boolean isSearch = false;

    // constructor for prod
    public SongAdapter(Context context, MediaController player, List<Song> songs,
                       RecyclerView recyclerView, GlideProvider glideProvider) {
        this.context = context;
        this.player = player;
        this.songs = songs;
        this.recyclerView = recyclerView;
        layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        this.glideProvider = glideProvider;
    }

    @VisibleForTesting
    public void setScrolling(boolean isScrolling) {
        this.isScrolling = isScrolling;
    }

    @VisibleForTesting
    public boolean getScrolling() {
        return isScrolling;
    }

    @VisibleForTesting
    public void setCurrentCardHolder(MaterialCardView cardHolder) {
        currentCardHolder = cardHolder;
    }

    @VisibleForTesting
    public MaterialCardView getCurrentCardHolder() {
        return currentCardHolder;
    }

    @VisibleForTesting
    public void setPreviousCardHolder(MaterialCardView cardHolder) {
        previousCardHolder = cardHolder;
    }

    @VisibleForTesting
    public MaterialCardView getPreviousCardHolder() {
        return previousCardHolder;
    }

    @VisibleForTesting
    public void setCurrentIndex(int index) {
        currentIndex = index;
    }
    @VisibleForTesting
    public int getCurrentIndex() {
        return currentIndex;
    }
    @VisibleForTesting
    public void setPreviousIndex(int index) {
        previousIndex = index;
    }

    @VisibleForTesting
    public int getPreviousIndex() {
        return previousIndex;
    }

    @VisibleForTesting
    public void setCountTimeHandler(Handler handler) {
        countTimeHandler = handler;
    }

    @VisibleForTesting
    public Handler getCountTimeHandler() {
        return countTimeHandler;
    }

    @VisibleForTesting
    public void setBackgroundHandler(Handler handler) {
        backgroundHandler = handler;
    }

    @VisibleForTesting
    public Handler getBackgroundHandler() {
        return backgroundHandler;
    }

    @VisibleForTesting
    public SongBindingHolder getPreviousViewHolder() {
        return previousViewHolder;
    }

    @VisibleForTesting
    public PlayerControlListener getPlayerControllerListener() {
        return playerControlListener;
    }

    public void startCountdownTimer() {
        isScrolling = true;
        // remove previous count time handler
        countTimeHandler.removeCallbacksAndMessages(null);
        countTimeHandler.postDelayed(() -> {
            isScrolling = false;
            showVisibleBitmaps();
        }, LOAD_COVER_IMAGE_AFTER_STOP_SCROLLING_MILLISECOND);
    }

    public void showVisibleBitmaps() {
        // if scrolling has stopped, trigger image loading
        // calculate start and end
        int start = Math.max(layoutManager.findFirstVisibleItemPosition(), 0);
        int end = layoutManager.findLastVisibleItemPosition();
        notifyItemsChangedOneByOne(start, end);
    }

    public void notifyItemsChangedOneByOne(final int start, final int end) {
        Runnable runnable = new Runnable() {
            private int currentPosition = start;

            public void run() {
                if (currentPosition <= end) {
                    notifyItemChanged(currentPosition);
                    ++currentPosition;
                    backgroundHandler.postDelayed(this, DELAY_BETWEEN_ITEMS_MILLISECOND);
                }
            }
        };

        // trigger runnable
        backgroundHandler.postDelayed(runnable, DELAY_BETWEEN_ITEMS_MILLISECOND);
    }

    public void clearImageCache() {
        glideProvider.getGlideInstance(context).clearMemory();
    }

    public interface PlayerControlListener {
        void onSameItemClicked();

        void collapseActionView();
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
        int durationSecond = millisecondToSecond(song.getDuration());
        viewHolder.binding.durationView.setText(getReadableDuration(durationSecond));
        viewHolder.binding.sizeView.setText(getSize(song.getSize()));

        // set and clear border, slow but stable
        if(player.getCurrentMediaItem() != null &&
                player.getCurrentMediaItem().mediaId.equals(String.valueOf(song.getUri()))) {
            if(cardView.getStrokeWidth() == 0) {
                cardView.setStrokeWidth(STROKE_WIDTH);
                clearViewBorder(false);
            }
        }
        else if(cardView.getStrokeWidth() == STROKE_WIDTH){
            cardView.setStrokeWidth(0);
        }

        // load image
        if (!isScrolling) {
            loadCoverImageWithGlide(viewHolder, song);
        }

        // when item click
        onCardViewClicked(position, song, cardView);
    }

    public void onCardViewClicked(int position, Song song, MaterialCardView cardView) {
        if (player == null) {
            return;
        }

        cardView.setOnClickListener(view -> {

            // when user click search result
            if (isSearch) {
                int targetPosition = song.getPlaylistPosition();
                playerControlListener.collapseActionView();
                // trigger close SearchView
                if (layoutManager != null) {
                    // scroll to make the clicked item appear at the top of the layout
                    layoutManager.scrollToPositionWithOffset(targetPosition, 0);
                }
                notifyItemChanged(targetPosition);
                waitForLayoutAndStartFlashing(targetPosition);
                isSearch = false;
            }
            // if the clicked viewHolder is the same as currentMetadata
            else if (player.getCurrentMediaItem() != null &&
                    player.getCurrentMediaItem().mediaId.equals(String.valueOf(song.getUri()))) {
                playerControlListener.onSameItemClicked();
            }
            // if a song is currently playing and the user switches to a different song
            else {
                player.seekTo(position, 0);
                setViewBorder(position);

                player.prepare();
                player.play();
            }
            playerControlListener.collapseActionView();
        });
    }

    // fast setting border
    public void setViewBorder(int viewPosition) {

        if(viewPosition >= 0 && viewPosition <= songs.size()) {

            // ensure the following code is posted to the main thread
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
                    currentCardHolder.setStrokeWidth(STROKE_WIDTH);
                }
            });
        }
    }

    public void clearViewBorder(boolean clearCurrent) {
        if(previousCardHolder != null && previousCardHolder.getStrokeWidth() != 0) {
            previousCardHolder.setStrokeWidth(0);
        }

        previousViewHolder = (SongBindingHolder) recyclerView.findViewHolderForAdapterPosition(previousIndex);
        if (previousViewHolder != null && previousViewHolder.binding.card.getStrokeWidth() != 0) {
            previousViewHolder.binding.card.setStrokeWidth(0);
        }

        // clear all border
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
                    flashCardHolder.setStrokeWidth(STROKE_WIDTH);
                }
                else {
                    flashCardHolder.setStrokeWidth(0);
                }

            }, FLASH_DELAY_MS * i);
        }
    }

    // Load cover image using Glide
    private void loadCoverImageWithGlide(SongBindingHolder viewHolder, Song song) {

        Bitmap coverImage = SongKt.getCoverImage(song, context);

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
        notifyDataSetChanged();
    }

    public void setIsSearch(boolean isSearch) {
        this.isSearch = isSearch;
    }

    public String getReadableDuration(int totalDuration) {

        String totalDurationText = "";

        int oneHr = 60 * 60;
        int oneMin = 60;

        int hrs = totalDuration / oneHr;
        int mins = (totalDuration % oneHr) / oneMin;
        int secs = (totalDuration % oneMin);
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

    public int millisecondToSecond(long duration) {
        final int oneSec = 1000;
        return (int) (duration / oneSec);
    }
}