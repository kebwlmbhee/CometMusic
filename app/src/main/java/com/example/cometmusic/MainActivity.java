package com.example.cometmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.media3.common.util.UnstableApi;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.example.cometmusic.viewmodel.PlayerViewModel;

@UnstableApi public class MainActivity extends AppCompatActivity {

    public static final String REQUEST_CLOSE_MAIN_ACTIVITY_ACTION = "close_main_activity_action";
    public static final String REQUEST_CHANGE_PLAYER_MODE_ACTION = "change_player_mode_action";

    public static final String REQUEST_UPDATE_PLAY_PAUSE_BUTTON_ACTION = "update_play_pause_button_action";
    public static final String CHANGE_PLAYER_MODE_KEY = "change_player_mode";

    private static final String TAG = "MyTag";

    private final boolean DEV_MODE = false;
    private CloseActivityActionReceiver closeActivityActionReceiver;

    private ChangePlayerModeActionReceiver changePlayerModeActionReceiver;

    private UpdatePlayPauseButtonActionReceiver updatePlayPauseButtonActionReceiver;

    private PlayerViewModel playerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);

        if (DEV_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyDialog()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
    }

    // respond to the close button in the notification
    // finish activity
    public class CloseActivityActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(REQUEST_CLOSE_MAIN_ACTIVITY_ACTION)) {
                finish();
            }
        }
    }

    public class ChangePlayerModeActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(REQUEST_CHANGE_PLAYER_MODE_ACTION)) {
                int mode = intent.getIntExtra(CHANGE_PLAYER_MODE_KEY, 1);
                playerViewModel.setPlayerMode(mode);
            }
        }
    }

    public class UpdatePlayPauseButtonActionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(REQUEST_UPDATE_PLAY_PAUSE_BUTTON_ACTION)) {
                playerViewModel.checkIsPlaying();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // register broadcastReceiver
        if(closeActivityActionReceiver == null)
            closeActivityActionReceiver = new CloseActivityActionReceiver();
        IntentFilter closeActivityActionFilter = new IntentFilter(REQUEST_CLOSE_MAIN_ACTIVITY_ACTION);
        registerReceiver(closeActivityActionReceiver, closeActivityActionFilter);

        if(changePlayerModeActionReceiver == null)
            changePlayerModeActionReceiver = new ChangePlayerModeActionReceiver();
        IntentFilter changePlayerModeActionFilter = new IntentFilter(REQUEST_CHANGE_PLAYER_MODE_ACTION);
        registerReceiver(changePlayerModeActionReceiver, changePlayerModeActionFilter);

        if(updatePlayPauseButtonActionReceiver == null)
            updatePlayPauseButtonActionReceiver = new UpdatePlayPauseButtonActionReceiver();
        IntentFilter updatePlayPauseButtonActionFilter = new IntentFilter(REQUEST_UPDATE_PLAY_PAUSE_BUTTON_ACTION);
        registerReceiver(updatePlayPauseButtonActionReceiver, updatePlayPauseButtonActionFilter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }


    @Override
    protected void onStop() {
        // avoid memory leak
        if(closeActivityActionReceiver != null) {
            unregisterReceiver(closeActivityActionReceiver);
            closeActivityActionReceiver = null;
        }
        if(changePlayerModeActionReceiver != null) {
            unregisterReceiver(changePlayerModeActionReceiver);
            changePlayerModeActionReceiver = null;
        }
        if(updatePlayPauseButtonActionReceiver != null) {
            unregisterReceiver(updatePlayPauseButtonActionReceiver);
            updatePlayPauseButtonActionReceiver = null;
        }
        Log.d(TAG, "onStop: ");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        if(playerViewModel.isPlayerExistMediaItem())
//            Objects.requireNonNull(playerViewModel.getPlayer().getValue()).release();
        playerViewModel = null;
        Log.d(TAG, "onDestroy: MainActivity");
        super.onDestroy();
    }
}
