package com.example.cometmusic

import android.app.Application
import com.example.cometmusic.repository.FetchAudioFiles

class CometMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()

        FetchAudioFiles.init(this)
    }
}