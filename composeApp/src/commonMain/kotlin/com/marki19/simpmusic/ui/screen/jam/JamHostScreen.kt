package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Close
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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerCreate() {
        errorMessage = null
        viewModel.createSession(
            initialVideoId = initialVideoId,
            initialTitle = initialTitle,
            initialArtist = initialArtist,
            initialThumbnailUrl = initialThumbnailUrl,
            initialDurationMs = initialDurationMs
        )
    }

    // Immediately trigger session creation upon launching Host screen
    LaunchedEffect(Unit) {
        triggerCreate()
    }

    // Automatically navigate to Jam session screen when single-shot sessionCreatedEvent fires
    LaunchedEffect(Unit) {
        viewModel.sessionCreatedEvent.collectLatest { _ ->
            onNavigateToSession()
        }
    }

    // Show error state without instantly popping screen
    LaunchedEffect(Unit) {
        viewModel.connectionError.collectLatest { errorMsg ->
            errorMessage = errorMsg
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
            if (errorMessage != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = SimpIcons.Close,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Unable to Start Jam Room",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.cancelConnection()
                                onBack()
                            }
                        ) {
                            Text("Back")
                        }
                        Button(
                            onClick = { triggerCreate() }
                        ) {
                            Text("Retry")
                        }
                    }
                }
            } else {
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
}
