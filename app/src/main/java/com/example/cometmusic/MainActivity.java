package com.example.cometmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
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

    private CloseActivityActionReceiver closeActivityActionReceiver;

    private ChangePlayerModeActionReceiver changePlayerModeActionReceiver;

    private PlayerViewModel playerViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);
        setContentView(R.layout.activity_main);

        playerViewModel = new ViewModelProvider(this).get(PlayerViewModel.class);
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

    // register broadcastReceiver
    @Override
    protected void onResume() {
        super.onResume();

        closeActivityActionReceiver = new CloseActivityActionReceiver();
        IntentFilter closeActivityActionFilter = new IntentFilter(REQUEST_CLOSE_MAIN_ACTIVITY_ACTION);
        registerReceiver(closeActivityActionReceiver, closeActivityActionFilter, RECEIVER_NOT_EXPORTED);

        changePlayerModeActionReceiver = new ChangePlayerModeActionReceiver();
        IntentFilter changePlayerModeActionFilter = new IntentFilter(REQUEST_CHANGE_PLAYER_MODE_ACTION);
        registerReceiver(changePlayerModeActionReceiver, changePlayerModeActionFilter, RECEIVER_NOT_EXPORTED);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    // avoid memory leak
    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(closeActivityActionReceiver);
        unregisterReceiver(changePlayerModeActionReceiver);
    }

    @Override
    protected void onDestroy() {
        playerViewModel = null;
        super.onDestroy();
    }
}
