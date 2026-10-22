package com.cameronbatten.dadmusic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.ui.folders.FolderPickerScreen
import com.cameronbatten.dadmusic.ui.navigator.NavigatorScreen
import com.cameronbatten.dadmusic.ui.player.PlayerScreen
import com.cameronbatten.dadmusic.ui.settings.SettingsScreen
import com.cameronbatten.dadmusic.ui.zen.ZenPlayerScreen

@Composable
fun MainNavHost() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "navigator") {
        composable("navigator") {
            NavigatorScreen(
                onOpenFolders = { navController.navigate("folders") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenPlayer = { fileType ->
                    val route = when (fileType) {
                        FileType.AUDIO -> "zen"
                        FileType.VIDEO -> "player"
                    }
                    navController.navigate(route)
                },
            )
        }
        composable("folders") {
            FolderPickerScreen(onDone = { navController.popBackStack() })
        }
        composable("player") {
            PlayerScreen(onBack = { navController.popBackStack() })
        }
        composable("zen") {
            ZenPlayerScreen(onBack = { navController.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
