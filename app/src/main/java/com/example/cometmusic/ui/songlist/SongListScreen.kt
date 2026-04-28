package com.example.cometmusic.ui.songlist

import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.compose.AsyncImage
import com.example.cometmusic.R
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.model.Song
import com.example.cometmusic.service.PlaybackService
import com.example.cometmusic.ui.PlayerViewModel
import com.example.cometmusic.ui.theme.Gray
import com.example.cometmusic.ui.theme.LightGreen
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun SongListScreen(
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit
) {
    val context = LocalContext.current
    val sharedData = remember { SharedData(context) }
    val songs by playerViewModel.songsFlow.collectAsStateWithLifecycle()
    val player by playerViewModel.playerFlow.collectAsStateWithLifecycle()
    val isPlaying by playerViewModel.isPlayingFlow.collectAsStateWithLifecycle()
    val currentSongName by playerViewModel.currentSongNameFlow.collectAsStateWithLifecycle()
    val currentMediaId by playerViewModel.currentMediaIdFlow.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var isBackPressedOnce by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to current song once on initial load (when currentMediaId first becomes non-null).
    val initialScrollDone = remember { mutableStateOf(false) }
    LaunchedEffect(currentMediaId) {
        if (!initialScrollDone.value && currentMediaId != null) {
            listState.scrollToItem(playerViewModel.getPlayerCurrentIndex())
            initialScrollDone.value = true
        }
    }

    // Scroll to current song on explicit request (e.g., notification tap → onResume).
    LaunchedEffect(Unit) {
        playerViewModel.scrollToCurrentSong.collect {
            playerViewModel.songsFlow.first { it != null }
            val targetIndex = playerViewModel.getPlayerCurrentIndex()
            // Wait for Compose to recompose the LazyColumn with enough items.
            // Without this, scrollToItem(targetIndex) may run while the list still has the
            // old (smaller) song count and the index gets clamped to the old last item.
            snapshotFlow { listState.layoutInfo.totalItemsCount }
                .first { count -> count > targetIndex }
            listState.scrollToItem(targetIndex)
        }
    }

    val filteredSongs = remember(songs, searchQuery) {
        if (searchQuery.isEmpty()) songs ?: emptyList()
        else songs?.filter { it.title?.lowercase()?.contains(searchQuery.lowercase()) == true } ?: emptyList()
    }

    // Back press double-tap to exit
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else if (isBackPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            isBackPressedOnce = true
            Toast.makeText(context, R.string.click_double_back_message, Toast.LENGTH_SHORT).show()
            kotlinx.coroutines.MainScope().launch {
                kotlinx.coroutines.delay(2000)
                isBackPressedOnce = false
            }
        }
    }

    // Initialize player/session and fetch songs
    LaunchedEffect(Unit) {
        val sessionToken = playerViewModel.sessionTokenFlow.value
            ?: SessionToken(context, ComponentName(context, PlaybackService::class.java)).also {
                playerViewModel.setSessionToken(it)
            }

        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            try {
                val mediaController = future.get()
                playerViewModel.setPlayer(mediaController)
                playerViewModel.updateIsPlaying(mediaController.isPlaying)
                fetchAndSetupSongs(playerViewModel, sharedData)
                mediaController.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playerViewModel.updateIsPlaying(isPlaying)
                    }
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        playerViewModel.setCurrentSongNameFromItem(mediaItem)
                    }
                })
            } catch (e: Exception) {
                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
            }
        }, MoreExecutors.directExecutor())
    }

    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            val uriString = uri.toString()
            if (uriString == sharedData.chooseDir) {
                Toast.makeText(context, R.string.same_as_the_current_main_music_folder, Toast.LENGTH_SHORT).show()
            } else {
                sharedData.chooseDir = uriString
                reloadFolder(playerViewModel, sharedData)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (!isSearchActive) {
                TopAppBar(
                    title = {
                        val count = songs?.size ?: 0
                        Text(
                            text = if (count > 0) "CometMusic - $count" else "CometMusic",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = context.getString(R.string.toolbar_search))
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = context.getString(R.string.toolbar_more))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(context.getString(R.string.choose_main_music_folder)) },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    folderPickerLauncher.launch(null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(context.getString(R.string.reset_main_music_folder)) },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    reloadFolder(playerViewModel, sharedData)
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (songs?.isNotEmpty() == true) {
                val displayName = currentSongName
                    ?: player?.currentMediaItem?.mediaMetadata?.title?.toString()
                    ?: songs?.firstOrNull()?.title
                    ?: ""
                MiniPlayer(
                    songName = displayName,
                    isPlaying = isPlaying,
                    onPlayPause = { playerViewModel.clickPlayPauseBtn() },
                    onSkipPrev = { playerViewModel.skipToPreviousSong() },
                    onSkipNext = { playerViewModel.skipToNextSong() },
                    onOpenPlayer = onNavigateToPlayer
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isSearchActive) {
                SearchBar(
                    inputField = {
                        SearchBarDefaults.InputField(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSearch = {},
                            expanded = true,
                            onExpandedChange = { isSearchActive = it },
                            placeholder = { Text(context.getString(R.string.toolbar_search)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                    },
                    expanded = true,
                    onExpandedChange = {
                        isSearchActive = it
                        if (!it) searchQuery = ""
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {}
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (filteredSongs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(context.getString(R.string.no_songs))
                    }
                } else {
                    LazyColumnWithFastScroll(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        popupTextProvider = { index -> filteredSongs.getOrNull(index)?.title ?: "" }
                    ) {
                        itemsIndexed(filteredSongs, key = { _, song -> song.id }) { _, song ->
                            val isCurrent = currentMediaId == song.uri?.toString()
                            SongListItem(
                                song = song,
                                isCurrent = isCurrent,
                                onClick = {
                                    if (isSearchActive) {
                                        isSearchActive = false
                                        searchQuery = ""
                                        val targetPos = song.playlistPosition
                                        scope.launch { listState.scrollToItem(targetPos) }
                                    } else if (isCurrent) {
                                        if (!playerViewModel.getIsPlaying()) playerViewModel.clickPlayPauseBtn()
                                        onNavigateToPlayer()
                                    } else {
                                        player?.seekTo(song.playlistPosition, 0)
                                        player?.prepare()
                                        player?.play()
                                        // Update name and media ID immediately — onMediaItemTransition
                                        // is async (IPC) and may lag after setMediaItems+stop.
                                        playerViewModel.setCurrentSongNameFromTitle(song.title)
                                        playerViewModel.setCurrentMediaId(song.uri?.toString())
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
        border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF5AB35C)) else null,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.artworkUrl,
                contentDescription = null,
                error = painterResource(R.drawable.ic_music_artwork),
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = getReadableDuration(song.duration),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = getSize(song.size.toLong()),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(
    songName: String,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSkipPrev: () -> Unit,
    onSkipNext: () -> Unit,
    onOpenPlayer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(56.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpenPlayer)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Audiotrack,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = songName,
            modifier = Modifier.weight(1f).basicMarquee(),
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onSkipPrev) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Skip previous")
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }
        IconButton(onClick = onSkipNext) {
            Icon(Icons.Default.SkipNext, contentDescription = "Skip next")
        }
    }
}

@UnstableApi
private fun fetchAndSetupSongs(
    playerViewModel: PlayerViewModel,
    sharedData: SharedData,
    forceSetMediaItems: Boolean = false
) {
    if (playerViewModel.songsFlow.value == null) {
        playerViewModel.getSongs()
    }
    val songs = playerViewModel.songsFlow.value ?: return
    val savedIndex = playerViewModel.getSavedMediaItemIndex()

    val mediaItems = songs.map { song ->
        MediaItem.Builder()
            .setMediaId(song.uri.toString())
            .setMediaMetadata(MediaMetadata.Builder().setTitle(song.title).build())
            .build()
    }

    // forceSetMediaItems is true on folder changes: clearMediaItems() is async (IPC) so
    // isPlayerExistMediaItem() may still return true even after clearPlayer() was called.
    // Without the force flag the new folder's media items would never be loaded.
    if (forceSetMediaItems || !playerViewModel.isPlayerExistMediaItem()) {
        playerViewModel.setPlayerMediaItems(mediaItems)
        val savedPosition = if (playerViewModel.getSavedSongNotFound().value == false) {
            sharedData.songPosition
        } else 0
        playerViewModel.seekToSongIndexAndPosition(savedIndex, savedPosition * 1000L)
        // Set the song name only on first load / folder change. On navigation back the
        // ViewModel's _currentSongName already holds the correct value — overwriting it
        // here with savedIndex would revert to the old song.
        playerViewModel.setCurrentSongNameFromItem(mediaItems.getOrNull(savedIndex))
    } else {
        // Player already has media items (e.g., process restarted but service survived).
        // ViewModel StateFlows are fresh — sync them from the live player state so that
        // currentMediaId becomes non-null and the initial scroll can fire.
        playerViewModel.syncCurrentSongFromPlayer()
    }
    playerViewModel.preparePlayer()

    val mode = playerViewModel.getPlayerMode().value ?: 1
    playerViewModel.setPlayerMode(mode)
}

@UnstableApi
private fun reloadFolder(
    playerViewModel: PlayerViewModel,
    sharedData: SharedData
) {
    playerViewModel.saveCurrentSongStatus()
    playerViewModel.clearPlayer()
    playerViewModel.setSongs(null)
    val player = playerViewModel.playerFlow.value
    if (player != null) {
        // Force-reset media items because clearMediaItems() is async – the IPC may not have
        // completed by the time fetchAndSetupSongs runs, leaving the old playlist in place.
        fetchAndSetupSongs(playerViewModel, sharedData, forceSetMediaItems = true)
    }
    // Scroll to the current song after folder reload. triggerScrollToCurrentSong() is
    // collected in LaunchedEffect which waits for songs to be non-null before scrolling,
    // ensuring the LazyList has items at scroll time.
    playerViewModel.triggerScrollToCurrentSong()
}

private fun getReadableDuration(durationMs: Long): String {
    val totalSec = (durationMs / 1000).toInt()
    val oneHr = 60 * 60
    val oneMin = 60
    val hrs = totalSec / oneHr
    val mins = (totalSec % oneHr) / oneMin
    val secs = maxOf(totalSec % oneMin, 1)
    return buildString {
        if (hrs >= 1) append(String.format(Locale.getDefault(), "%02d:", hrs))
        append(String.format(Locale.getDefault(), "%02d:%02d", mins, secs))
    }
}

private fun getSize(bytes: Long): String {
    val dec = DecimalFormat("0.00")
    val kb = bytes / 1024.0
    val mb = bytes / 1024.0.pow(2)
    val gb = bytes / 1024.0.pow(3)
    return when {
        gb > 0.8 -> "${dec.format(gb)} GB"
        mb > 0.8 -> "${dec.format(mb)} MB"
        kb > 0.8 -> "${dec.format(kb)} KB"
        else -> "${dec.format(bytes)} Bytes"
    }
}

@Composable
private fun LazyColumnWithFastScroll(
    state: LazyListState,
    modifier: Modifier = Modifier,
    popupTextProvider: ((Int) -> String)? = null,
    content: LazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var containerHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    Box(modifier = modifier.onSizeChanged { containerHeightPx = it.height.toFloat() }) {
        LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
            content()
        }

        // Derive scroll metrics inside derivedStateOf so reads of layoutInfo don't cause
        // the whole composable to recompose on every scroll frame.
        val totalItems by remember { derivedStateOf { state.layoutInfo.totalItemsCount } }
        val visibleItems by remember { derivedStateOf { state.layoutInfo.visibleItemsInfo.size } }
        val scrollFraction by remember {
            derivedStateOf {
                val firstItemFraction = state.layoutInfo.visibleItemsInfo.firstOrNull()?.let { item ->
                    if (item.size > 0) state.firstVisibleItemScrollOffset.toFloat() / item.size else 0f
                } ?: 0f
                ((state.firstVisibleItemIndex + firstItemFraction) / (totalItems - visibleItems).coerceAtLeast(1))
                    .coerceIn(0f, 1f)
            }
        }

        if (totalItems > visibleItems && containerHeightPx > 0f) {
            val thumbHeightDp = if (isDragging) 52.dp else 60.dp
            val thumbWidthDp = if (isDragging) 8.dp else 12.dp
            val thumbColor = if (isDragging) LightGreen else Gray

            val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
            val availableHeightPx = (containerHeightPx - thumbHeightPx).coerceAtLeast(1f)
            val thumbOffsetPx by remember(isDragging) {
                derivedStateOf { if (isDragging) dragOffsetY else scrollFraction * availableHeightPx }
            }

            // Capture latest values to avoid stale closures inside pointerInput
            val latestAvailableHeightPx by rememberUpdatedState(availableHeightPx)
            val latestTotalItems by rememberUpdatedState(totalItems)
            val latestVisibleItems by rememberUpdatedState(visibleItems)
            val latestScrollFraction by rememberUpdatedState(scrollFraction)

            // Track (visual only, no touch handling – touches fall through to LazyColumn)
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .background(Color(0x266C6363), RoundedCornerShape(8.dp))
            )

            // Thumb (interactive)
            Box(
                modifier = Modifier
                    .width(thumbWidthDp)
                    .height(thumbHeightDp)
                    .align(Alignment.TopEnd)
                    .offset { IntOffset(0, thumbOffsetPx.toInt()) }
                    .background(thumbColor, RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                isDragging = true
                                dragOffsetY = latestScrollFraction * latestAvailableHeightPx
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onVerticalDrag = { _, delta ->
                                if (latestAvailableHeightPx > 0f) {
                                    dragOffsetY =
                                        (dragOffsetY + delta).coerceIn(0f, latestAvailableHeightPx)
                                    val fraction = dragOffsetY / latestAvailableHeightPx
                                    val targetIndex =
                                        (fraction * (latestTotalItems - latestVisibleItems))
                                            .toInt().coerceIn(0, latestTotalItems - 1)
                                    coroutineScope.launch { state.scrollToItem(targetIndex) }
                                }
                            }
                        )
                    }
            )

            // Popup text shown while dragging
            if (isDragging && popupTextProvider != null) {
                val fraction =
                    if (availableHeightPx > 0f) dragOffsetY / availableHeightPx else 0f
                val popupIndex = (fraction * (totalItems - visibleItems))
                    .toInt().coerceIn(0, totalItems - 1)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            IntOffset(
                                x = -with(density) { 72.dp.roundToPx() },
                                y = (thumbOffsetPx - with(density) { 8.dp.toPx() }).toInt()
                            )
                        }
                        .widthIn(max = 200.dp)
                        .wrapContentSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = popupTextProvider(popupIndex),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SongListPreview() {
    com.example.cometmusic.ui.theme.CometMusicTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(title = { Text("CometMusic - 5") })
            },
            bottomBar = {
                MiniPlayer(
                    songName = "preview song name",
                    isPlaying = false,
                    onPlayPause = {},
                    onSkipPrev = {},
                    onSkipNext = {},
                    onOpenPlayer = {}
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                repeat(8) { i ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Song $i",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}
