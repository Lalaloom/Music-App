package com.cameronbatten.dadmusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cameronbatten.dadmusic.data.db.FileType
import com.cameronbatten.dadmusic.ui.admin.AdminScreen
import com.cameronbatten.dadmusic.ui.folders.FolderPickerScreen
import com.cameronbatten.dadmusic.ui.lyricseditor.LyricsEditorScreen
import com.cameronbatten.dadmusic.ui.navigator.NavigatorScreen
import com.cameronbatten.dadmusic.ui.onboarding.OnboardingHost
import com.cameronbatten.dadmusic.ui.player.PlayerScreen
import com.cameronbatten.dadmusic.ui.settings.SettingsScreen
import com.cameronbatten.dadmusic.ui.zen.ZenPlayerScreen

@Composable
fun MainNavHost(appStartViewModel: AppStartViewModel = hiltViewModel()) {
    val startState by appStartViewModel.state.collectAsStateWithLifecycle()

    when (startState) {
        AppStartState.Unknown -> {
            // Brief splash while the onboarding-completion preference resolves.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        AppStartState.Onboarding, AppStartState.Ready -> {
            val navController = rememberNavController()
            val startRoute = if (startState == AppStartState.Onboarding) "onboarding" else "navigator"
            NavHost(navController, startDestination = startRoute) {
                composable("onboarding") {
                    OnboardingHost(
                        onFinish = {
                            navController.navigate("navigator") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        },
                    )
                }
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
                    ZenPlayerScreen(
                        onBack = { navController.popBackStack() },
                        onEditLyrics = { songId -> navController.navigate("editor/$songId") },
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        onOpenAdmin = { navController.navigate("admin") },
                    )
                }
                composable("admin") {
                    AdminScreen(
                        onBack = { navController.popBackStack() },
                        onResetComplete = {
                            navController.navigate("onboarding") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
                composable(
                    route = "editor/{songId}",
                    arguments = listOf(navArgument("songId") { type = NavType.LongType }),
                ) {
                    LyricsEditorScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
