package com.example.cometmusic.ui.player

import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.ui.PlayerViewModel
import com.example.cometmusic.ui.theme.LightGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sharedData = remember { SharedData(context) }

    val player by playerViewModel.playerFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentSecond by playerViewModel.currentSecondFlow.collectAsStateWithLifecycle()
    val durationSecond by playerViewModel.durationSecondFlow.collectAsStateWithLifecycle()
    val readableCurrentString by playerViewModel.readableCurrentStringFlow.collectAsStateWithLifecycle()
    val readableDurationString by playerViewModel.readableDurationStringFlow.collectAsStateWithLifecycle()
    val currentSongName by playerViewModel.currentSongNameFlow.collectAsStateWithLifecycle()
    val playerMode by playerViewModel.playerModeFlow.collectAsStateWithLifecycle()
    val songs by playerViewModel.songsFlow.collectAsStateWithLifecycle()

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
                if (playerViewModel.isPlayerExistMediaItem()) {
                    playerViewModel.setDurationSecond()
                    playerViewModel.setCurrentSecond()
                    playerViewModel.setCurrentSongName()
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

    // Initialize player state when entering screen
    LaunchedEffect(player) {
        if (player != null && playerViewModel.isPlayerExistMediaItem()) {
            playerViewModel.setDurationSecond()
            playerViewModel.setCurrentSecond()
            playerViewModel.setCurrentSongName()
            val savedPosition = sharedData.songPosition * 1000L
            playerViewModel.seekToSongIndexAndPosition(playerViewModel.getPlayerCurrentIndex(), savedPosition)
        }
    }

    val currentSongArtworkUrl = remember(songs, player) {
        val idx = player?.currentMediaItemIndex ?: 0
        songs?.getOrNull(idx)?.artworkUrl
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        }

        // Artwork
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
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

        // Song name
        Text(
            text = currentSongName ?: "",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Seekbar area with cancel zone
        SeekBarSection(
            currentSecond = currentSecond,
            durationSecond = durationSecond,
            readableCurrentString = readableCurrentString,
            readableDurationString = readableDurationString,
            onProgressChange = { playerViewModel.setSecondAndStringWhenMoving(it) },
            onSeek = { playerViewModel.seekToPosition(it) },
            onCancelled = { playerViewModel.setCurrentSecond() }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Control buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Player mode button
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

        Spacer(modifier = Modifier.height(24.dp))
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
    var isCancelHovered by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }

    // Sync slider with player progress when not dragging
    LaunchedEffect(currentSecond, isDragging) {
        if (!isDragging) sliderValue = currentSecond.toFloat()
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Cancel icon shown during drag
        if (isDragging) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(onDragEnd = {}) { _, _ -> }
                    },
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        isCancelHovered = true
                        onCancelled()
                        isDragging = false
                        isCancelHovered = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancel seek",
                        tint = if (isCancelHovered) Color.Red else LightGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(48.dp))
        }

        Slider(
            value = sliderValue,
            onValueChange = { value ->
                isDragging = true
                sliderValue = value
                onProgressChange(value.toInt())
            },
            onValueChangeFinished = {
                if (!isCancelHovered) {
                    onSeek(sliderValue.toLong() * 1000L)
                }
                isDragging = false
                isCancelHovered = false
            },
            valueRange = 0f..maxOf(durationSecond.toFloat(), 1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = readableCurrentString, style = MaterialTheme.typography.labelSmall)
            Text(text = readableDurationString, style = MaterialTheme.typography.labelSmall)
        }
    }
}
