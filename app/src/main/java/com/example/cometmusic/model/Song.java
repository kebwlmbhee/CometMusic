package com.example.cometmusic.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;

public class Song {
    // members
    int playlistPosition;
    long id;
    String title;
    Uri uri;
    Uri artworkUri;
    int size;
    int duration;

    private static final MediaMetadataRetriever sRetriever = new MediaMetadataRetriever();

    // constructor

    public Song(int playlistPosition, long id, String title, Uri uri, Uri artworkUri, int size, int duration) {
        this.playlistPosition = playlistPosition;
        this.id = id;
        this.title = title;
        this.uri = uri;
        this.artworkUri = artworkUri;
        this.size = size;
        this.duration = duration;
    }

    // getters

    public int getPlaylistPosition() {
        return playlistPosition;
    }

    public String getTitle() {
        return title;
    }

    public long getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    public Uri getArtworkUri() {
        return artworkUri;
    }

    public int getSize() {
        return size;
    }

    public int getDuration() {
        return duration;
    }

    // method to get the song cover image
    public Bitmap getCoverImage(Context context) {
        sRetriever.setDataSource(context, getUri());
        byte[] coverBytes = sRetriever.getEmbeddedPicture();

        if (coverBytes != null && coverBytes.length > 0) {
            return BitmapFactory.decodeByteArray(coverBytes, 0, coverBytes.length);
        }

        return null;
    }
}
