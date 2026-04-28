package com.example.cometmusic.model

import android.net.Uri

data class Song(
    val playlistPosition: Int,
    val id: Long = 0,
    val title: String? = null,
    val uri: Uri? = null,
    val artworkUrl: Uri? = null,
    val size: Int = 0,
    val duration: Long = 0)
