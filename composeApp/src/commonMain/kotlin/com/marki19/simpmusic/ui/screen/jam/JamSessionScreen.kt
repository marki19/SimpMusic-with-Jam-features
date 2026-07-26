package com.marki19.simpmusic.ui.screen.jam

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.marki19.domain.jam.cleanId
import com.marki19.domain.jam.JamParticipant
import com.marki19.domain.jam.JamQueueItem
import com.marki19.domain.jam.JamRepeatMode
import com.marki19.domain.jam.JamSessionState
import com.marki19.simpmusic.viewModel.jam.JamViewModel
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.simpmusic.viewModel.SharedViewModel
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

// ─────────────────────────────────────────────────────────────────────────────
//  Jam Session Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun JamSessionScreen(
    viewModel: JamViewModel,
    sharedViewModel: SharedViewModel = koinInject(),
    mediaPlayerHandler: MediaPlayerHandler = koinInject(),
    onBack: () -> Unit,
    onOpenNowPlaying: () -> Unit = {},
) {

    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val nowPlayingState by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val hostTransferNotice by viewModel.hostTransferNotice.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showAddSongSheet by remember { mutableStateOf(false) }
    var showEndSessionDialog by remember { mutableStateOf(false) }
    var showNowPlayingMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    // Navigate back if session ended
    if (sessionState == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val session = sessionState!!
    val isHost = session.isHost
    val perms = session.permissions

    // HOST_TRANSFER snackbar
    LaunchedEffect(hostTransferNotice) {
        hostTransferNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice, duration = SnackbarDuration.Short)
            viewModel.dismissHostTransferNotice()
        }
    }

    val hostParticipant = session.participants.find { it.userId == session.hostId }
    val hostName = hostParticipant?.name?.ifBlank { null } ?: if (isHost) "Your" else "Host"
    val sessionTitleText = if (isHost) "Your Jam" else "${hostName}'s Jam"
    val sessionChipText = if (isHost) "Your Jam Session" else "${hostName}'s Jam Session"
    val playlistName = nowPlayingState?.songEntity?.albumName?.ifBlank { null } ?: "Playlist"

    // Full queue = manual + recommendations
    val fullQueue = session.playbackState.queue

    // Current playing song
    val nowPlayingId = session.playbackState.currentSongId?.substringAfterLast('/')
        ?: mediaPlayerHandler.nowPlayingState.value?.track?.videoId?.substringAfterLast('/')

    val currentIdx = fullQueue.indexOfFirst { it.videoId.substringAfterLast('/') == nowPlayingId }
    val upcomingQueue = when {
        currentIdx >= 0 -> fullQueue.drop(currentIdx + 1)
        nowPlayingId != null -> fullQueue.filter { it.videoId.substringAfterLast('/') != nowPlayingId }
        else -> fullQueue
    }

    val manualQueue = upcomingQueue.filter { !it.isRecommendation }
    val recommendations = upcomingQueue.filter { it.isRecommendation }

    // Lazy list state for reordering
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        val fromItem = from.key as? String ?: return@rememberReorderableLazyListState
        val toItem = to.key as? String ?: return@rememberReorderableLazyListState
        val toIndex = manualQueue.indexOfFirst { it.queueId == toItem }
        if (toIndex >= 0) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.moveQueueItem(fromItem, toIndex)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val unreadChatCount by viewModel.unreadChatCount.collectAsState()
            JamTopBar(
                playlistName = playlistName,
                isSyncing = isSyncing,
                unreadChatCount = unreadChatCount,
                onBack = onBack,
                onChat = {
                    showChatSheet = true
                    viewModel.setChatSheetOpen(true)
                },
            )
        },
        bottomBar = {
            JamBottomPlaybackControlBar(
                isPlaying = session.playbackState.isPlaying,
                shuffle = session.playbackState.shuffle,
                repeat = session.playbackState.repeatMode,
                canControl = isHost || perms.allowPause,
                onTogglePlayPause = {
                    if (isHost || perms.allowPause) {
                        if (session.playbackState.isPlaying) viewModel.pause() else viewModel.play()
                    }
                },
                onToggleShuffle = { viewModel.setShuffle(!session.playbackState.shuffle) },
                onCycleRepeat = {
                    val next = when (session.playbackState.repeatMode) {
                        JamRepeatMode.OFF -> JamRepeatMode.QUEUE
                        JamRepeatMode.QUEUE -> JamRepeatMode.ONE
                        JamRepeatMode.ONE -> JamRepeatMode.OFF
                    }
                    viewModel.setRepeat(next)
                },
                onPrevious = { if (isHost || perms.allowSkip) viewModel.skipPrevious() },
                onNext = { if (isHost || perms.allowSkip) viewModel.skipNext() },
            )
        }
    ) { padding ->

        Box(modifier = Modifier.fillMaxSize()) {

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {

                // ── 2. Session Identity Block ───────────────────────────────────
                item(key = "session_identity") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = sessionTitleText,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Avatar pill + Chip badge row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { showSettingsSheet = true }
                                .padding(vertical = 2.dp, horizontal = 4.dp)
                        ) {
                            // Stacked participant avatars in a pill border container
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(session.participants.take(3)) { participant ->
                                            ParticipantAvatar(
                                                participant = participant,
                                                fallbackName = participant.userId,
                                                isHost = participant.userId == session.hostId,
                                            )
                                        }
                                    }
                                }
                            }

                            // Accent colored chip
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Devices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = sessionChipText,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── 3. Session Action Buttons ─────────────────────────────────
                item(key = "session_actions") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Invite button
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(session.roomId))
                                sharedViewModel.makeToast("Room code copied: ${session.roomId}")
                            },
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        ) {
                            Text(
                                "Invite",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Leave / End Jam button
                        OutlinedButton(
                            onClick = { showEndSessionDialog = true },
                            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                        ) {
                            Text(
                                if (isHost) "End Jam" else "Leave",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                item(key = "divider_1") {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }

                // ── 4. "Now Playing" Section ──────────────────────────────────
                item(key = "now_playing_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Now playing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isHost || perms.allowAddSongs) {
                            Button(
                                onClick = { showAddSongSheet = true },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black // High-contrast black text on light blue background for WCAG AA compliance
                                )
                            ) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "+ Add songs",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                item(key = "now_playing") {
                    val currentSongId = session.playbackState.currentSongId?.cleanId()
                    val currentItem = session.playbackState.queue.find {
                        it.videoId.cleanId() == currentSongId
                    }
                    val initialTrack = viewModel.initialTrack
                    val handlerTrack = mediaPlayerHandler.nowPlayingState.value?.track

                    val resolvedTitle = currentItem?.title?.ifBlank { null }
                        ?: initialTrack?.title?.ifBlank { null }
                        ?: handlerTrack?.title?.ifBlank { null }
                    val resolvedArtist = currentItem?.artist?.ifBlank { null }
                        ?: initialTrack?.artists?.joinToString(", ") { it.name }?.ifBlank { null }
                        ?: handlerTrack?.artists?.joinToString(", ") { it.name }?.ifBlank { null }
                    val resolvedArtwork = currentItem?.thumbnailUrl?.ifBlank { null }
                        ?: initialTrack?.thumbnails?.lastOrNull()?.url
                        ?: handlerTrack?.thumbnails?.lastOrNull()?.url

                    LaunchedEffect(currentItem?.videoId) {
                        if (currentItem != null) viewModel.clearInitialTrack()
                    }

                    if (resolvedTitle != null) {
                        NowPlayingRow(
                            title = resolvedTitle,
                            artist = resolvedArtist ?: "Unknown Artist",
                            artworkUrl = resolvedArtwork,
                            showMenu = showNowPlayingMenu,
                            onToggleMenu = { showNowPlayingMenu = !showNowPlayingMenu },
                            onDismissMenu = { showNowPlayingMenu = false },
                            onRowClick = onOpenNowPlaying,
                            onRemoveFromQueue = {
                                if (currentItem != null && (isHost || perms.allowRemoveSongs)) {
                                    viewModel.removeFromQueue(currentItem.queueId)
                                }
                            }
                        )
                    } else if (session.playbackState.currentSongId.isNullOrBlank() && session.playbackState.queue.isEmpty()) {
                        NowPlayingEmptyRow()
                    } else {
                        NowPlayingSkeletonRow()
                    }
                }

                // ── 5. Queue Section — "Next From: [source]" ──────────────────
                item(key = "queue_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Next From: $playlistName",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // ── Queue List ────────────────────────────────────────────────
                if (manualQueue.isEmpty()) {
                    item(key = "empty_queue") {
                        Text(
                            "Queue is empty — add a song to get started!",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    items(manualQueue, key = { it.queueId }) { item ->
                        val canDrag = isHost || (perms.allowReorder && item.addedBy == viewModel.localUserId)
                        val canRemove = isHost || (perms.allowRemoveSongs && item.addedBy == viewModel.localUserId)

                        ReorderableItem(reorderState, key = item.queueId, enabled = canDrag) { isDragging ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart && canRemove) {
                                        viewModel.removeFromQueue(item.queueId)
                                    }
                                    false
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .animateItem(),
                                enableDismissFromStartToEnd = false,
                                enableDismissFromEndToStart = canRemove,
                                backgroundContent = {
                                    val alpha = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) 0.85f else 0f
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = alpha))
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd,
                                    ) {
                                        Icon(
                                            Icons.Rounded.Delete,
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                },
                            ) {
                                QueueRowItem(
                                    item = item,
                                    isDragging = isDragging,
                                    canDrag = canDrag,
                                    dragModifier = Modifier.draggableHandle(
                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    ),
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                // ── Recommendations Section (if active) ─────────────────────
                if (recommendations.isNotEmpty()) {
                    item(key = "recs_header") {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Based on Everyone's Taste",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (isHost) {
                                Row {
                                    IconButton(onClick = { viewModel.refreshRecommendations() }) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                                    }
                                    IconButton(onClick = { viewModel.toggleRecommendations(false) }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Disable")
                                    }
                                }
                            }
                        }
                    }

                    items(recommendations, key = { it.queueId }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.StartToEnd) {
                                    viewModel.addToQueue(item.videoId, item.title, item.artist, item.thumbnailUrl, item.durationMs)
                                    sharedViewModel.makeToast("Added ${item.title} to queue")
                                }
                                false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .animateItem(),
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = false,
                            backgroundContent = {
                                val alpha = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) 0.85f else 0f
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.PlaylistAdd,
                                            contentDescription = "Add to Queue",
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Add to Queue",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            },
                        ) {
                            QueueRowItem(
                                item = item,
                                isDragging = false,
                                canDrag = false,
                                dragModifier = Modifier,
                                modifier = Modifier
                                    .animateItem()
                                    .clickable {
                                        viewModel.playNow(item.videoId, item.title, item.artist, item.thumbnailUrl, item.durationMs)
                                        sharedViewModel.makeToast("Now playing: ${item.title}")
                                    },
                            )
                        }
                    }
                }
            }

            // Chat Sheet
            if (showChatSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showChatSheet = false
                        viewModel.setChatSheetOpen(false)
                    },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    var textInput by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.85f)
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Room Chat", style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = {
                                showChatSheet = false
                                viewModel.setChatSheetOpen(false)
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            reverseLayout = true,
                        ) {
                            items(chatMessages.reversed()) { msg ->
                                val myUserId = viewModel.localUserId
                                val isMe = (myUserId != null && msg.senderId == myUserId) ||
                                           (session.isHost && msg.senderId == session.hostId)
                                val senderParticipant = session.participants.find { it.userId == msg.senderId }
                                val senderName = senderParticipant?.name ?: msg.senderId
                                val senderImage = senderParticipant?.imageUrl ?: ""

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    if (!isMe) {
                                        Box {
                                            if (senderImage.isNotBlank()) {
                                                AsyncImage(
                                                    model = senderImage,
                                                    contentDescription = "Profile",
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop,
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Text(
                                                        senderName.take(2).uppercase(),
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        style = MaterialTheme.typography.labelSmall,
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 280.dp)
                                    ) {
                                        Text(
                                            text = if (isMe) "You" else senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 16.dp,
                                                topEnd = 16.dp,
                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                            ),
                                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            tonalElevation = 2.dp,
                                        ) {
                                            Text(
                                                text = msg.text,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = { textInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message…") },
                                maxLines = 3,
                                shape = RoundedCornerShape(24.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    viewModel.sendChatMessage(textInput)
                                    textInput = ""
                                },
                            ) {
                                Icon(Icons.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings sheet
    if (showSettingsSheet) {
        ModalBottomSheet(onDismissRequest = { showSettingsSheet = false }) {
            JamSettingsSheetContent(
                session = session,
                isHost = isHost,
                onUpdatePermissions = viewModel::updatePermissions,
                onToggleRecommendations = viewModel::toggleRecommendations,
                onLeave = { showSettingsSheet = false; viewModel.leaveSession(); onBack() },
                onDismiss = { showSettingsSheet = false },
            )
        }
    }

    // End Session / Leave Confirmation Dialog
    if (showEndSessionDialog) {
        AlertDialog(
            onDismissRequest = { showEndSessionDialog = false },
            title = { Text(if (session.isHost) "End Jam Session?" else "Leave Jam Session?") },
            text = {
                Text(
                    if (session.isHost)
                        "Are you sure you want to end this Jam session? This will close the room for all participants."
                    else
                        "Are you sure you want to leave this Jam session?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndSessionDialog = false
                        viewModel.leaveSession()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        if (session.isHost) "End Jam" else "Leave",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndSessionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Song sheet
    if (showAddSongSheet) {
        JamAddSongBottomSheet(
            onDismissRequest = { showAddSongSheet = false },
            jamViewModel = viewModel,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamTopBar(
    playlistName: String,
    isSyncing: Boolean,
    unreadChatCount: Int,
    onBack: () -> Unit,
    onChat: () -> Unit,
) {
    TopAppBar(
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "PLAYING FROM PLAYLIST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    playlistName,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = "Minimize Jam",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        actions = {
            IconButton(onClick = onChat) {
                BadgedBox(
                    badge = {
                        if (unreadChatCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ) {
                                Text(
                                    if (unreadChatCount > 99) "99+" else unreadChatCount.toString(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Chat, contentDescription = "Chat")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Now Playing Row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NowPlayingRow(
    title: String,
    artist: String,
    artworkUrl: String?,
    showMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRowClick: () -> Unit,
    onRemoveFromQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onRowClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = "Cover",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box {
            IconButton(onClick = onToggleMenu) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Track options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu
            ) {
                DropdownMenuItem(
                    text = { Text("Remove from queue") },
                    onClick = {
                        onDismissMenu()
                        onRemoveFromQueue()
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Queue Row Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QueueRowItem(
    item: JamQueueItem,
    isDragging: Boolean,
    canDrag: Boolean,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    val elevation by androidx.compose.animation.core.animateFloatAsState(
        if (isDragging) 8f else 0f,
        label = "drag elevation",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        tonalElevation = elevation.dp,
        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Far Left: Album Artwork Thumbnail
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = "Cover",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Middle: Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Far Right: Dedicated Always-Visible Drag Handle (☰)
            if (canDrag) {
                Icon(
                    Icons.Rounded.Menu,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(24.dp)
                        .then(dragModifier),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Bottom Playback Control Bar (Controls-Only Compact Bar)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JamBottomPlaybackControlBar(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeat: JamRepeatMode,
    canControl: Boolean,
    onTogglePlayPause: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 1. Shuffle (Crossed arrows)
                IconButton(onClick = onToggleShuffle, enabled = canControl) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Previous track
                IconButton(onClick = onPrevious, enabled = canControl) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 3. Play/Pause (Center, solid white filled circle with black play/pause triangle)
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canControl, onClick = onTogglePlayPause)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // 4. Next track
                IconButton(onClick = onNext, enabled = canControl) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 5. Repeat/loop
                IconButton(onClick = onCycleRepeat, enabled = canControl) {
                    Icon(
                        if (repeat == JamRepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeat != JamRepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Participant avatar with online dot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParticipantAvatar(
    participant: JamParticipant?,
    fallbackName: String,
    isHost: Boolean,
) {
    val displayName = participant?.name ?: fallbackName
    Box(modifier = Modifier.size(28.dp)) {
        if (participant != null && participant.imageUrl.isNotBlank()) {
            AsyncImage(
                model = participant.imageUrl,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isHost) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondaryContainer
                    )
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    color = if (isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        val isOnline = participant?.online != false
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOnline) Color(0xFF4CAF50) else Color(0xFFBDBDBD))
                .border(1.dp, MaterialTheme.colorScheme.background, CircleShape)
                .align(Alignment.BottomEnd),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Settings sheet content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun JamSettingsSheetContent(
    session: JamSessionState,
    isHost: Boolean,
    onUpdatePermissions: (com.marki19.domain.jam.JamPermissions) -> Unit,
    onToggleRecommendations: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val perms = session.permissions
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 40.dp),
    ) {
        Text("Session Settings", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))

        if (isHost) {
            Text(
                "Guest Permissions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(12.dp))

            PermissionRow("Add Songs", perms.allowAddSongs) {
                onUpdatePermissions(perms.copy(allowAddSongs = it))
            }
            PermissionRow("Remove Songs", perms.allowRemoveSongs) {
                onUpdatePermissions(perms.copy(allowRemoveSongs = it))
            }
            PermissionRow("Reorder Queue", perms.allowReorder) {
                onUpdatePermissions(perms.copy(allowReorder = it))
            }
            PermissionRow("Pause / Play", perms.allowPause) {
                onUpdatePermissions(perms.copy(allowPause = it))
            }
            PermissionRow("Skip Songs", perms.allowSkip) {
                onUpdatePermissions(perms.copy(allowSkip = it))
            }
            PermissionRow("Seek", perms.allowSeek) {
                onUpdatePermissions(perms.copy(allowSeek = it))
            }
            PermissionRow("Vote on Songs", perms.allowVoting) {
                onUpdatePermissions(perms.copy(allowVoting = it))
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                "Recommendations",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            PermissionRow("Based on Everyone's Taste", session.recommendationsEnabled) {
                onToggleRecommendations(it)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
        }

        TextButton(
            onClick = onLeave,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Text(if (isHost) "End Session" else "Leave Session")
        }
    }
}

@Composable
private fun PermissionRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun NowPlayingEmptyRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.Podcasts,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                "Nothing is playing",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "Add a song to start the jam!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NowPlayingSkeletonRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}
