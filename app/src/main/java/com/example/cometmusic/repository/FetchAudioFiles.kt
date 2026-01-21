package com.example.cometmusic.repository

import android.app.Application
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.net.toUri
import com.example.cometmusic.data.SharedData
import com.example.cometmusic.model.Song

object FetchAudioFiles {
    val TAG: String = FetchAudioFiles::class.java.simpleName

    private var appContext: Context? = null
    private val sharedData by lazy { SharedData(appContext!!) }

    fun init(application: Application) {
        if (appContext == null) {
            appContext = application.applicationContext
        }
    }

    private val _songs = mutableListOf<Song>()
    val songs: MutableList<Song> = _songs
    private var _savedMediaItemIndex = -1
    val savedMediaItemIndex: Int = _savedMediaItemIndex

    fun fetchSongs() {
        _savedMediaItemIndex = -1

        songs.clear()

        var selectedFolderUri: Uri? = null
        if (sharedData.chooseDir != null) {
            selectedFolderUri = Uri.parse(sharedData.chooseDir)
        }

        sharedData.deniedTimes = 0
        val mediaStoreUri: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        // define projection
        val projection: Array<String> = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DATA
        )

        var selection: String? = null

        var isScopeInSDCard = false

        if (selectedFolderUri != null) {
            // only music, music file is non-zero
            selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"

            // convert URL encoded to String
            val decodedPath = Uri.decode(selectedFolderUri.lastPathSegment)
            var removeColonPath: String?

            // only keep the path user selected
            val colonIndex = decodedPath.lastIndexOf(":")
            removeColonPath = decodedPath
            if (colonIndex != -1) {
                isScopeInSDCard = decodedPath.substring(0, colonIndex) != "primary"
                removeColonPath = decodedPath.substring(colonIndex + 1)
            }

            // add RELATIVE_PATH condition，find match path
            // can not distinguish sdcard or storage
            selection += " AND " + MediaStore.Audio.Media.RELATIVE_PATH + " LIKE '" + removeColonPath + "/%'"
        }
        // order
        val sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC"

        // get the songs
        try {
            appContext?.contentResolver
                ?.query(mediaStoreUri, projection, selection, null, sortOrder).use { cursor ->
                    // cache cursor indices
                    val idColumn = cursor!!.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val nameColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                    val durationColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                    val albumColumn =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)


                    var playlistPosition = 0
                    // clear the previous loaded before adding loading again
                    while (cursor.moveToNext()) {
                        // get the values of a column for a given audio file

                        val id = cursor.getLong(idColumn)
                        var name = cursor.getString(nameColumn)
                        val duration = cursor.getInt(durationColumn).toLong()
                        val size = cursor.getInt(sizeColumn)
                        val albumId = cursor.getLong(albumColumn)
                        val data = cursor.getString(dataColumn)

                        // check if user choose is sdcard or storage, and match the correct path
                        if (selectedFolderUri != null && isDataPathInSDCard(data) != isScopeInSDCard) continue

                        // song uri
                        val uri =
                            ContentUris.withAppendedId(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                id
                            )

                        // album artwork uri
                        val albumArtworkUri = ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(),
                            albumId
                        )

                        // remove file extension like .mp3 from the song
                        name = name.substringBeforeLast(".", name)
                        // song item
                        val song =
                            Song(playlistPosition, id, name, uri, albumArtworkUri, size, duration)

                        // add song item to song list
                        songs.add(song)


                        // add mediaItem to list
                        if (sharedData.songMediaId != null &&
                            sharedData.songMediaId == uri.toString()
                        ) {
                            _savedMediaItemIndex = songs.size - 1
                        }
                        ++playlistPosition
                    }
                }
        } catch (e: Exception) {
            // show Exception
            Log.e(TAG, "Error fetching songs: " + e.message, e)
            Toast.makeText(appContext, "Failed to fetch songs: " + e.message, Toast.LENGTH_SHORT)
                .show()
            e.printStackTrace()
        }
    }

    private fun isDataPathInSDCard(dataPath: String): Boolean {
        return !dataPath.contains("emulated")
    }
}