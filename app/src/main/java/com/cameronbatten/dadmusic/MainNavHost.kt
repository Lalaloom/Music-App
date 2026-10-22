package com.cameronbatten.dadmusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cameronbatten.dadmusic.ui.folders.FolderPickerScreen
import com.cameronbatten.dadmusic.ui.navigator.NavigatorScreen
import com.cameronbatten.dadmusic.ui.player.PlayerScreen

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "navigator") {
        composable("navigator") {
            NavigatorScreen(
                onOpenFolders = { navController.navigate("folders") },
                onSongPlaying = { navController.navigate("player") },
            )
        }
        composable("folders") {
            FolderPickerScreen(onDone = { navController.popBackStack() })
        }
        composable("player") {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
    }
}
