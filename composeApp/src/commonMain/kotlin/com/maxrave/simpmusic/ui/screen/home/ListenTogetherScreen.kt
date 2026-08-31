package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import kotlin.math.roundToInt
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.listentogether.JamChatMessage
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomMember
import com.maxrave.domain.data.model.listentogether.RoomSuggestion
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.searchResult.songs.Album
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.utils.toTrack
import com.maxrave.simpmusic.expect.shareUrl
import com.maxrave.simpmusic.expect.ui.PlatformBackdrop
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.component.SongFullWidthItems
import com.maxrave.simpmusic.ui.icon.*
import com.maxrave.simpmusic.ui.navigation.destination.home.ListenTogetherSettingsDestination
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.lt_background_warning
import simpmusic.composeapp.generated.resources.lt_cancel_join
import simpmusic.composeapp.generated.resources.lt_create_room
import simpmusic.composeapp.generated.resources.lt_display_name
import simpmusic.composeapp.generated.resources.lt_display_name_hint
import simpmusic.composeapp.generated.resources.lt_host_badge
import simpmusic.composeapp.generated.resources.lt_in_room
import simpmusic.composeapp.generated.resources.lt_join_requests
import simpmusic.composeapp.generated.resources.lt_join_room
import simpmusic.composeapp.generated.resources.lt_just_asked
import simpmusic.composeapp.generated.resources.lt_leave_room
import simpmusic.composeapp.generated.resources.lt_or_join_with_code
import simpmusic.composeapp.generated.resources.lt_room_code
import simpmusic.composeapp.generated.resources.lt_suggestions
import simpmusic.composeapp.generated.resources.lt_tagline
import simpmusic.composeapp.generated.resources.lt_waiting_approval
import simpmusic.composeapp.generated.resources.lt_waiting_approval_desc
import kotlin.time.Duration.Companion.milliseconds

private val CARD_SHAPE = RoundedCornerShape(24.dp)
private val ROW_SHAPE = RoundedCornerShape(18.dp)
private const val TWO_COLUMN_MIN_DP = 760
private val COPIED_FEEDBACK_DURATION = 1400.milliseconds
private const val SHARE_PREFIX = "Join my SimpMusic Jam with code "

@Composable
private fun tintFor(id: String): Color {
    val palette =
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.tertiaryContainer,
        )
    val index = id.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) } % palette.size
    return palette[index]
}

private fun initialOf(name: String): String = name.trim().firstOrNull()?.uppercase() ?: "?"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()
    val accountThumbUrl by viewModel.accountThumbUrl.collectAsStateWithLifecycle()
    val codeInput by viewModel.roomCodeInput.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val recommendedSongs by viewModel.recommendedSongs.collectAsStateWithLifecycle()
    val isLoadingRecommendations by viewModel.isLoadingRecommendations.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()

    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    var managing by remember { mutableStateOf<RoomMember?>(null) }
    var showAllMembersDialog by remember { mutableStateOf(false) }
    var showAddSongSheet by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var lastSeenMessageCount by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(showChatSheet, state.chatMessages.size) {
        if (showChatSheet) {
            lastSeenMessageCount = state.chatMessages.size
        }
    }

    val hasUnreadMessages = !showChatSheet && state.chatMessages.size > lastSeenMessageCount

    val screenScope = rememberCoroutineScope()
    var toastNotification by remember { mutableStateOf<String?>(null) }
    var toastIsError by remember { mutableStateOf(false) }

    val showToast: (String, Boolean) -> Unit = { msg, isErr ->
        toastIsError = isErr
        toastNotification = msg
        screenScope.launch {
            delay(2800.milliseconds)
            if (toastNotification == msg) {
                toastNotification = null
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            if (err.isNotBlank()) {
                showToast(err, true)
                viewModel.clearError()
            }
        }
    }

    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
    ) {
        val wide = maxWidth >= TWO_COLUMN_MIN_DP.dp

        val copyCode: () -> Unit = { state.roomCode?.let { clipboard.setText(AnnotatedString(it)) } }
        val shareTitle = stringResource(Res.string.listen_together)
        val shareCode: () -> Unit = {
            state.roomCode?.let { shareUrl(title = shareTitle, url = SHARE_PREFIX + it) }
        }
        val openSettings: () -> Unit = { navController.navigate(ListenTogetherSettingsDestination) }

        val bg = MaterialTheme.colorScheme.background
        val glow =
            if (bg.luminance() > 0.5f) {
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
            } else {
                MaterialTheme.colorScheme.primary.rgbFactor(0.3f)
            }

        // Ambient background glow
        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            Box(Modifier.matchParentSize().background(bg))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .angledGradientBackground(listOf(glow, bg), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(bg)),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            // Sleek Top Nav Bar (Back, Jam Header, Chat button with Instagram dot, Settings)
            JamTopBar(
                inRoom = state.inRoom,
                hasUnreadMessages = hasUnreadMessages,
                backdrop = backdrop,
                onBack = { navController.navigateUp() },
                onChatClick = {
                    lastSeenMessageCount = state.chatMessages.size
                    showChatSheet = true
                },
                onSettingsClick = openSettings,
            )

            if (wide) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Column(
                        modifier =
                            Modifier
                                .weight(0.42f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        TitleBlock(inRoom = state.inRoom)
                        AnimatedVisibility(visible = state.inRoom) {
                            RoomCodePoster(
                                state = state,
                                selfAvatar = accountThumbUrl,
                                onCopyCode = copyCode,
                                onShareCode = shareCode,
                                onLeaveJam = viewModel::leaveRoom,
                                onManageMember = { managing = it },
                                onOpenAllMembers = { showAllMembersDialog = true },
                            )
                        }
                    }

                    Column(
                        modifier =
                            Modifier
                                .weight(0.58f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 80.dp)
                                .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WorkArea(
                            state = state,
                            displayName = displayName,
                            accountThumbUrl = accountThumbUrl,
                            codeInput = codeInput,
                            viewModel = viewModel,
                            onOpenAddSongs = { showAddSongSheet = true },
                            onShowToast = showToast,
                        )
                    }
                }
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TitleBlock(inRoom = state.inRoom)
                    AnimatedVisibility(visible = state.inRoom) {
                        RoomCodePoster(
                            state = state,
                            selfAvatar = accountThumbUrl,
                            onCopyCode = copyCode,
                            onShareCode = shareCode,
                            onLeaveJam = viewModel::leaveRoom,
                            onManageMember = { managing = it },
                            onOpenAllMembers = { showAllMembersDialog = true },
                        )
                    }
                    WorkArea(
                        state = state,
                        displayName = displayName,
                        accountThumbUrl = accountThumbUrl,
                        codeInput = codeInput,
                        viewModel = viewModel,
                        onOpenAddSongs = { showAddSongSheet = true },
                        onShowToast = showToast,
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        // Floating bottom toast notification (auto-dismiss after 2.8s)
        AnimatedVisibility(
            visible = toastNotification != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 24.dp)
                    .padding(horizontal = 24.dp)
                    .zIndex(100f),
        ) {
            toastNotification?.let { msg ->
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .border(
                                1.dp,
                                if (toastIsError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                CircleShape,
                            ).shadow(8.dp, CircleShape)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (toastIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                        )
                        Text(
                            text = msg,
                            style = typo().bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }

    // Add Songs Sheet
    if (showAddSongSheet) {
        AddSongToJamSheet(
            query = searchQuery,
            isSearching = isSearching,
            searchResults = searchResults,
            recommendedSongs = recommendedSongs,
            queuedTrackIds = state.queue.map { it.id }.toSet(),
            isLoadingRecommendations = isLoadingRecommendations,
            canPlayDirect = state.isHost || state.permissions.allowPlayDirect,
            onQueryChange = viewModel::onSearchQueryChange,
            onLoadMoreRecommendations = viewModel::loadMoreRecommendations,
            onQueueTrack = viewModel::addSongToJam,
            onQueueSongResult = viewModel::addSongToJam,
            onPlayTrack = { track ->
                viewModel.playDirectInJam(track)
                showAddSongSheet = false
                viewModel.onSearchQueryChange("")
            },
            onPlaySongResult = { song ->
                viewModel.playDirectInJam(song)
                showAddSongSheet = false
                viewModel.onSearchQueryChange("")
            },
            onDismiss = {
                showAddSongSheet = false
                viewModel.onSearchQueryChange("")
            },
        )
    }

    // In-Jam Messenger-Style Chat Sheet
    if (showChatSheet) {
        JamChatSheet(
            state = state,
            selfUserId = state.selfUserId,
            selfAvatar = accountThumbUrl,
            onSendMessage = { text, replyToId, replyToText, replyToSenderName ->
                viewModel.sendChatMessage(text, replyToId, replyToText, replyToSenderName)
            },
            onReact = { msgId, emoji ->
                viewModel.reactToMessage(msgId, emoji)
            },
            onDismiss = { showChatSheet = false },
        )
    }

    // All Members Dialog (when tapping "..." beside room code)
    if (showAllMembersDialog) {
        AllMembersDialog(
            members = state.members,
            selfId = state.selfUserId,
            selfAvatar = accountThumbUrl,
            canManage = state.isHost,
            onManage = { member ->
                showAllMembersDialog = false
                managing = member
            },
            onDismiss = { showAllMembersDialog = false },
        )
    }

    // Member Action Menu Dialog
    managing?.let { member ->
        if (member.userId != state.selfUserId) {
            EnhancedMemberActionDialog(
                member = member,
                onTransferHost = {
                    viewModel.transferHost(member.userId)
                    managing = null
                },
                onKick = {
                    viewModel.kickUser(member.userId)
                    managing = null
                },
                onBlock = {
                    viewModel.blockAndKick(member.userId, member.username)
                    managing = null
                },
                onDismiss = { managing = null },
            )
        } else {
            managing = null
        }
    }
}

@Composable
private fun JamTopBar(
    inRoom: Boolean,
    hasUnreadMessages: Boolean,
    backdrop: PlatformBackdrop,
    onBack: () -> Unit,
    onChatClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        LiquidGlassIconButton(
            backdrop = backdrop,
            imageVector = SimpIcons.ArrowBackIosNew,
            tint = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.size(42.dp),
            onClick = onBack,
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (inRoom) {
                // In-Jam Chat Button with Instagram-Style Unread Small Red Dot
                Box {
                    LiquidGlassIconButton(
                        backdrop = backdrop,
                        imageVector = SimpIcons.Chat,
                        tint = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(42.dp),
                        onClick = onChatClick,
                    )
                    if (hasUnreadMessages) {
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-3).dp, y = 3.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                                    .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                        )
                    }
                }
            }

            LiquidGlassIconButton(
                backdrop = backdrop,
                imageVector = SimpIcons.Settings,
                tint = MaterialTheme.colorScheme.onSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(42.dp),
                onClick = onSettingsClick,
            )
        }
    }
}

@Composable
private fun ColumnScope.WorkArea(
    state: ListenTogetherRoom,
    displayName: String,
    accountThumbUrl: String?,
    codeInput: String,
    viewModel: ListenTogetherViewModel,
    onOpenAddSongs: () -> Unit,
    onShowToast: (String, Boolean) -> Unit,
) {
    when {
        state.inRoom -> {
            val jamAutoplay by viewModel.jamAutoplay.collectAsStateWithLifecycle()

            // 1. Now Playing in Jam (Mini player with duration line, previous, play/pause, next)
            JamNowPlayingCard(
                track = state.currentTrack,
                isPlaying = state.isPlaying,
                canControlPlayback = state.isHost || state.permissions.allowPlayPause,
                onPrevious = viewModel::skipPrevious,
                onTogglePlayPause = viewModel::togglePlayPause,
                onNext = viewModel::skipNext,
            )

            AnimatedVisibility(visible = state.waitingFor.isNotEmpty()) {
                BufferBanner(state.waitingForNames)
            }
            AnimatedVisibility(visible = state.isHost && state.joinRequests.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    JoinRequests(state.joinRequests, viewModel::approveJoin, viewModel::rejectJoin)
                }
            }
            AnimatedVisibility(visible = state.isHost && state.suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Suggestions(state.suggestions, viewModel::approveSuggestion, viewModel::rejectSuggestion)
                }
            }

            // 2. Jam Queue Section (with swipe to re-queue / remove)
            JamQueueSection(
                queue = state.queue,
                isHost = state.isHost,
                jamAutoplay = jamAutoplay,
                canQueue = state.isHost || state.permissions.allowQueue,
                canReorder = state.isHost || state.permissions.allowReorder,
                onAddClick = onOpenAddSongs,
                onToggleAutoplay = { viewModel.setJamAutoplay(!jamAutoplay) },
                onReorder = { from, to -> viewModel.reorderJamQueue(from, to) },
                onRequeue = { track ->
                    viewModel.addSongToJam(track)
                    onShowToast("Re-queued \"${track.title}\" to Jam", false)
                },
                onRemove = { idx, track ->
                    viewModel.removeSongFromJam(idx)
                    onShowToast("Removed \"${track.title}\" from queue", false)
                },
            )
        }

        state.pendingJoinCode != null -> {
            WaitingForApproval(state.pendingJoinCode.orEmpty(), viewModel::cancelJoin)
        }

        else -> {
            JamCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Avatar(
                        name = initialOf(displayName.ifBlank { "You" }),
                        background = tintFor("self"),
                        size = 48.dp,
                        imageUrl = accountThumbUrl,
                    )
                    Column(Modifier.weight(1f)) {
                        NameField(displayName, viewModel::onDisplayNameChange)
                    }
                }
            }

            JamCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PrimaryButton(
                        text = stringResource(Res.string.lt_create_room),
                        enabled = displayName.isNotBlank(),
                        onClick = viewModel::createRoom,
                    )
                    DividerLabel(stringResource(Res.string.lt_or_join_with_code))
                    CodeInput(codeInput, viewModel::onRoomCodeChange)
                    SecondaryButton(
                        text = stringResource(Res.string.lt_join_room),
                        enabled = displayName.isNotBlank() && codeInput.length == ListenTogetherViewModel.ROOM_CODE_MIN_LENGTH,
                        onClick = viewModel::joinRoom,
                    )
                }
            }

            JamCard(tint = MaterialTheme.colorScheme.primary) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Jam in SimpMusic",
                        style = typo().titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(Res.string.lt_background_warning),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun JamNowPlayingCard(
    track: RoomTrack?,
    isPlaying: Boolean,
    canControlPlayback: Boolean,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
) {
    JamCard(tint = MaterialTheme.colorScheme.primary) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Album Art
                Box(
                    modifier =
                        Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    if (track?.thumbnail.isNullOrBlank()) {
                        Icon(
                            SimpIcons.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp).align(Alignment.Center),
                        )
                    } else {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(track.thumbnail)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                // Track Info
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track?.title ?: "No track currently playing",
                        style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track?.artist.orEmpty().ifBlank { "Jam session is active" },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Controls: Previous, Play/Pause, Next
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = canControlPlayback && track != null,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipPrevious,
                            contentDescription = "Previous Track",
                            tint = if (canControlPlayback && track != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        enabled = canControlPlayback && track != null,
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canControlPlayback && track != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                ),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (canControlPlayback && track != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        enabled = canControlPlayback && track != null,
                        modifier = Modifier.size(34.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.SkipNext,
                            contentDescription = "Next Track",
                            tint = if (canControlPlayback && track != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun JamQueueSection(
    queue: List<RoomTrack>,
    canQueue: Boolean,
    canReorder: Boolean,
    onAddClick: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    onRequeue: (RoomTrack) -> Unit,
    onRemove: (Int, RoomTrack) -> Unit,
    modifier: Modifier = Modifier,
    isHost: Boolean = false,
    jamAutoplay: Boolean = true,
    onToggleAutoplay: () -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader("Jam Queue", queue.size)
            if (isHost) {
                Surface(
                    onClick = onToggleAutoplay,
                    shape = CircleShape,
                    color = if (jamAutoplay) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            "Autoplay",
                            style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                            color = if (jamAutoplay) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (jamAutoplay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                        )
                    }
                }
            }
        }
        if (canQueue) {
            FilledTonalButton(
                onClick = onAddClick,
                shape = CircleShape,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Icon(SimpIcons.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Add songs", style = typo().labelMedium.copy(fontWeight = FontWeight.SemiBold))
            }
        }
    }

    if (queue.isEmpty()) {
        JamCard {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        SimpIcons.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(28.dp),
                    )
                    Text(
                        "Queue is empty",
                        style = typo().bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (canQueue) "Anyone in the Jam can queue up songs!" else "Only the host can add songs to the queue",
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    } else {
        val lazyListState = rememberLazyListState()
        val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
            onReorder(from.index, to.index)
        }
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(queue, key = { index, track -> track.id + "_$index" }) { index, track ->
                ReorderableItem(reorderableState, key = track.id + "_$index") { _ ->
                    SwipeableQueueCard(
                        track = track,
                        canReorder = canReorder,
                        onRemove = { onRemove(index, track) },
                        onRequeue = { onRequeue(track) },
                        dragModifier = Modifier.draggableHandle(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableQueueCard(
    track: RoomTrack,
    canReorder: Boolean,
    onRemove: () -> Unit,
    onRequeue: () -> Unit,
    modifier: Modifier = Modifier,
    dragModifier: Modifier = Modifier,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val swipeThreshold = 180f
    val maxDrag = 280f

    val isDraggingRight = offsetX.value > 0f
    val isDraggingLeft = offsetX.value < 0f
    val progressRight = (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
    val progressLeft = (-offsetX.value / swipeThreshold).coerceIn(0f, 1f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE),
    ) {
        // Background reveal
        when {
            isDraggingRight -> {
                // Primary background on left for Re-Queue
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = progressRight * 0.95f))
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.scale(0.7f + progressRight * 0.3f),
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onPrimary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(SimpIcons.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            if (offsetX.value >= swipeThreshold) "Release to Re-queue" else "Re-queue",
                            style = typo().labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
            isDraggingLeft -> {
                // Red background on right for Remove from Queue
                Box(
                    modifier =
                        Modifier
                            .matchParentSize()
                            .background(Color(0xFFE53935).copy(alpha = progressLeft * 0.95f))
                            .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.scale(0.7f + progressLeft * 0.3f),
                    ) {
                        Text(
                            if (-offsetX.value >= swipeThreshold) "Release to Remove" else "Remove",
                            style = typo().labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(SimpIcons.Delete, contentDescription = null, tint = Color(0xFFE53935), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Foreground card
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                    .clip(CARD_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f), CARD_SHAPE)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (offsetX.value >= swipeThreshold) {
                                    onRequeue()
                                } else if (offsetX.value <= -swipeThreshold) {
                                    onRemove()
                                }
                                scope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec =
                                            spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                            ),
                                    )
                                }
                            },
                            onDragCancel = {
                                scope.launch { offsetX.animateTo(0f, spring()) }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                scope.launch {
                                    val next = (offsetX.value + dragAmount).coerceIn(-maxDrag, maxDrag)
                                    offsetX.snapTo(next)
                                }
                            },
                        )
                    },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))) {
                    if (track.thumbnail.isNotBlank()) {
                        AsyncImage(
                            model =
                                ImageRequest
                                    .Builder(LocalPlatformContext.current)
                                    .data(track.thumbnail)
                                    .crossfade(true)
                                    .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            SimpIcons.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp).align(Alignment.Center),
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist.ifBlank { "Unknown Artist" },
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (canReorder) {
                    Icon(
                        SimpIcons.DragHandle,
                        contentDescription = "Reorder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp).then(dragModifier),
                    )
                }
            }
        }
    }
}

@Composable
private fun QueuedBadge() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier =
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Icon(
            SimpIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp),
        )
        Text(
            "Queued",
            style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSongToJamSheet(
    query: String,
    isSearching: Boolean,
    searchResults: List<SongsResult>,
    recommendedSongs: List<RoomTrack>,
    queuedTrackIds: Set<String> = emptySet(),
    isLoadingRecommendations: Boolean,
    canPlayDirect: Boolean,
    onQueryChange: (String) -> Unit,
    onLoadMoreRecommendations: () -> Unit,
    onQueueTrack: (RoomTrack) -> Unit,
    onQueueSongResult: (SongsResult) -> Unit,
    onPlayTrack: (RoomTrack) -> Unit,
    onPlaySongResult: (SongsResult) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var queuedNotification by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun triggerQueueToast(trackTitle: String) {
        queuedNotification = "Added \"$trackTitle\" to Jam queue"
        scope.launch {
            delay(2200.milliseconds)
            if (queuedNotification?.contains(trackTitle) == true) {
                queuedNotification = null
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            "Add Songs to Jam",
                            style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (canPlayDirect) "Tap to play now • Swipe right to queue" else "Tap or swipe right to queue",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(SimpIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Search Bar
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        SimpIcons.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    "Search YouTube Music tracks…",
                                    style = typo().bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                    )
                    if (query.isNotEmpty()) {
                        Icon(
                            SimpIcons.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).clickable { onQueryChange("") },
                        )
                    }
                }

                // Content Area
                if (isSearching) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                    }
                } else if (query.isNotBlank()) {
                    // Search results
                    if (searchResults.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No songs found", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(searchResults, key = { it.videoId }) { song ->
                                val isQueued = song.videoId in queuedTrackIds
                                SongFullWidthItems(
                                    track = song.toTrack(),
                                    isPlaying = false,
                                    modifier = Modifier,
                                    rightView = if (isQueued) { { QueuedBadge() } } else null,
                                    onAddToQueue = { _ ->
                                        onQueueSongResult(song)
                                        triggerQueueToast(song.title.orEmpty())
                                    },
                                    onClickListener = { _ ->
                                        if (canPlayDirect) {
                                            onPlaySongResult(song)
                                        } else {
                                            onQueueSongResult(song)
                                            triggerQueueToast(song.title.orEmpty())
                                        }
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // Recommendations List with Endless Scroll
                    if (recommendedSongs.isEmpty() && isLoadingRecommendations) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (recommendedSongs.isEmpty()) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            Text("No recommendations found", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Text(
                                "Recommended for you",
                                style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(bottom = 6.dp),
                            )
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                itemsIndexed(recommendedSongs, key = { index, track -> "${track.id}_$index" }) { index, track ->
                                    if (index >= recommendedSongs.size - 4) {
                                        LaunchedEffect(Unit) {
                                            onLoadMoreRecommendations()
                                        }
                                    }
                                    val isQueued = track.id in queuedTrackIds
                                    SongFullWidthItems(
                                        track = track.toTrack(),
                                        isPlaying = false,
                                        modifier = Modifier,
                                        rightView = if (isQueued) { { QueuedBadge() } } else null,
                                        onAddToQueue = { _ ->
                                            onQueueTrack(track)
                                            triggerQueueToast(track.title)
                                        },
                                        onClickListener = { _ ->
                                            if (canPlayDirect) {
                                                onPlayTrack(track)
                                            } else {
                                                onQueueTrack(track)
                                                triggerQueueToast(track.title)
                                            }
                                        },
                                    )
                                }

                                if (isLoadingRecommendations) {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom toast notification when a song is queued
            androidx.compose.animation.AnimatedVisibility(
                visible = queuedNotification != null,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp)
                        .padding(horizontal = 24.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.inverseSurface)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = SimpIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = queuedNotification ?: "",
                            style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}



// ─────────────────────── In-Jam Messenger-Style Chat ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JamChatSheet(
    state: ListenTogetherRoom,
    selfUserId: String,
    selfAvatar: String?,
    onSendMessage: (text: String, replyToId: String?, replyToText: String?, replyToSenderName: String?) -> Unit,
    onReact: (messageId: String, emoji: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var messageText by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<JamChatMessage?>(null) }
    var reactingMessageId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.size - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
        ) {
            // Chat Header with members avatars
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "Jam Chat",
                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    // Mini avatar stack of participants
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy((-8).dp),
                    ) {
                        items(state.members) { m ->
                            Avatar(
                                name = initialOf(m.username),
                                background = tintFor(m.userId),
                                size = 26.dp,
                                imageUrl = m.avatarUrl ?: if (m.userId == selfUserId) selfAvatar else null,
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(SimpIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Message stream
            if (state.chatMessages.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("👋 Welcome to Jam Chat!", style = typo().titleSmall, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            "Swipe right to reply • Long-press to react with emoji",
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                ) {
                    items(state.chatMessages, key = { it.id }) { message ->
                        val isSelf = message.senderId == selfUserId
                        val senderAvatarUrl =
                            if (isSelf) {
                                selfAvatar
                            } else {
                                state.members.firstOrNull { it.userId == message.senderId }?.avatarUrl ?: message.senderAvatar
                            }

                        ChatMessageBubble(
                            message = message,
                            isSelf = isSelf,
                            senderAvatar = senderAvatarUrl,
                            onReply = { replyTarget = message },
                            onLongPress = { reactingMessageId = message.id },
                            onReact = { emoji -> onReact(message.id, emoji) },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            // Emoji Reaction Bar (if active)
            reactingMessageId?.let { targetId ->
                EmojiReactionSelector(
                    onSelect = { emoji ->
                        onReact(targetId, emoji)
                        reactingMessageId = null
                    },
                )
            }

            // Reply Context Preview Bar
            replyTarget?.let { reply ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Replying to ${reply.senderName}",
                            style = typo().labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            reply.text,
                            style = typo().bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = { replyTarget = null }, modifier = Modifier.size(24.dp)) {
                        Icon(SimpIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // Message Composer
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    singleLine = true,
                    textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions =
                        KeyboardActions(onSend = {
                            if (messageText.isNotBlank()) {
                                onSendMessage(
                                    messageText,
                                    replyTarget?.id,
                                    replyTarget?.text,
                                    replyTarget?.senderName,
                                )
                                messageText = ""
                                replyTarget = null
                            }
                        }),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (messageText.isEmpty()) {
                            Text("Send a message…", style = typo().bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        inner()
                    },
                )
                IconButton(
                    onClick = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(
                                messageText,
                                replyTarget?.id,
                                replyTarget?.text,
                                replyTarget?.senderName,
                            )
                            messageText = ""
                            replyTarget = null
                        }
                    },
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(SimpIcons.Send, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: JamChatMessage,
    isSelf: Boolean,
    senderAvatar: String?,
    onReply: () -> Unit,
    onLongPress: () -> Unit,
    onReact: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX.value > 60f) {
                                onReply()
                            }
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring()) }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val next = (offsetX.value + dragAmount).coerceIn(0f, 100f)
                                offsetX.snapTo(next)
                            }
                        },
                    )
                },
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom,
    ) {
        if (!isSelf) {
            // Google Profile Photo on each message like Facebook Messenger
            Avatar(
                name = initialOf(message.senderName),
                background = tintFor(message.senderId),
                size = 32.dp,
                imageUrl = senderAvatar,
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isSelf) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            if (!isSelf) {
                // Name above message
                Text(
                    text = message.senderName,
                    style = typo().labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = tintFor(message.senderId),
                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Message Bubble
            Box(
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = if (isSelf) 18.dp else 4.dp,
                                topEnd = if (isSelf) 4.dp else 18.dp,
                                bottomStart = 18.dp,
                                bottomEnd = 18.dp,
                            ),
                        ).combinedClickable(
                            onClick = onReply,
                            onLongClick = onLongPress,
                        ).background(
                            if (isSelf) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ).padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Quoted reply snippet
                    if (!message.replyToText.isNullOrBlank()) {
                        Box(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelf) Color.Black.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background.copy(alpha = 0.35f),
                                    ).padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Column {
                                Text(
                                    message.replyToSenderName ?: "Reply",
                                    style = typo().labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    message.replyToText.orEmpty(),
                                    style = typo().bodySmall,
                                    color = if (isSelf) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    Text(
                        text = message.text,
                        style = typo().bodyMedium,
                        color = if (isSelf) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Reactions chip list
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    message.reactions.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = typo().bodySmall.copy(fontSize = 13.sp),
                            modifier = Modifier.clickable { onReact(emoji) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmojiReactionSelector(
    onSelect: (String) -> Unit,
) {
    val emojis = listOf("❤️", "👍", "🔥", "😂", "😮", "😢", "🎉")
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        emojis.forEach { emoji ->
            Text(
                text = emoji,
                style = typo().titleMedium.copy(fontSize = 22.sp),
                modifier =
                    Modifier
                        .clip(CircleShape)
                        .clickable { onSelect(emoji) }
                        .padding(4.dp),
            )
        }
    }
}

// ─────────────────────── Enhanced Member Action Menu ───────────────────────

@Composable
private fun EnhancedMemberActionDialog(
    member: RoomMember,
    onTransferHost: () -> Unit,
    onKick: () -> Unit,
    onBlock: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .width(340.dp)
                    .clip(CARD_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CARD_SHAPE)
                    .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Avatar(
                    name = initialOf(member.username),
                    background = tintFor(member.userId),
                    size = 48.dp,
                    imageUrl = member.avatarUrl,
                )
                Column {
                    Text(
                        member.username,
                        style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        if (member.isHost) "Room Host" else "Participant",
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EnhancedDialogAction(
                    title = "Make host",
                    subtitle = "They will control playback",
                    tint = MaterialTheme.colorScheme.onSurface,
                    onClick = onTransferHost,
                )
                EnhancedDialogAction(
                    title = "Remove from Jam",
                    subtitle = "They can rejoin with the code",
                    tint = Color(0xFFEF9A9A),
                    onClick = onKick,
                )
                EnhancedDialogAction(
                    title = "Block permanently",
                    subtitle = "They cannot rejoin, even with the code",
                    tint = Color(0xFFFF8A80),
                    onClick = onBlock,
                )
            }
        }
    }
}

@Composable
private fun EnhancedDialogAction(
    title: String,
    subtitle: String,
    tint: Color,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(ROW_SHAPE)
                .background(tint.copy(alpha = 0.05f))
                .border(1.dp, tint.copy(alpha = 0.28f), ROW_SHAPE)
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(title, style = typo().bodyMedium.copy(fontWeight = FontWeight.Bold), color = tint)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}



@Composable
private fun JamCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(
                    tint?.copy(alpha = 0.08f) ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f),
                ).border(
                    1.dp,
                    tint?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                    CARD_SHAPE,
                ),
    ) { content() }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
) {
    Row(
        modifier = Modifier.padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = typo().titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground)
        Box(
            Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        ) {
            Text("$count", style = typo().labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Avatar(
    name: String,
    background: Color,
    size: Dp = 40.dp,
    imageUrl: String? = null,
) {
    if (!imageUrl.isNullOrBlank()) {
        Box(
            modifier =
                Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(background.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(imageUrl)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .crossfade(true)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
        }
    } else {
        Box(
            modifier = Modifier.size(size).clip(CircleShape).background(background.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name,
                style = typo().titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = (size.value * 0.42f).sp),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun GlyphButton(
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ActionGlyph(
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                    },
                ).clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
            color =
                if (enabled) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.35f else 0.15f), CircleShape)
                .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
private fun DividerLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
        Text(text, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)))
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.lt_display_name), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = typo().bodyMedium.copy(color = MaterialTheme.colorScheme.onBackground),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(ROW_SHAPE)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)),
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) { inner() }
            },
        )
        Text(stringResource(Res.string.lt_display_name_hint), style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val fieldValue = TextFieldValue(text = code, selection = TextRange(code.length))
    val displaySlots = 6

    BasicTextField(
        value = fieldValue,
        onValueChange = { onCodeChange(it.text) },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(Modifier.size(0.dp)) { innerTextField() }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                repeat(displaySlots) { index ->
                    val filled = index < code.length
                    val isCaret = index == code.length
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                                .then(
                                    if (isCaret) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp)) else Modifier,
                                ).clickable { focusRequester.requestFocus() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (filled) code[index].toString() else "",
                            style = typo().titleSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun TitleBlock(inRoom: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Jam",
            style = typo().titleLarge.copy(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (!inRoom) {
            Text(
                text = stringResource(Res.string.lt_tagline),
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RoomCodePoster(
    state: ListenTogetherRoom,
    selfAvatar: String?,
    onCopyCode: () -> Unit,
    onShareCode: () -> Unit,
    onLeaveJam: () -> Unit,
    onManageMember: (RoomMember) -> Unit,
    onOpenAllMembers: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Left: Room code
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text =
                        if (state.isHost) {
                            stringResource(Res.string.lt_room_code)
                        } else {
                            stringResource(Res.string.lt_in_room)
                        },
                    style = typo().labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text =
                        state.roomCode
                            ?.chunked(3)
                            ?.joinToString(" ")
                            .orEmpty(),
                    style =
                        typo().titleLarge.copy(
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                        ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            // Right: Up to 3 Google head circles + "..." if > 3 members
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val displayedMembers = state.members.take(3)
                val hasMore = state.members.size > 3

                displayedMembers.forEach { member ->
                    val isSelf = member.userId == state.selfUserId
                    val avatarUrl = member.avatarUrl ?: if (isSelf) selfAvatar else null
                    val displayName = member.username.ifBlank { "User" }

                    Box(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .clickable(enabled = state.isHost && !isSelf) { onManageMember(member) },
                    ) {
                        Avatar(
                            name = initialOf(displayName),
                            background = tintFor(member.userId),
                            size = 38.dp,
                            imageUrl = avatarUrl,
                        )
                        // Live status dot
                        Box(
                            modifier =
                                Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(11.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(1.5.dp),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            member.isBuffering -> MaterialTheme.colorScheme.tertiary
                                            member.isConnected -> Color(0xFF4CAF50)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    ),
                            )
                        }
                    }
                }

                if (hasMore) {
                    Box(
                        modifier =
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                                .clickable { onOpenAllMembers() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "...",
                            style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
            Text(
                text =
                    if (state.isHost) {
                        "You are the host · ${state.members.size} in Jam"
                    } else {
                        "${state.members.firstOrNull { it.isHost }?.username.orEmpty()} is controlling playback"
                    },
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Action Buttons Row: Copy & Share on left (if host), Leave Jam on rightmost
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (state.isHost) {
                var copied by remember { mutableStateOf(false) }
                LaunchedEffect(copied) {
                    if (copied) {
                        delay(COPIED_FEEDBACK_DURATION)
                        copied = false
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Crossfade(targetState = copied, label = "ltCopied") { done ->
                        GlyphButton(if (done) SimpIcons.Check else SimpIcons.ContentCopy) {
                            onCopyCode()
                            copied = true
                        }
                    }
                    GlyphButton(SimpIcons.Share, onShareCode)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }

            FilledTonalButton(
                onClick = onLeaveJam,
                shape = CircleShape,
                colors =
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Icon(SimpIcons.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(Res.string.lt_leave_room),
                    style = typo().labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun AllMembersDialog(
    members: List<RoomMember>,
    selfId: String,
    selfAvatar: String?,
    canManage: Boolean,
    onManage: (RoomMember) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .width(340.dp)
                    .clip(CARD_SHAPE)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), CARD_SHAPE)
                    .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Jam Members (${members.size})",
                    style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(SimpIcons.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(members, key = { it.userId }) { member ->
                    val isSelf = member.userId == selfId
                    val avatarUrl = member.avatarUrl ?: if (isSelf) selfAvatar else null
                    val displayName = member.username.ifBlank { "User" }

                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = canManage && !isSelf) {
                                    onDismiss()
                                    onManage(member)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Avatar(
                            name = initialOf(displayName),
                            background = tintFor(member.userId),
                            size = 38.dp,
                            imageUrl = avatarUrl,
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = if (isSelf) "$displayName (You)" else displayName,
                                style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (member.isHost) "Host" else if (member.isConnected) "Connected" else "Disconnected",
                                style = typo().bodySmall,
                                color = if (member.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (canManage && !isSelf) {
                            Icon(
                                SimpIcons.MoreVert,
                                contentDescription = "Manage",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BufferBanner(names: List<String>) {
    JamCard(tint = MaterialTheme.colorScheme.tertiary) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(
                text = if (names.isEmpty()) "Waiting for everyone" else "Waiting for ${names.joinToString(", ")}",
                style = typo().bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                "Playback resumes when everyone is ready",
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JoinRequests(
    requests: List<com.maxrave.domain.data.model.listentogether.RoomJoinRequest>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.lt_join_requests), requests.size)
    requests.forEach { request ->
        JamCard {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(
                    initialOf(request.username),
                    tintFor(request.userId),
                    40.dp,
                    imageUrl = request.avatarUrl,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        request.username,
                        style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(Res.string.lt_just_asked),
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ActionGlyph(SimpIcons.Check, MaterialTheme.colorScheme.primary) { onApprove(request.userId) }
                ActionGlyph(SimpIcons.Close, MaterialTheme.colorScheme.error) { onReject(request.userId) }
            }
        }
    }
}

@Composable
private fun Suggestions(
    suggestions: List<RoomSuggestion>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    SectionHeader(stringResource(Res.string.lt_suggestions), suggestions.size)
    suggestions.forEach { suggestion ->
        JamCard {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        suggestion.track.title,
                        style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${suggestion.fromUsername} suggested",
                        style = typo().bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                ActionGlyph(SimpIcons.Check, MaterialTheme.colorScheme.primary) { onApprove(suggestion.suggestionId) }
                ActionGlyph(SimpIcons.Close, MaterialTheme.colorScheme.error) { onReject(suggestion.suggestionId) }
            }
        }
    }
}

@Composable
private fun WaitingForApproval(
    code: String,
    onCancel: () -> Unit,
) {
    JamCard(tint = MaterialTheme.colorScheme.tertiary) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                stringResource(Res.string.lt_waiting_approval),
                style = typo().titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                code.chunked(3).joinToString("  "),
                style = typo().titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 3.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                stringResource(Res.string.lt_waiting_approval_desc),
                style = typo().bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            SecondaryButton(
                text = stringResource(Res.string.lt_cancel_join),
                enabled = true,
                onClick = onCancel,
            )
        }
    }
}

private fun RoomTrack.toTrack(): Track =
    Track(
        videoId = id,
        title = title,
        artists = if (artist.isNotBlank()) listOf(Artist(name = artist, id = null)) else null,
        album = if (album.isNotBlank()) Album(name = album, id = "") else null,
        duration = null,
        durationSeconds = (durationMs / 1000L).toInt(),
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = if (thumbnail.isNotBlank()) listOf(Thumbnail(url = thumbnail, width = 0, height = 0)) else null,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
    )