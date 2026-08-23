package com.marki19.simpmusic.ui.navigation.graph

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.marki19.simpmusic.ui.navigation.destination.jam.JamGuestDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamHostDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamMenuDestination
import com.marki19.simpmusic.ui.navigation.destination.jam.JamSessionDestination
import com.marki19.simpmusic.ui.screen.jam.JamGuestScreen
import com.marki19.simpmusic.ui.screen.jam.JamHostScreen
import com.marki19.simpmusic.ui.screen.jam.JamMenuScreen
import com.marki19.simpmusic.ui.screen.jam.JamSessionScreen
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.simpmusic.ui.navigation.destination.home.HomeDestination
import org.koin.compose.koinInject

fun NavGraphBuilder.jamScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
    showNowPlayingSheet: () -> Unit = {},
    hideNavBar: () -> Unit = {},
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = {},
) {
    composable<JamMenuDestination> { backStackEntry ->
        val dest = backStackEntry.toRoute<JamMenuDestination>()
        JamMenuScreen(
            navController = navController,
            initialVideoId = dest.initialVideoId,
            initialTitle = dest.initialTitle,
            initialArtist = dest.initialArtist,
            initialThumbnailUrl = dest.initialThumbnailUrl,
            initialDurationMs = dest.initialDurationMs,
        )
    }
    
    composable<JamHostDestination> { backStackEntry ->
        val dest = backStackEntry.toRoute<JamHostDestination>()
        val jamViewModel: JamViewModel = koinInject()
        JamHostScreen(
            viewModel = jamViewModel,
            initialVideoId = dest.initialVideoId,
            initialTitle = dest.initialTitle,
            initialArtist = dest.initialArtist,
            initialThumbnailUrl = dest.initialThumbnailUrl,
            initialDurationMs = dest.initialDurationMs,
            onNavigateToSession = { 
                navController.navigate(JamSessionDestination(roomCode = jamViewModel.sessionState.value?.roomId ?: "")) {
                    popUpTo<JamHostDestination> { inclusive = true }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
    
    composable<JamGuestDestination> {
        val jamViewModel: JamViewModel = koinInject()
        JamGuestScreen(
            viewModel = jamViewModel,
            onNavigateToSession = { 
                navController.navigate(JamSessionDestination(roomCode = jamViewModel.sessionState.value?.roomId ?: "")) {
                    popUpTo<JamGuestDestination> { inclusive = true }
                }
            },
            onBack = { navController.navigateUp() }
        )
    }
    
    composable<JamSessionDestination> { entry ->
        val jamViewModel: JamViewModel = koinInject()
        val params = entry.toRoute<JamSessionDestination>()
        JamSessionScreen(
            viewModel = jamViewModel,
            onBack = {
                navController.navigate(HomeDestination) {
                    popUpTo(HomeDestination)
                }
            },
            onOpenNowPlaying = showNowPlayingSheet,
        )
    }
}
