package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.SimpIcons
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamHostScreen(
    viewModel: JamViewModel,
    initialVideoId: String? = null,
    initialTitle: String? = null,
    initialArtist: String? = null,
    initialThumbnailUrl: String? = null,
    initialDurationMs: Long? = null,
    onNavigateToSession: () -> Unit,
    onBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Immediately trigger session creation upon launching Host screen
    LaunchedEffect(Unit) {
        println("=== DEBUG JAM: JamHostScreen LaunchedEffect(Unit) -> calling createSession ===")
        viewModel.createSession(
            initialVideoId = initialVideoId,
            initialTitle = initialTitle,
            initialArtist = initialArtist,
            initialThumbnailUrl = initialThumbnailUrl,
            initialDurationMs = initialDurationMs
        )
    }

    // Automatically navigate to Jam session screen when single-shot sessionCreatedEvent fires
    LaunchedEffect(Unit) {
        viewModel.sessionCreatedEvent.collectLatest { _ ->
            println("=== DEBUG JAM: JamHostScreen sessionCreatedEvent received -> Triggering onNavigateToSession() ===")
            onNavigateToSession()
        }
    }

    // Navigate back with an error snackbar if the connection times out or fails
    LaunchedEffect(Unit) {
        viewModel.connectionError.collectLatest { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Hosting Jam") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.cancelConnection()
                        onBack()
                    }) {
                        Icon(imageVector = SimpIcons.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "Setting up your Jam room...",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Connecting to server...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(32.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.cancelConnection()
                        onBack()
                    },
                    modifier = Modifier.width(160.dp).height(44.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}
