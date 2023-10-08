package com.example.cometmusic.factory.buildversion;

import android.os.Build;

public class BuildVersionProviderImpl implements BuildVersionProvider {
    @Override
    public boolean isTiramisuOrAbove() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU;
    }
}
