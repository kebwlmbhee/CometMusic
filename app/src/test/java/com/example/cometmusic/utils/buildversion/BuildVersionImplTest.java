package com.example.cometmusic.utils.buildversion;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

@RunWith(AndroidJUnit4.class)
@Config(sdk = 33)
public class BuildVersionImplTest {
    private BuildVersionImpl buildVersionImpl;

    @Test
    public void BuildVersionImpl_IsTiramisuOrAbove_ReturnsTrue() {
        buildVersionImpl = new BuildVersionImpl(Build.VERSION_CODES.TIRAMISU);
        // call the method that should be tested
        assertTrue(buildVersionImpl.isTiramisuOrAbove());
    }

    @Test
    public void BuildVersionImpl_IsNotTiramisuOrAbove_ReturnsFalse() {
        buildVersionImpl = new BuildVersionImpl(Build.VERSION_CODES.S);
        assertFalse(buildVersionImpl.isTiramisuOrAbove());
    }
}
