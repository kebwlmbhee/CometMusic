package com.example.cometmusic.factory.buildversion;

import android.os.Build;

public class BuildVersionImpl implements BuildVersionProvider {
    @Override
    public boolean isTiramisuOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
