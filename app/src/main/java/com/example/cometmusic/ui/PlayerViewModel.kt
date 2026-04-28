package com.example.cometmusic.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.cometmusic.R
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.model.Song
import com.example.cometmusic.repository.FetchAudioFiles
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

@UnstableApi
class PlayerViewModel : AndroidViewModel {

    companion object {
        const val PLAYER_IS_NULL =
            "The player should be initialized by calling the setPlayer() method first"
        const val SAVED_SONG_NOT_FOUND_VARIABLE_IS_NULL =
            "The savedSongNotFound variable should be initialized by calling the getSavedMediaItemIndex() method first"
    }

    private val sharedData: SharedData

    // Called by viewModels() via reflection — IDE cannot see this usage.
    @Suppress("unused")
    constructor(application: Application) : super(application) {
        sharedData = SharedData(application.applicationContext)
    }

    // for test
    constructor(application: Application, sharedData: SharedData) : super(application) {
        this.sharedData = sharedData
    }

    private val _player = MutableStateFlow<MediaController?>(null)
    private val _playerMode = MutableStateFlow<Int?>(null)
    private val _currentSecond = MutableStateFlow(-1)
    private val _durationSecond = MutableStateFlow(100)
    private val _readableCurrentString = MutableStateFlow("")
    private val _readableDurationString = MutableStateFlow("")
    private val _currentSongName = MutableStateFlow<String?>(null)
    private val _currentMediaId = MutableStateFlow<String?>(null)
    private val _songs = MutableStateFlow<List<Song>?>(null)
    private val _savedSongNotFound = MutableStateFlow<Boolean?>(null)
    private val _mediaItems = MutableStateFlow<List<MediaItem>?>(null)
    private val _sessionToken = MutableStateFlow<SessionToken?>(null)
    private val _isPlaying = MutableStateFlow(false)
    // Emits a signal to scroll the song list to the current item (e.g., on app foreground).
    private val _scrollToCurrentSong = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    // Exposed StateFlows for Compose UI
    val playerFlow: StateFlow<MediaController?> = _player.asStateFlow()
    val isPlayingFlow: StateFlow<Boolean> = _isPlaying.asStateFlow()
    val currentSecondFlow: StateFlow<Int> = _currentSecond.asStateFlow()
    val durationSecondFlow: StateFlow<Int> = _durationSecond.asStateFlow()
    val currentSongNameFlow: StateFlow<String?> = _currentSongName.asStateFlow()
    val currentMediaIdFlow: StateFlow<String?> = _currentMediaId.asStateFlow()
    val songsFlow: StateFlow<List<Song>?> = _songs.asStateFlow()
    val playerModeFlow: StateFlow<Int?> = _playerMode.asStateFlow()
    val sessionTokenFlow: StateFlow<SessionToken?> = _sessionToken.asStateFlow()
    val scrollToCurrentSong: SharedFlow<Unit> = _scrollToCurrentSong.asSharedFlow()

    fun setPlayer(player: MediaController?) {
        _player.value = player
        if (player != null) {
            _isPlaying.value = player.isPlaying
        }
    }

    /** Throws [IllegalStateException] if player is not initialized. */
    fun getPlayer(): StateFlow<MediaController?> {
        if (_player.value == null) throw IllegalStateException(PLAYER_IS_NULL)
        return _player
    }

    fun getPlayerOrNull(): StateFlow<MediaController?> = _player

    fun setPlayerMode(mode: Int) {
        _playerMode.value = mode
        val p = _player.value ?: return
        when (mode) {
            1 -> {
                p.setShuffleModeEnabled(false)
                p.setRepeatMode(Player.REPEAT_MODE_ALL)
            }
            2 -> p.setRepeatMode(Player.REPEAT_MODE_ONE)
            3 -> {
                p.setShuffleModeEnabled(true)
                p.setRepeatMode(Player.REPEAT_MODE_OFF)
            }
        }
    }

    fun getPlayerMode(): StateFlow<Int?> {
        if (_playerMode.value == null) {
            _playerMode.value = sharedData.playerMode
        }
        return _playerMode
    }

    fun clickRepeatButton() {
        when (getPlayerMode().value) {
            1 -> setPlayerMode(2)
            2 -> setPlayerMode(3)
            3 -> setPlayerMode(1)
        }
    }

    fun skipToNextSong() {
        if (!isPlayerExistMediaItem() || _player.value?.hasNextMediaItem() != true) return
        _player.value?.seekToNext()
    }

    fun skipToPreviousSong() {
        if (!isPlayerExistMediaItem() || _player.value?.hasPreviousMediaItem() != true) return
        _player.value?.seekToPrevious()
    }

    fun getPlayerCurrentIndex(): Int {
        if (!isPlayerExistMediaItem()) return 0
        return _player.value?.currentMediaItemIndex ?: 0
    }

    fun getDurationSecond(): StateFlow<Int> = _durationSecond

    fun setDurationSecond() {
        if (!isPlayerExistMediaItem()) return
        val currentIndex = _player.value?.currentMediaItemIndex ?: return
        val duration = getSongs().value?.getOrNull(currentIndex)?.duration ?: return
        val durationSec = millisecondToSecond(duration)
        _durationSecond.value = durationSec
        _readableDurationString.value = getReadableTime(durationSec)
    }

    fun getCurrentSecond(): StateFlow<Int> = _currentSecond

    fun setCurrentSecond() {
        if (isPlayerExistMediaItem()) {
            val position = _player.value?.currentPosition ?: 0L
            _currentSecond.value = millisecondToSecond(position)
        } else {
            _currentSecond.value = -1
            Toast.makeText(getApplication(), R.string.can_not_find_current_second, Toast.LENGTH_SHORT).show()
        }
        _readableCurrentString.value = getReadableTime(_currentSecond.value)
    }

    fun getReadableCurrentString(): StateFlow<String> {
        if (_readableCurrentString.value.isEmpty()) setCurrentSecond()
        return _readableCurrentString
    }

    fun getReadableDurationString(): StateFlow<String> {
        if (_readableDurationString.value.isEmpty()) setDurationSecond()
        return _readableDurationString
    }

    fun seekToPosition(position: Long) {
        _player.value?.seekTo(position)
    }

    fun seekToSongIndexAndPosition(index: Int, position: Long) {
        _player.value?.seekTo(index, position)
        setCurrentSecond()
    }

    fun setSecondAndStringWhenMoving(progress: Int) {
        _currentSecond.value = progress
        _readableCurrentString.value = getReadableTime(progress)
    }

    fun setCurrentSongName() {
        if (isPlayerExistMediaItem()) {
            val title = _player.value?.currentMediaItem?.mediaMetadata?.title?.toString() ?: return
            _currentSongName.value = title
        }
    }

    /**
     * Sets [_currentSongName] directly from a [MediaItem] that was passed as a callback
     * parameter (e.g. [Player.Listener.onMediaItemTransition]).  This avoids relying on
     * [MediaController.currentMediaItem], whose client-side state may lag behind the session
     * state when the command was asynchronous (prepare / setMediaItems).
     */
    fun setCurrentSongNameFromItem(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: return
        _currentSongName.value = title
        _currentMediaId.value = mediaItem.mediaId
    }

    /**
     * Sets duration directly from a [Song] object (no IPC dependency on currentMediaItem).
     * Use this when currentMediaItem may not yet be mirrored from the PlaybackService.
     */
    fun setDurationSecondFromSong(song: Song) {
        val durationSec = millisecondToSecond(song.duration)
        _durationSecond.value = durationSec
        _readableDurationString.value = getReadableTime(durationSec)
    }

    /**
     * Sets [_currentSongName] directly from a title string.
     * Use this when the MediaItem is not yet available via IPC.
     */
    fun setCurrentSongNameFromTitle(title: String?) {
        if (!title.isNullOrEmpty()) {
            _currentSongName.value = title
        }
    }

    /** Updates the current media ID (used for green-border highlight in the song list). */
    fun setCurrentMediaId(mediaId: String?) {
        _currentMediaId.value = mediaId
    }

    /**
     * Syncs [_currentSongName] and [_currentMediaId] from the live player state.
     * Call this when the ViewModel is fresh (e.g., process restart) but the player already
     * has media items (service survived) so the normal init path was skipped.
     */
    fun syncCurrentSongFromPlayer() {
        val mediaItem = _player.value?.currentMediaItem ?: return
        _currentSongName.value = mediaItem.mediaMetadata?.title?.toString() ?: return
        _currentMediaId.value = mediaItem.mediaId
    }

    /** Signals the song list to scroll to the currently playing item (e.g., on app resume). */
    fun triggerScrollToCurrentSong() {
        _scrollToCurrentSong.tryEmit(Unit)
    }


    fun getCurrentSongName(): StateFlow<String?> {
        if (_currentSongName.value == null) setCurrentSongName()
        return _currentSongName
    }

    fun clickPlayPauseBtn() {
        if (!isPlayerExistMediaItem()) return
        if (getIsPlaying()) {
            _player.value?.pause()
        } else {
            _player.value?.play()
        }
    }

    fun getIsPlaying(): Boolean = _player.value?.isPlaying ?: false

    fun setPlayerMediaItems(mediaItems: List<MediaItem>?) {
        _mediaItems.value = mediaItems
        if (mediaItems != null) {
            _player.value?.setMediaItems(mediaItems)
        }
    }

    fun getPlayerMediaItems(): StateFlow<List<MediaItem>?> = _mediaItems

    fun isPlayerExistMediaItem(): Boolean {
        val p = _player.value ?: return false
        return p.currentMediaItem != null
    }

    fun clearPlayer() {
        val p = _player.value ?: return
        p.clearMediaItems()
        p.pause()
        p.stop()
    }

    fun preparePlayer() {
        val p = _player.value ?: throw IllegalStateException(PLAYER_IS_NULL)
        p.prepare()
    }

    fun getSongs(): StateFlow<List<Song>?> {
        if (_songs.value == null) {
            FetchAudioFiles.fetchSongs()
            _songs.value = FetchAudioFiles.songs
        }
        return _songs
    }

    fun setSongs(songs: List<Song>?) {
        _songs.value = songs
    }

    fun getSavedMediaItemIndex(): Int {
        val index = FetchAudioFiles.savedMediaItemIndex
        return if (index == -1) {
            setSavedSongNotFound(true)
            0
        } else {
            setSavedSongNotFound(false)
            index
        }
    }

    fun setSavedSongNotFound(isNotFound: Boolean) {
        _savedSongNotFound.value = isNotFound
    }

    /** Throws [IllegalStateException] if [getSavedMediaItemIndex] has not been called yet. */
    fun getSavedSongNotFound(): StateFlow<Boolean?> {
        if (_savedSongNotFound.value == null) throw IllegalStateException(SAVED_SONG_NOT_FOUND_VARIABLE_IS_NULL)
        return _savedSongNotFound
    }

    fun getSessionToken(): StateFlow<SessionToken?> = _sessionToken

    fun setSessionToken(sessionToken: SessionToken?) {
        _sessionToken.value = sessionToken
    }

    fun updateIsPlaying(isPlaying: Boolean) {
        _isPlaying.value = isPlaying
    }

    fun millisecondToSecond(duration: Long): Int = (duration / 1000).toInt()

    fun getReadableTime(durationSecond: Int): String {
        val oneHr = 60 * 60
        val oneMin = 60
        val hrs = durationSecond / oneHr
        val mins = (durationSecond % oneHr) / oneMin
        val secs = durationSecond % oneMin

        return buildString {
            if (hrs >= 1) append(String.format(Locale.getDefault(), "%02d:", hrs))
            append(String.format(Locale.getDefault(), "%02d:%02d", mins, secs))
        }
    }

    fun saveCurrentSongStatus() {
        if (!isPlayerExistMediaItem()) return
        val p = _player.value ?: return
        val currentSongId = p.currentMediaItem?.mediaId ?: return
        val playingCurrentSecond = millisecondToSecond(p.currentPosition)
        val mode = getPlayerMode().value ?: return

        sharedData.songMediaId = currentSongId
        sharedData.songPosition = playingCurrentSecond
        sharedData.playerMode = mode
    }

    public override fun onCleared() {
        saveCurrentSongStatus()
        super.onCleared()
    }
}
