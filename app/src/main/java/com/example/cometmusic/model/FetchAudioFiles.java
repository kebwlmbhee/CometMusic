package com.example.cometmusic.model;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class FetchAudioFiles {

    private static final String TAG = "MyTag";

    SharedData sharedData;
    List<Song> songs;
    // weak reference
    private final WeakReference<Context> contextRef;

    private int savedMediaItemIndex = -1;

    private static FetchAudioFiles instance;

    private FetchAudioFiles(Context context) {
        contextRef = new WeakReference<>(context.getApplicationContext());
        sharedData = new SharedData(context);
    }

    public static synchronized FetchAudioFiles getInstance(Context context) {
        if (instance == null) {
            instance = new FetchAudioFiles(context);
        }
        return instance;
    }

    private void fetchSongs() {

        savedMediaItemIndex = -1;
        Context context = contextRef.get();
        if(context == null)
            return;

        // define a list to carry songs
        songs = new ArrayList<>();

        Uri mediaStoreUri;

        Uri selectedFolderUri = null;
        if (sharedData.getChooseDir() != null) {
            selectedFolderUri = Uri.parse(sharedData.getChooseDir());
        }

        sharedData.setDeniedTimes(0);
        mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);

        // define projection
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.RELATIVE_PATH,
                MediaStore.Audio.Media.DATA
        };

        String selection = null;

        boolean isScopeInSDCard = false;

        if (selectedFolderUri != null) {
            // only music, music file is non-zero
            selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";

            // convert URL encoded to String
            String decodedPath = Uri.decode(selectedFolderUri.getLastPathSegment());
            String removeColonPath;

            // only keep the path user selected
            int colonIndex = decodedPath.lastIndexOf(":");
            removeColonPath = decodedPath;
            if (colonIndex != -1) {
                isScopeInSDCard = !decodedPath.substring(0, colonIndex).equals("primary");
                removeColonPath = decodedPath.substring(colonIndex + 1);
            }

            // add RELATIVE_PATH condition，find match path
            // can not distinguish sdcard or storage
            selection += " AND " + MediaStore.Audio.Media.RELATIVE_PATH + " LIKE '" + removeColonPath + "/%'";

        }
        // order
        String sortOrder = MediaStore.Audio.Media.DATE_ADDED + " DESC";

        // get the songs
        try (Cursor cursor = context.getContentResolver().query(mediaStoreUri, projection, selection, null, sortOrder)) {
            // cache cursor indices
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);


            int playlistPosition = 0;
            // clear the previous loaded before adding loading again
            while (cursor.moveToNext()) {

                // get the values of a column for a given audio file
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                int duration = cursor.getInt(durationColumn);
                int size = cursor.getInt(sizeColumn);
                long albumId = cursor.getLong(albumColumn);
                String data = cursor.getString(dataColumn);

                // check if user choose is sdcard or storage, and match the correct path
                if (selectedFolderUri != null && isDataPathInSDCard(data) != isScopeInSDCard)
                    continue;

                // song uri
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                // album artwork uri
                Uri albumArtworkUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);

                // remove file extension like .mp3 from the song
                name = name.substring(0, name.lastIndexOf("."));
                // song item
                Song song = new Song(playlistPosition, id, name, uri, albumArtworkUri, size, duration);

                // add song item to song list
                songs.add(song);


                // add mediaItem to list
                if (sharedData.getSongMediaId() != null &&
                        sharedData.getSongMediaId().equals(String.valueOf(uri))) {
                    savedMediaItemIndex = songs.size() - 1;
                }
                ++playlistPosition;
            }

            // display songs
        } catch (Exception e) {
            // show Exception
            Log.e(TAG, "Error fetching songs: " + e.getMessage(), e);
            Toast.makeText(context, "Failed to fetch songs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private boolean isDataPathInSDCard(String dataPath) {
        return !dataPath.contains("emulated");
    }

    public int getSavedMediaItemIndex() {
        return savedMediaItemIndex;
    }

    public List<Song> getSongs() {
        fetchSongs();
        return songs;
    }
}
