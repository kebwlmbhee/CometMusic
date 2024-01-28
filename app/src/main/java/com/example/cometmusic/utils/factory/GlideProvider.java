package com.example.cometmusic.utils.factory;

import android.content.Context;

import com.bumptech.glide.Glide;

public class GlideProvider {
    public Glide getGlideInstance(Context context) {
        return Glide.get(context);
    }
}
