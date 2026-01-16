package com.example.cometmusic.model

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri

data class Song(
    val playlistPosition: Int,
    val id: Long = 0,
    val title: String? = null,
    val uri: Uri? = null,
    val artworkUrl: Uri? = null,
    val size: Int = 0,
    val duration: Long = 0)

fun Song.getCoverImage(context: Context): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        retriever.embeddedPicture?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)
        }
    } finally {
        retriever.release()
    }

}