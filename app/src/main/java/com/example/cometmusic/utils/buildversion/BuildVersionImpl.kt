package com.example.cometmusic.utils.buildversion

import android.os.Build

class BuildVersionImpl(private val androidVersion: Int) : BuildVersionProvider {
    override fun isTiramisuOrAbove(): Boolean = androidVersion >= Build.VERSION_CODES.TIRAMISU
}
