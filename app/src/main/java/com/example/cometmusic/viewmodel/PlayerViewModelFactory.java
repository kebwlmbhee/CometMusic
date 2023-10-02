package com.example.cometmusic.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.cometmusic.model.SharedData;

public class PlayerViewModelFactory implements ViewModelProvider.Factory {
    private final SharedData sharedData;
    private final Application application;

    public PlayerViewModelFactory(Application application, SharedData sharedData) {
        this.application = application;
        this.sharedData = sharedData;
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(PlayerViewModel.class)) {
            // create PlayerViewModel instance and pass the parameters
            return (T) new PlayerViewModel(application, sharedData);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
