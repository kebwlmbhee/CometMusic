package com.example.cometmusic.ui.player

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import com.example.cometmusic.R
import com.example.cometmusic.ui.PlayerViewModel
import com.example.cometmusic.ui.theme.LightGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.tooling.preview.Preview
import com.example.cometmusic.ui.theme.CometMusicTheme

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val player by playerViewModel.playerFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentSecond by playerViewModel.currentSecondFlow.collectAsStateWithLifecycle()
    val durationSecond by playerViewModel.durationSecondFlow.collectAsStateWithLifecycle()
    val currentSongName by playerViewModel.currentSongNameFlow.collectAsStateWithLifecycle()
    val playerMode by playerViewModel.playerModeFlow.collectAsStateWithLifecycle()
    val songs by playerViewModel.songsFlow.collectAsStateWithLifecycle()

    val idx = player?.currentMediaItemIndex ?: 0
    val currentSong = songs?.getOrNull(idx)
    val displaySongName = currentSongName ?: currentSong?.title ?: ""
    val displayDuration = remember(durationSecond, currentSong) {
        val sec = if (durationSecond > 0) durationSecond
                  else ((currentSong?.duration ?: 0L) / 1000).toInt()
        playerViewModel.getReadableTime(sec)
    }
    val displayCurrent = remember(currentSecond) {
        playerViewModel.getReadableTime(maxOf(currentSecond, 0))
    }

    // Progress update ticker
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                playerViewModel.setCurrentSecond()
                val now = SystemClock.uptimeMillis()
                delay(500 - now % 500)
            }
        }
    }

    // Player listener for song transitions
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                playerViewModel.updateIsPlaying(playing)
            }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // Use the passed mediaItem directly — avoids async currentMediaItem lag.
                playerViewModel.setCurrentSongNameFromItem(mediaItem)
                if (playerViewModel.isPlayerExistMediaItem()) {
                    playerViewModel.setDurationSecond()
                    playerViewModel.setCurrentSecond()
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Toast.makeText(context, error.toString(), Toast.LENGTH_SHORT).show()
            }
        }
        player?.addListener(listener)
        onDispose {
            player?.removeListener(listener)
            playerViewModel.saveCurrentSongStatus()
        }
    }

    // Initialize player state when entering screen.
    // Avoid isPlayerExistMediaItem() here — it reads currentMediaItem which is an
    // async IPC mirror and may be null right after navigation even though the player
    // is already playing.  Use songs list + currentMediaItemIndex instead.
    LaunchedEffect(player) {
        val p = player ?: return@LaunchedEffect
        val idx = p.currentMediaItemIndex
        val songList = playerViewModel.songsFlow.value
        val song = songList?.getOrNull(idx)

        // Set duration from songs list (no IPC dependency)
        if (song != null) {
            playerViewModel.setDurationSecondFromSong(song)
        } else {
            playerViewModel.setDurationSecond()
        }
        // Set current position directly from player
        playerViewModel.setCurrentSecond()
        // Set song name: ViewModel may already have it from MiniPlayer's onMediaItemTransition;
        // only override if still null.
        if (playerViewModel.currentSongNameFlow.value == null && song != null) {
            playerViewModel.setCurrentSongNameFromTitle(song.title)
        }
    }

    val currentSongArtworkUrl = remember(songs, player) {
        val idx = player?.currentMediaItemIndex ?: 0
        songs?.getOrNull(idx)?.artworkUrl
    }

    // Match the original ConstraintLayout proportions: head 10% / artwork 40% /
    // song-name 20% / seekbar 20% / controls 10%
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Top bar – 10%
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        // Artwork – 40%
        Box(
            modifier = Modifier.fillMaxWidth().weight(4f),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = currentSongArtworkUrl,
                contentDescription = context.getString(R.string.song_artwork),
                contentScale = ContentScale.Fit,
                error = painterResource(R.drawable.ic_music_artwork),
                modifier = Modifier.fillMaxSize().padding(4.dp)
            )
        }

        // Song name – 10%
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = displaySongName,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth().basicMarquee()
            )
        }

        // Seekbar area – 20%
        Box(
            modifier = Modifier.fillMaxWidth().weight(2f),
            contentAlignment = Alignment.Center
        ) {
            SeekBarSection(
                currentSecond = currentSecond,
                durationSecond = durationSecond,
                readableCurrentString = displayCurrent,
                readableDurationString = displayDuration,
                onProgressChange = { playerViewModel.setSecondAndStringWhenMoving(it) },
                onSeek = { playerViewModel.seekToPosition(it) },
                onCancelled = { playerViewModel.setCurrentSecond() }
            )
        }

        // Control buttons – 20%
        Row(
            modifier = Modifier.fillMaxWidth().weight(2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playerViewModel.clickRepeatButton() }) {
                val modeIcon = when (playerMode) {
                    2 -> painterResource(R.drawable.ic_repeat_one)
                    3 -> painterResource(R.drawable.ic_shuffle)
                    else -> painterResource(R.drawable.ic_repeat_all)
                }
                Icon(
                    painter = modeIcon,
                    contentDescription = "Player mode",
                    modifier = Modifier.size(28.dp)
                )
            }
            IconButton(onClick = { playerViewModel.skipToPreviousSong() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Skip previous", modifier = Modifier.size(36.dp))
            }
            IconButton(
                onClick = { playerViewModel.clickPlayPauseBtn() },
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp)
                )
            }
            IconButton(onClick = { playerViewModel.skipToNextSong() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Skip next", modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.QueueMusic, contentDescription = "Playlist", modifier = Modifier.size(28.dp))
            }
        }
    }
    }
}

@Composable
private fun SeekBarSection(
    currentSecond: Int,
    durationSecond: Int,
    readableCurrentString: String,
    readableDurationString: String,
    onProgressChange: (Int) -> Unit,
    onSeek: (Long) -> Unit,
    onCancelled: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var isInCancelZone by remember { mutableStateOf(false) }
    var dragDeltaY by remember { mutableFloatStateOf(0f) }
    var sliderValue by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    // Threshold = half the cancel-box height (24 dp) + half the slider track (~22 dp)
    // The cancel box always occupies space (alpha-only show/hide keeps layout stable),
    // so the X icon is always exactly 24 dp above the slider top.
    val cancelThresholdPx = with(density) { 46.dp.toPx() }

    // Sync slider with player progress when not dragging
    LaunchedEffect(currentSecond, isDragging) {
        if (!isDragging) sliderValue = currentSecond.toFloat()
    }

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        // Cancel icon – always reserves 48 dp so the slider never shifts when dragging starts.
        // Alpha-only visibility keeps layout stable so the threshold stays accurate.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .alpha(if (isDragging) 1f else 0f),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = "Drag up to cancel",
                tint = if (isInCancelZone) Color.Red else LightGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        // Outer Box intercepts pointer Y-movement (Initial pass) so we can detect
        // an upward drag into the cancel zone without interfering with the Slider's
        // horizontal drag handling.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            event.changes.firstOrNull()?.let { change ->
                                when {
                                    change.pressed && !change.previousPressed -> {
                                        // New press – reset Y tracking
                                        dragDeltaY = 0f
                                        isInCancelZone = false
                                    }
                                    change.pressed && isDragging -> {
                                        dragDeltaY += change.positionChange().y
                                        isInCancelZone = dragDeltaY < -cancelThresholdPx
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Slider(
                value = sliderValue,
                onValueChange = { value ->
                    if (!isDragging) {
                        dragDeltaY = 0f
                        isInCancelZone = false
                    }
                    isDragging = true
                    sliderValue = value
                    onProgressChange(value.toInt())
                },
                onValueChangeFinished = {
                    if (isInCancelZone) {
                        onCancelled()
                    } else {
                        onSeek(sliderValue.toLong() * 1000L)
                    }
                    isDragging = false
                    isInCancelZone = false
                    dragDeltaY = 0f
                },
                valueRange = 0f..maxOf(durationSecond.toFloat(), 1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = if (isSystemInDarkTheme()) Color(0xFF3A3A3A) else Color(0xFFC0C0C0),
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = readableCurrentString, style = MaterialTheme.typography.labelSmall)
            Text(text = readableDurationString, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Preview — shows static layout without a real player / ViewModel
// ---------------------------------------------------------------------------

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun PlayerScreenPreview() {
    CometMusicTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar – 10%
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null)
                }
            }
            // Artwork – 40%
            Box(
                modifier = Modifier.fillMaxWidth().weight(4f),
                contentAlignment = Alignment.Center
            ) {
                Text("[ Artwork ]", style = MaterialTheme.typography.bodyLarge)
            }
            // Song name – 20%
            Box(
                modifier = Modifier.fillMaxWidth().weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sample Song Title",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // Seekbar – 20%
            Box(
                modifier = Modifier.fillMaxWidth().weight(2f),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
                    Slider(value = 45f, onValueChange = {}, valueRange = 0f..180f, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("0:45", style = MaterialTheme.typography.labelSmall)
                        Text("3:00", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            // Controls – 10%
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(28.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(36.dp)) }
                IconButton(onClick = {}, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(40.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(36.dp)) }
                IconButton(onClick = {}) { Icon(Icons.Default.QueueMusic, contentDescription = null, modifier = Modifier.size(28.dp)) }
            }
        }
    }
}
