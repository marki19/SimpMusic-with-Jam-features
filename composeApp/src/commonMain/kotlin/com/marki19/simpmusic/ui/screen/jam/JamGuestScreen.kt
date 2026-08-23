package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.foundation.layout.*
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JamGuestScreen(
    viewModel: JamViewModel,
    onNavigateToSession: () -> Unit,
    onBack: () -> Unit
) {
    var roomCode by remember { mutableStateOf(TextFieldValue("")) }
    val isConnecting by viewModel.isConnecting.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.sessionCreatedEvent.collectLatest { _ ->
            onNavigateToSession()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.connectionError.collectLatest { errorMsg ->
            snackbarHostState.showSnackbar(errorMsg)
        }
    }

    if (isConnecting) {
        AlertDialog(
            onDismissRequest = { 
                viewModel.cancelConnection()
                onBack()
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cancelConnection()
                    onBack()
                }) {
                    Text("Cancel")
                }
            },
            title = { Text("Joining Jam") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to server...")
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Join a Jam") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Join a Jam Session", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = roomCode,
                onValueChange = { if (it.text.length <= 6) roomCode = it },
                label = { Text("6-Digit Room Code") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { viewModel.joinSession(roomCode.text.uppercase()) },
                enabled = roomCode.text.length == 6 && !isConnecting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            ) {
                Text("Join Room")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = {
                    viewModel.cancelConnection()
                    onBack()
                },
                modifier = Modifier.fillMaxWidth(0.8f).height(50.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}
