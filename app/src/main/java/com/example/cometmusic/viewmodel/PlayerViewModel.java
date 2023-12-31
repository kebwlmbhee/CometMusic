package com.example.cometmusic.viewmodel;

import android.app.Application;
import android.widget.Toast;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.example.cometmusic.R;
import com.example.cometmusic.model.FetchAudioFiles;
import com.example.cometmusic.model.SharedData;
import com.example.cometmusic.model.Song;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PlayerViewModel extends AndroidViewModel {

    public static final String playerIsNullException =
            "The player should be initialized by calling the setPlayer() method first";

    public static final String savedSongNotFoundVariableIsNull =
            "The savedSongNotFound variable should be initialized by calling the getSavedMediaItemIndex() method first";

    private final FetchAudioFiles fetchAudioFiles;
    private final SharedData sharedData;

    private final MutableLiveData<MediaController> player = new MutableLiveData<>();

    private final MutableLiveData<Integer> playerMode = new MutableLiveData<>();

    private final MutableLiveData<Integer> currentSecond = new MutableLiveData<>();
    private final MutableLiveData<Integer> durationSecond = new MutableLiveData<>();

    private final MutableLiveData<String> readableCurrentString = new MutableLiveData<>();
    private final MutableLiveData<String> readableDurationString = new MutableLiveData<>();

    private final MutableLiveData<String> currentSongName = new MutableLiveData<>();

    private final MutableLiveData<List<Song>> songs = new MutableLiveData<>();
    
    private final MutableLiveData<Boolean> savedSongNotFound = new MutableLiveData<>();

    private final MutableLiveData<List<MediaItem>> mediaItems = new MutableLiveData<>();
    private final MutableLiveData<SessionToken> sessionToken = new MutableLiveData<>();

    // for prod
    public PlayerViewModel(Application application) {
        super(application);
        sharedData = new SharedData(application.getApplicationContext());
        fetchAudioFiles = FetchAudioFiles.getInstance(getApplication().getApplicationContext());
    }

    // for test
    public PlayerViewModel(Application application, SharedData sharedData, FetchAudioFiles fetchAudioFiles) {
        super(application);
        this.sharedData = sharedData;
        this.fetchAudioFiles = fetchAudioFiles;
    }

    public void setPlayer(MediaController player) {
        this.player.setValue(player);
    }

    public MutableLiveData<MediaController> getPlayer() {
        // resume playerMode, if it is not exist, return defValue = 1

        if(player.getValue() == null) {
            throw new IllegalStateException(playerIsNullException);
        }

        return player;
    }

    public MutableLiveData<MediaController> getPlayerOrNull() {
        return player;
    }


    public void setPlayerMode(int mode) {

        playerMode.setValue(mode);

        switch (mode) {
            // repeat all
            case 1:
                Objects.requireNonNull(getPlayer().getValue()).setShuffleModeEnabled(false);
                getPlayer().getValue().setRepeatMode(Player.REPEAT_MODE_ALL);
                break;
            // repeat one
            case 2:
                Objects.requireNonNull(getPlayer().getValue()).setRepeatMode(Player.REPEAT_MODE_ONE);
                break;
            // shuffle
            case 3:
                Objects.requireNonNull(getPlayer().getValue()).setShuffleModeEnabled(true);
                getPlayer().getValue().setRepeatMode(Player.REPEAT_MODE_OFF);
                break;
        }
    }

    public MutableLiveData<Integer> getPlayerMode() {
        if(playerMode.getValue() == null) {
            playerMode.setValue(sharedData.getPlayerMode());
        }
        return playerMode;
    }

    public void clickRepeatButton() {

        switch (Objects.requireNonNull(getPlayerMode().getValue())) {
            //      1     ->      2
            // repeat all -> repeat one
            case 1:
                setPlayerMode(2);
                break;
            //      2     ->      3
            // repeat one -> shuffle mode
            case 2:
                setPlayerMode(3);
                break;
            //      3     ->      1
            // shuffle mode -> repeat all
            case 3:
                setPlayerMode(1);
                break;
        }
    }
    public void skipToNextSong() {
        if(!isPlayerExistMediaItem() || !Objects.requireNonNull(getPlayer().getValue()).hasNextMediaItem())
            return;

        getPlayer().getValue().seekToNext();
    }

    public void skipToPreviousSong() {
        if(!isPlayerExistMediaItem() || !Objects.requireNonNull(getPlayer().getValue()).hasPreviousMediaItem())
            return;

        getPlayer().getValue().seekToPrevious();
    }

    public int getPlayerCurrentIndex() {
        if(!isPlayerExistMediaItem())
            return 0;

        return Objects.requireNonNull(getPlayer().getValue()).getCurrentMediaItemIndex();
    }

    public MutableLiveData<Integer> getDurationSecond() {
        if(durationSecond.getValue() == null) {
            durationSecond.setValue(100);
        }
        return durationSecond;
    }

    public void setDurationSecond() {
        if(!isPlayerExistMediaItem())
            return;

        int currentIndex = Objects.requireNonNull(getPlayer().getValue()).getCurrentMediaItemIndex();

        durationSecond.setValue(
                millisecondToSecond(
                        Objects.requireNonNull(getSongs().getValue()).get(currentIndex)
                                .getDuration())
        );
        String durationReadableString = getReadableTime(Objects.requireNonNull(durationSecond.getValue()));
        readableDurationString.setValue(durationReadableString);
    }

    public MutableLiveData<Integer> getCurrentSecond() {
        if(currentSecond.getValue() == null) {
            currentSecond.setValue(-1);
        }
        return currentSecond;
    }

    public void setCurrentSecond() {
        if (isPlayerExistMediaItem()) {
            long position = Objects.requireNonNull(getPlayer().getValue()).getCurrentPosition();
            currentSecond.setValue(millisecondToSecond(position));
        } else {
            currentSecond.setValue(-1);
            Toast.makeText(this.getApplication(), R.string.can_not_find_current_second, Toast.LENGTH_SHORT).show();
        }

        String currentReadableSecond = getReadableTime(Objects.requireNonNull(currentSecond.getValue()));
        readableCurrentString.setValue(currentReadableSecond);
    }

    public MutableLiveData<String> getReadableCurrentString() {
        if(readableCurrentString.getValue() == null)
            setCurrentSecond();

        return readableCurrentString;
    }

    public MutableLiveData<String> getReadableDurationString() {
        if(readableDurationString.getValue() == null)
            setDurationSecond();

        return readableDurationString;
    }

    public void seekToPosition(long position) {
        Objects.requireNonNull(getPlayer().getValue()).seekTo(position);
    }

    public void seekToSongIndexAndPosition(int index, long position) {
        Objects.requireNonNull(getPlayer().getValue()).seekTo(index, position);
        setCurrentSecond();
    }

    public void setSecondAndStringWhenMoving(int progress) {
        currentSecond.setValue(progress);
        String currentReadableSecond = getReadableTime(Objects.requireNonNull(currentSecond.getValue()));
        readableCurrentString.setValue(currentReadableSecond);
    }

    public void setCurrentSongName() {
        if(isPlayerExistMediaItem()) {
            String songTitle = String.valueOf(Objects.requireNonNull((
                    Objects.requireNonNull(getPlayer().getValue()))
                            .getCurrentMediaItem()).mediaMetadata.title);
            currentSongName.setValue(songTitle);
        }
    }

    public MutableLiveData<String> getCurrentSongName() {
        if(currentSongName.getValue() == null)
            setCurrentSongName();
        return currentSongName;
    }

    public void clickPlayPauseBtn() {
        if(!isPlayerExistMediaItem())
            return;

        // from play to pause
        if (getIsPlaying()) {
            Objects.requireNonNull(getPlayer().getValue()).pause();
        }
        // from pause to play
        else {
            Objects.requireNonNull(getPlayer().getValue()).play();
        }
    }

    public boolean getIsPlaying() {
        return Objects.requireNonNull(getPlayer().getValue()).isPlaying();
    }

    public void setPlayerMediaItems(List<MediaItem> mediaItems) {
        this.mediaItems.setValue(mediaItems);
        Objects.requireNonNull(getPlayer().getValue()).setMediaItems(mediaItems);
    }

    public MutableLiveData<List<MediaItem>> getPlayerMediaItems() {
        return mediaItems;
    }

    public boolean isPlayerExistMediaItem() {
        return getPlayerOrNull().getValue() != null &&
                getPlayerOrNull().getValue().getCurrentMediaItem() != null;
    }

    public void clearPlayer() {
        if(getPlayerOrNull().getValue() == null)
            return;
        Objects.requireNonNull(getPlayer().getValue()).clearMediaItems();
        getPlayer().getValue().pause();

        getPlayer().getValue().stop();
    }

    public void preparePlayer() {
        Objects.requireNonNull(getPlayer().getValue()).prepare();
    }

    public MutableLiveData<List<Song>> getSongs() {
        if(songs.getValue() == null) {
            songs.setValue(fetchAudioFiles.getSongs());
        }
        return songs;
    }

    public void setSongs(List<Song> songs) {
        this.songs.setValue(songs);
    }

    public int getSavedMediaItemIndex() {
        int index = fetchAudioFiles.getSavedMediaItemIndex();
        // return the first song when the saved song is not found in the current path
        if(index == -1) {
            setSavedSongNotFound(true);
            return 0;
        }
        else {
            setSavedSongNotFound(false);
            return index;
        }
    }

    public void setSavedSongNotFound(boolean isFind) {
        savedSongNotFound.setValue(isFind);
    }
    
    public MutableLiveData<Boolean> getSavedSongNotFound() {
        if(savedSongNotFound.getValue() == null) {
            throw new IllegalStateException(savedSongNotFoundVariableIsNull);
        }
        return savedSongNotFound;
    }

    public MutableLiveData<SessionToken> getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(SessionToken sessionToken) {
        this.sessionToken.setValue(sessionToken);
    }

    public int millisecondToSecond(long duration) {
        final int oneSec = 1000;
        return (int) (duration / oneSec);
    }

    public String getReadableTime(int durationSecond) {
        String time = "";

        final int oneHr = 60 * 60;
        final int oneMin = 60;

        int hrs = durationSecond / oneHr;
        int mins = (durationSecond % oneHr) / oneMin;
        int secs = (durationSecond % oneMin);

        if (hrs >= 1) {
            time += String.format(Locale.getDefault(), "%02d:", hrs);
        }
        time += String.format(Locale.getDefault(), "%02d:%02d", mins, secs);

        return time;
    }

    public void saveCurrentSongStatus() {
        if(!isPlayerExistMediaItem())
            return;
        String currentSongId = Objects.requireNonNull(Objects.requireNonNull(
                getPlayer().getValue()).getCurrentMediaItem())
                .mediaId;
        int playingCurrentSecond = millisecondToSecond(getPlayer().getValue().getCurrentPosition());


        int playerMode = Objects.requireNonNull(getPlayerMode().getValue());

        sharedData.setSongMediaId(currentSongId);
        sharedData.setSongPosition(playingCurrentSecond);
        sharedData.setPlayerMode(playerMode);
    }

    @Override
    public void onCleared() {
        saveCurrentSongStatus();
        super.onCleared();
    }
}
