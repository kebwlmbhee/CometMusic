package com.example.cometmusic.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.media3.common.util.UnstableApi
import com.example.cometmusic.ui.permission.CheckPermissionScreen
import com.example.cometmusic.ui.player.PlayerScreen
import com.example.cometmusic.ui.songlist.SongListScreen

private object Routes {
    const val PERMISSION = "permission"
    const val SONGLIST = "songlist"
    const val PLAYER = "player"
}

@UnstableApi
@Composable
fun AppNavHost(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.PERMISSION, modifier = Modifier.fillMaxSize()) {
        composable(Routes.PERMISSION) {
            CheckPermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Routes.SONGLIST) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SONGLIST) {
            SongListScreen(
                playerViewModel = playerViewModel,
                onNavigateToPlayer = {
                    navController.navigate(Routes.PLAYER)
                }
            )
        }
        composable(Routes.PLAYER) {
            PlayerScreen(
                playerViewModel = playerViewModel,
                onNavigateBack = {
                    navController.navigateUp()
                }
            )
        }
    }
}
