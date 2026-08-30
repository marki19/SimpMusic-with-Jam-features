package com.marki19.simpmusic.ui.screen.jam

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
import com.maxrave.simpmusic.ui.icon.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.Crossfade
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    val accountThumbnail by viewModel.accountThumbnail.collectAsStateWithLifecycle()
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val nowPlayingJamTrack by viewModel.nowPlayingJamTrack.collectAsStateWithLifecycle()
    val unreadChatCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var showAddSongSheet by remember { mutableStateOf(false) }
    var showEndSessionDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    var wasActive by remember { mutableStateOf(false) }
    if (sessionState != null) {
        wasActive = true
    }

    // Only navigate back if the session WAS active previously and has now ended
    if (sessionState == null) {
        if (wasActive) {
            LaunchedEffect(Unit) { onBack() }
        }
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

    // Separate real queue from recommendations
    val currentSongId = session.playbackState.currentSongId?.cleanId()
    val manualQueue = session.playbackState.queue.filter {
        if (!currentSongId.isNullOrBlank()) it.videoId.cleanId() != currentSongId else true
    }

    val recommendations = session.recommendations.filter { rec ->
        val inQueue = session.playbackState.queue.any { it.videoId.cleanId() == rec.videoId.cleanId() }
        !inQueue && (currentSongId.isNullOrBlank() || rec.videoId.cleanId() != currentSongId)
    }

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
            JamTopBar(
                unreadChatCount = unreadChatCount,
                onBack = onBack,
                onChat = {
                    showChatSheet = true
                    viewModel.setChatSheetOpen(true)
                },
            )
        },
        bottomBar = {
            val localControlState by mediaPlayerHandler.controlState.collectAsStateWithLifecycle()
            val localTimeline by sharedViewModel.timeline.collectAsStateWithLifecycle()
            val isLocallyPlaying = localControlState.isPlaying || mediaPlayerHandler.player.playWhenReady
            val effectiveIsPlaying = if (isHost) isLocallyPlaying else session.playbackState.isPlaying
            val canSeek = isHost || perms.allowSeek

            val pb = session.playbackState
            val lagMs = if (pb.serverTimestampMs > 0)
                (kotlin.time.Clock.System.now().toEpochMilliseconds() - pb.serverTimestampMs).coerceAtLeast(0L)
            else 0L
            // Don't fall back to firstOrNull() — the server removes the playing track from the queue,
            // so firstOrNull() would return the *next* song's duration, making the bar look nearly done.
            val currentItem = pb.queue.find { it.videoId.cleanId() == pb.currentSongId?.cleanId() }

            // Fallback duration from the local player when the playing song isn't in the queue
            val fallbackDurationMs = nowPlayingJamTrack?.durationMs?.takeIf { it > 0L }
                ?: nowPlayingState?.track?.durationSeconds?.let { it.toLong() * 1000L } 
                ?: 0L

            val jamTotalMs = if (localTimeline.total > 0L) {
                // ExoPlayer's player.duration is always in milliseconds — use it for both host and guest.
                // The guest's local player is also loaded by JamPlayerSynchronizer Section 1.
                localTimeline.total
            } else {
                currentItem?.durationMs?.takeIf { it > 0L } ?: fallbackDurationMs
            }

            val jamCurrentMs = if (isHost) {
                localTimeline.current.coerceAtLeast(0L)
            } else {
                if (effectiveIsPlaying) pb.playbackPositionMs + lagMs else pb.playbackPositionMs
            }

            val jamTimeline = com.maxrave.domain.data.model.streams.TimeLine(
                current = jamCurrentMs,
                total = jamTotalMs,
                bufferedPercent = if (isHost) localTimeline.bufferedPercent else 100,
                loading = if (isHost) localTimeline.loading else pb.currentSongId.isNullOrBlank()
            )

            JamBottomPlaybackControlBar(
                isPlaying = effectiveIsPlaying,
                shuffle = session.playbackState.shuffle,
                repeat = session.playbackState.repeatMode,
                canControl = isHost || perms.allowPause,
                canSeek = canSeek,
                timeline = jamTimeline,
                onSeekTo = viewModel::seekTo,
                onTogglePlayPause = {
                    if (isHost || perms.allowPause) {
                        if (effectiveIsPlaying) viewModel.pause() else viewModel.play()
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

                // ── 2. Header & Host Actions (Spotify Style) ──────────────────
                item(key = "header_and_actions") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Device info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                SimpIcons.Sensors,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "This device",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title
                        Text(
                            text = sessionTitleText,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Host Avatar + Buttons
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ParticipantAvatar(
                                participant = hostParticipant,
                                fallbackName = if (isHost) (accountName ?: viewModel.localUserName ?: "Host") else (hostParticipant?.name ?: "Host"),
                                isHost = true,
                                overrideImageUrl = if (isHost) accountThumbnail else null,
                            )
                            
                            // Invite button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { 
                                        clipboardManager.setText(AnnotatedString(session.roomId))
                                        sharedViewModel.makeToast("Room code copied: ${session.roomId}")
                                    }
                            ) {
                                Text(
                                    "Invite",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }

                            // Leave / End Jam button
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Transparent,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { showEndSessionDialog = true }
                            ) {
                                Text(
                                    if (isHost) "End" else "Leave",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // "Let others change what's playing" toggle (Host only)
                        if (isHost) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Let others change what's playing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Switch(
                                    checked = perms.allowAddSongs && perms.allowPause,
                                    onCheckedChange = { isChecked ->
                                        viewModel.updatePermissions(
                                            perms.copy(
                                                allowAddSongs = isChecked,
                                                allowRemoveSongs = isChecked,
                                                allowPause = isChecked,
                                                allowSkip = isChecked,
                                                allowSeek = isChecked
                                            )
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Participants list as a row of avatars
                        Text(
                            text = "${session.participants.size} participant${if (session.participants.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(session.participants) { participant ->
                                val isParticipantMe = participant.userId == viewModel.localUserId
                                ParticipantAvatar(
                                    participant = participant,
                                    fallbackName = if (isParticipantMe) (accountName ?: viewModel.localUserName ?: participant.name) else participant.name,
                                    isHost = participant.userId == session.hostId,
                                    overrideImageUrl = if (isParticipantMe) accountThumbnail else null,
                                )
                            }
                        }
                    }
                }

                // ── 3. Add Songs Pill & Action Bar ────────────────────────────
                item(key = "action_bar") {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { showAddSongSheet = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(
                                    SimpIcons.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Add songs",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── 4. Now Playing Section ────────────────────────────────────
                item(key = "now_playing_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Now playing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (isHost) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = "HOST",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                item(key = "now_playing") {
                    val currentItem = session.playbackState.queue.find {
                        it.videoId.cleanId() == currentSongId
                    }
                    val initialTrack = viewModel.initialTrack
                    val handlerTrack = nowPlayingState?.songEntity
                    val handlerMediaItem = nowPlayingState?.mediaItem?.takeIf { it.mediaId.isNotBlank() }

                    val resolvedTitle = nowPlayingJamTrack?.title?.ifBlank { null }
                        ?: currentItem?.title?.ifBlank { null }
                        ?: initialTrack?.title?.ifBlank { null }
                        ?: handlerTrack?.title?.ifBlank { null }
                        ?: handlerMediaItem?.metadata?.title?.toString()?.ifBlank { null }
                        ?: if (!currentSongId.isNullOrBlank()) "Playing Track" else null

                    val resolvedArtist = nowPlayingJamTrack?.artist?.ifBlank { null }
                        ?: currentItem?.artist?.ifBlank { null }
                        ?: initialTrack?.artists?.joinToString(", ") { it.name }?.ifBlank { null }
                        ?: handlerTrack?.artistName?.joinToString(", ")?.ifBlank { null }
                        ?: handlerMediaItem?.metadata?.artist?.toString()?.ifBlank { null }

                    val resolvedArtwork: String? = nowPlayingJamTrack?.thumbnailUrl?.ifBlank { null }
                        ?: currentItem?.thumbnailUrl?.ifBlank { null }
                        ?: initialTrack?.thumbnails?.lastOrNull()?.url?.ifBlank { null }
                        ?: handlerTrack?.thumbnails?.ifBlank { null }
                        ?: handlerMediaItem?.metadata?.artworkUri?.toString()?.ifBlank { null }

                    LaunchedEffect(currentItem?.videoId) {
                        if (currentItem != null) viewModel.clearInitialTrack()
                    }

                    val isRoomEmpty = session.playbackState.currentSongId.isNullOrBlank() && session.playbackState.queue.isEmpty()

                    if (resolvedTitle != null) {
                        val queuedById = nowPlayingJamTrack?.addedBy ?: currentItem?.addedBy ?: ""
                        val queuedByParticipant = session.participants.find { it.userId == queuedById }
                        
                        NowPlayingRow(
                            title = resolvedTitle,
                            artist = resolvedArtist ?: "Unknown Artist",
                            artworkUrl = resolvedArtwork,
                            queuedByParticipant = queuedByParticipant,
                            onRowClick = onOpenNowPlaying,
                        )
                    } else if (isRoomEmpty) {
                        NowPlayingEmptyRow()
                    } else {
                        NowPlayingSkeletonRow()
                    }
                }

                // ── 5. Queue Section — "Next From: [source]" ──────────────────
                item(key = "queue_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Next from Queue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
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
                        val queuedByParticipant = session.participants.find { it.userId == item.addedBy }

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
                                            SimpIcons.Delete,
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
                                    queuedByParticipant = queuedByParticipant,
                                    dragModifier = Modifier.draggableHandle(
                                        onDragStarted = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                                    ),
                                    onClick = {
                                        viewModel.playNow(
                                            videoId = item.videoId,
                                            title = item.title,
                                            artist = item.artist,
                                            thumbnailUrl = item.thumbnailUrl,
                                            durationMs = item.durationMs
                                        )
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }
                    }
                }

                // ── Recommendations Section (if active) ─────────────────────
                if (session.recommendationsEnabled && recommendations.isNotEmpty()) {
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
                                    SimpIcons.Add,
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
                                        Icon(SimpIcons.Sync, contentDescription = "Refresh")
                                    }
                                    IconButton(onClick = { viewModel.toggleRecommendations(false) }) {
                                        Icon(SimpIcons.Close, contentDescription = "Disable")
                                    }
                                }
                            }
                        }
                    }

                    items(recommendations, key = { it.queueId }) { item ->
                        RecommendationRowItem(
                            item = item,
                            onPlayNow = {
                                viewModel.playNow(item.videoId, item.title, item.artist, item.thumbnailUrl, item.durationMs)
                                sharedViewModel.makeToast("Now playing: ${item.title}")
                            },
                            onAddToQueue = {
                                viewModel.addToQueue(item.videoId, item.title, item.artist, item.thumbnailUrl, item.durationMs)
                                sharedViewModel.makeToast("Added ${item.title} to queue")
                            },
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 2.dp)
                                .animateItem(),
                        )
                    }
                }
            }

            // Chat Sheet
            if (showChatSheet) {
                val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val chatScope = rememberCoroutineScope()
                ModalBottomSheet(
                    onDismissRequest = {
                        showChatSheet = false
                        viewModel.setChatSheetOpen(false)
                    },
                    sheetState = chatSheetState,
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
                            Text("Session Chat", style = MaterialTheme.typography.titleLarge)
                            IconButton(onClick = {
                                chatScope.launch {
                                    chatSheetState.hide()
                                    showChatSheet = false
                                    viewModel.setChatSheetOpen(false)
                                }
                            }) {
                                Icon(SimpIcons.Close, contentDescription = "Close")
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            reverseLayout = true,
                        ) {
                            items(chatMessages.reversed()) { msg ->
                                val isMe = msg.senderId == viewModel.localUserId
                                // Look up the participant by exact userId — this includes the local user
                                val participant = session.participants.find { it.userId == msg.senderId }
                                // If it's our own message and the participant lookup failed,
                                // try matching by host ID as a fallback (host's userId == hostId)
                                val effectiveParticipant = participant
                                    ?: if (isMe && session.hostId == msg.senderId) session.participants.find { it.userId == session.hostId } else null
                                val senderName = if (isMe)
                                    effectiveParticipant?.name?.ifBlank { null } ?: viewModel.localUserName ?: "You"
                                else
                                    effectiveParticipant?.name?.ifBlank { null } ?: "Participant"
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    if (!isMe) {
                                        ParticipantAvatar(
                                            participant = effectiveParticipant,
                                            fallbackName = senderName,
                                            isHost = msg.senderId == session.hostId,
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Column(
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                                        modifier = Modifier.widthIn(max = 260.dp)
                                    ) {
                                        Text(
                                            senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        ) {
                                            Text(
                                                text = msg.text,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                    if (isMe) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        ParticipantAvatar(
                                            participant = effectiveParticipant,
                                            fallbackName = effectiveParticipant?.name ?: accountName ?: viewModel.localUserName ?: senderName,
                                            isHost = isHost,
                                            overrideImageUrl = accountThumbnail,
                                        )
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
                                Icon(
                                    SimpIcons.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
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
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = if (session.isHost) "End Session" else "Leave",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEndSessionDialog = false },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
} // Closes Scaffold body
} // Closes JamSessionScreen

// ─────────────────────────────────────────────────────────────────────────────
//  Top App Bar
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamTopBar(
    unreadChatCount: Int,
    onBack: () -> Unit,
    onChat: () -> Unit,
) {
    TopAppBar(
        title = {
            // Keep title empty for Spotify minimal UI
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    SimpIcons.Close,
                    contentDescription = "Close Jam",
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
                    Icon(SimpIcons.Subtitles, contentDescription = "Chat")
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
    queuedByParticipant: JamParticipant?,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onRowClick)
            .padding(vertical = 6.dp),
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
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "E",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        if (queuedByParticipant != null) {
            ParticipantAvatar(
                participant = queuedByParticipant,
                fallbackName = queuedByParticipant.name,
                isHost = false
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        Icon(
            SimpIcons.MoreVert,
            contentDescription = "More options",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
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
    queuedByParticipant: JamParticipant?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val elevation by androidx.compose.animation.core.animateFloatAsState(
        if (isDragging) 8f else 0f,
        label = "drag elevation",
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        tonalElevation = elevation.dp,
        color = if (isDragging) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circle selector style
            Icon(
                SimpIcons.AddCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
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

            Spacer(modifier = Modifier.width(8.dp))

            // Queued By Avatar
            if (queuedByParticipant != null) {
                ParticipantAvatar(
                    participant = queuedByParticipant,
                    fallbackName = queuedByParticipant.name,
                    isHost = false
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Far Right: Dedicated Always-Visible Drag Handle (☰)
            if (canDrag) {
                Icon(
                    SimpIcons.DragHandle,
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

@Composable
private fun RecommendationRowItem(
    item: JamQueueItem,
    onPlayNow: () -> Unit,
    onAddToQueue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onPlayNow),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = "Cover",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(12.dp))

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

            IconButton(onClick = onAddToQueue) {
                Icon(
                    SimpIcons.PlaylistAdd,
                    contentDescription = "Add to Queue",
                    tint = MaterialTheme.colorScheme.primary,
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
    canSeek: Boolean,
    timeline: com.maxrave.domain.data.model.streams.TimeLine,
    onSeekTo: (Long) -> Unit,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val currentMs = timeline.current.coerceAtLeast(0L)
                // total is already normalized to ms by the caller — don't re-apply the seconds heuristic
                val totalMs = timeline.total.coerceAtLeast(0L)
                var isScrubbing by remember { mutableStateOf(false) }
                var sliderValue by remember { mutableFloatStateOf(0f) }

                LaunchedEffect(key1 = timeline.current, key2 = timeline.total, key3 = isScrubbing) {
                    if (!isScrubbing) {
                        sliderValue = if (totalMs > 0L) {
                            (currentMs.toFloat() * 100f / totalMs.toFloat()).coerceIn(0f, 100f)
                        } else {
                            0f
                        }
                    }
                }

                val displayPos = if (isScrubbing) {
                    ((sliderValue / 100f) * totalMs.toFloat()).toLong().coerceAtLeast(0L)
                } else {
                    currentMs
                }

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = timeline.loading || totalMs <= 0L) { isLoading ->
                            if (isLoading) {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .padding(horizontal = 3.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        strokeCap = StrokeCap.Round,
                                    )
                                }
                            } else {
                                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                    LinearProgressIndicator(
                                        progress = { (timeline.bufferedPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .padding(horizontal = 3.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        strokeCap = StrokeCap.Round,
                                        drawStopIndicator = {},
                                    )
                                }
                            }
                        }
                        if (totalMs > 0L) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                                Slider(
                                    value = sliderValue,
                                    valueRange = 0f..100f,
                                    onValueChange = { newValue ->
                                        isScrubbing = true
                                        sliderValue = newValue
                                    },
                                    onValueChangeFinished = {
                                        isScrubbing = false
                                        if (canSeek) {
                                            val targetMs = ((sliderValue / 100f) * totalMs.toFloat()).toLong()
                                            onSeekTo(targetMs)
                                        }
                                    },
                                    enabled = canSeek,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    track = { sliderState ->
                                        SliderDefaults.Track(
                                            modifier = Modifier.height(4.dp),
                                            enabled = canSeek,
                                            sliderState = sliderState,
                                            colors = SliderDefaults.colors().copy(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                            thumbTrackGapSize = 0.dp,
                                            drawTick = { _, _ -> },
                                            drawStopIndicator = null,
                                        )
                                    },
                                    thumb = {
                                        SliderDefaults.Thumb(
                                            modifier = Modifier
                                                .size(14.dp),
                                            thumbSize = DpSize(14.dp, 14.dp),
                                            interactionSource = remember { MutableInteractionSource() },
                                            colors = SliderDefaults.colors().copy(
                                                thumbColor = MaterialTheme.colorScheme.primary,
                                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                                inactiveTrackColor = Color.Transparent,
                                            ),
                                            enabled = canSeek,
                                        )
                                    }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatJamDurationMs(displayPos),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatJamDurationMs(totalMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                // 1. Shuffle (Crossed arrows)
                IconButton(onClick = onToggleShuffle, enabled = canControl) {
                    Icon(
                        SimpIcons.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                // 2. Previous track
                IconButton(onClick = onPrevious, enabled = canControl) {
                    Icon(
                        SimpIcons.SkipPrevious,
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
                            if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                // 4. Next track
                IconButton(onClick = onNext, enabled = canControl) {
                    Icon(
                        SimpIcons.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // 5. Repeat/loop
                IconButton(onClick = onCycleRepeat, enabled = canControl) {
                    Icon(
                        if (repeat == JamRepeatMode.ONE) SimpIcons.RepeatOne else SimpIcons.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeat != JamRepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        }
    }
}

private fun formatJamDurationMs(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val min = totalSec / 60
    val sec = totalSec % 60
    val secStr = if (sec < 10) "0$sec" else "$sec"
    return "$min:$secStr"
}

// ─────────────────────────────────────────────────────────────────────────────
//  Participant avatar with online dot
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ParticipantAvatar(
    participant: JamParticipant?,
    fallbackName: String,
    isHost: Boolean,
    overrideImageUrl: String? = null,
) {
    val displayName = participant?.name?.ifBlank { null } ?: fallbackName
    val imageUrl = overrideImageUrl?.ifBlank { null } ?: participant?.imageUrl?.ifBlank { null }
    val hasValidImage = !imageUrl.isNullOrBlank()

    Box(modifier = Modifier.size(28.dp)) {
        if (hasValidImage && imageUrl != null) {
            val resolvedUrl = if (imageUrl.startsWith("//")) "https:$imageUrl" else imageUrl
            AsyncImage(
                model = resolvedUrl,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            val initial = displayName.take(1).uppercase().ifBlank { "?" }
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
                    text = initial,
                    color = if (isHost) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
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
                imageVector = SimpIcons.Sensors,
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
