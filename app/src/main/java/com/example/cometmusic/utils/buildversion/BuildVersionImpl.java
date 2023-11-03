package com.example.cometmusic.utils.buildversion;

import android.os.Build;

public class BuildVersionImpl implements BuildVersionProvider {
    private int androidVersion;
    public BuildVersionImpl(int androidVersion) {
        this.androidVersion = androidVersion;
    }

    @Override
    public boolean isTiramisuOrAbove() {
        return androidVersion >= Build.VERSION_CODES.TIRAMISU;
    }
}
