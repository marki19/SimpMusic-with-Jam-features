package com.marki19.simpmusic.ui.screen.jam

import android.widget.Toast
import androidx.compose.foundation.layout.*
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import kotlinx.coroutines.delay
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
    val sessionState by viewModel.sessionState.collectAsState()
    val context = LocalContext.current

    // Immediately trigger session creation upon launching Host screen
    LaunchedEffect(Unit) {
        if (sessionState != null) {
            // We are already in a session, but want to host a new one. Tear down first.
            viewModel.leaveSessionAndWait()
        }
        viewModel.createSession(
            initialVideoId = initialVideoId,
            initialTitle = initialTitle,
            initialArtist = initialArtist,
            initialThumbnailUrl = initialThumbnailUrl,
            initialDurationMs = initialDurationMs
        )
    }

    // Automatically navigate to Jam session screen as soon as session is ready
    LaunchedEffect(sessionState) {
        if (sessionState != null && sessionState!!.isHost) {
            onNavigateToSession()
        }
    }

    // Navigate back with an error toast if the connection times out or fails
    LaunchedEffect(Unit) {
        viewModel.connectionError.collectLatest { errorMsg ->
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
            onBack()
        }
    }

    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    Scaffold(
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
                if (elapsedSeconds < 5) {
                    Text("Connecting to server...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text("Waking up server... (~50s)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("${elapsedSeconds}s elapsed", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

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
