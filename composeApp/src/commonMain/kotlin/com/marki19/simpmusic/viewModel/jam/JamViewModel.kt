package com.marki19.simpmusic.viewModel.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marki19.domain.jam.cleanId
import com.marki19.domain.jam.JamCommand
import com.marki19.domain.jam.JamPermissions
import com.marki19.domain.jam.JamRepository
import com.marki19.domain.jam.JamRepeatMode
import com.marki19.domain.jam.JamSessionState
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.SongRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.minutes
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.utils.toTrack
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.searchResult.songs.Artist
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
class JamViewModel(
    private val jamRepository: JamRepository,
    private val songRepository: SongRepository,
    private val accountRepository: AccountRepository,
    private val dataStoreManager: DataStoreManager,
    private val mediaPlayerHandler: MediaPlayerHandler,
) : ViewModel() {

    val sessionState: StateFlow<JamSessionState?> = jamRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val chatMessages: StateFlow<List<JamCommand.ChatMessage>> = jamRepository.chatMessages
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private var _localUserId: String? = null
    val localUserId: String?
        get() = _localUserId

    // ── UI-only state ─────────────────────────────────────────────────────────

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    /** Non-null when we should show a host-transfer snackbar. */
    private val _hostTransferNotice = MutableStateFlow<String?>(null)
    val hostTransferNotice: StateFlow<String?> = _hostTransferNotice.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    private var isChatSheetOpen = false

    // Cache initial track for JamSessionScreen to show while server state populates
    private var _initialTrack: Track? = null
    val initialTrack: Track?
        get() = _initialTrack

    fun setChatSheetOpen(isOpen: Boolean) {
        isChatSheetOpen = isOpen
        if (isOpen) {
            _unreadChatCount.value = 0
        }
    }

    fun resetUnreadChatCount() {
        _unreadChatCount.value = 0
    }

    private var heartbeatJob: kotlinx.coroutines.Job? = null

    private var pendingOutgoingSongId: String? = null

    init {
        viewModelScope.launch {
            var lastCount = 0
            chatMessages.collect { messages ->
                if (messages.size > lastCount) {
                    val newMessages = messages.drop(lastCount)
                    if (!isChatSheetOpen) {
                        val hasOtherSender = newMessages.any { it.senderId != _localUserId }
                        if (hasOtherSender) {
                            _unreadChatCount.value += newMessages.count { it.senderId != _localUserId }
                        }
                    }
                }
                lastCount = messages.size
            }
        }

        // Interim client fix for Bug A: strip auto-inserted outgoing song if server re-inserted it
        viewModelScope.launch {
            sessionState.collect { state ->
                val staleId = pendingOutgoingSongId ?: return@collect
                val session = state ?: return@collect
                val autoInserted = session.playbackState.queue.firstOrNull {
                    it.videoId.cleanId() == staleId
                } ?: return@collect
                jamRepository.sendCommand(JamCommand.RemoveQueueItem(autoInserted.queueId, autoInserted.videoId))
                pendingOutgoingSongId = null
            }
        }

        viewModelScope.launch {
            var previousState: JamSessionState? = null
            sessionState.collect { state ->
                _isConnecting.value = state == null && _isConnecting.value
                if (state != null) {
                    _isConnecting.value = false
                    _isSyncing.value = state.isSyncing
                    if (state.newHostNotice != null) {
                        _hostTransferNotice.value = state.newHostNotice
                    }
                    if (previousState == null) {
                        syncTaste()
                        startHeartbeat()
                    }
                } else {
                    stopHeartbeat()
                    _hostTransferNotice.value = null
                }
                previousState = state
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(5.minutes) // 5 minutes
                jamRepository.sendCommand(JamCommand.Ping)
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    fun dismissHostTransferNotice() {
        _hostTransferNotice.value = null
    }

    // ── Taste sharing ─────────────────────────────────────────────────────────

    private fun syncTaste(shuffle: Boolean = false) {
        viewModelScope.launch {
            var topSongs = songRepository.getMostPlayedSongs().firstOrNull() ?: emptyList()
            if (topSongs.isEmpty()) {
                topSongs = songRepository.getRecentSong(100, 0)
            }
            if (topSongs.isEmpty()) {
                topSongs = songRepository.getLikedSongs().firstOrNull() ?: emptyList()
            }
            
            val listToTake = if (shuffle) topSongs.shuffled() else topSongs
            val tasteTracks = listToTake.take(20).map {
                com.marki19.domain.jam.JamCommand.TasteTrack(
                    videoId = it.videoId,
                    title = it.title,
                    artist = it.artistsName,
                    thumbnailUrl = it.thumbnailUrl,
                    durationMs = it.durationSeconds.toLong() * 1000L
                )
            }
            
            if (tasteTracks.isNotEmpty()) {
                jamRepository.sendCommand(JamCommand.ShareTaste(tasteTracks))
            } else {
                // Fallback to some default popular tracks if the user has a completely empty history
                jamRepository.sendCommand(JamCommand.ShareTaste(listOf(
                    com.marki19.domain.jam.JamCommand.TasteTrack("dQw4w9WgXcQ", "Never Gonna Give You Up", "Rick Astley", null, 212000),
                    com.marki19.domain.jam.JamCommand.TasteTrack("kJQP7kiw5Fk", "Despacito", "Luis Fonsi", null, 281000),
                    com.marki19.domain.jam.JamCommand.TasteTrack("fJ9rUzIMcZQ", "Bohemian Rhapsody", "Queen", null, 359000)
                )))
            }
        }
    }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun cancelConnection() {
        _isConnecting.value = false
        viewModelScope.launch {
            jamRepository.leaveSession()
        }
    }

    fun createSession(
        initialVideoId: String? = null,
        initialTitle: String? = null,
        initialArtist: String? = null,
        initialThumbnailUrl: String? = null,
        initialDurationMs: Long? = null
    ) {
        viewModelScope.launch {
            _isConnecting.value = true
            try {
            val account = accountRepository.getUsedGoogleAccount().firstOrNull()
            val dsName = dataStoreManager.getString("AccountName").firstOrNull()
            val dsThumb = dataStoreManager.getString("AccountThumbUrl").firstOrNull()

            val userId = account?.email?.takeIf { it.isNotBlank() } ?: "User-${(1000..9999).random()}"
            val name = dsName?.takeIf { it.isNotBlank() } ?: account?.name?.takeIf { it.isNotBlank() } ?: "Host"
            var rawImageUrl = dsThumb?.takeIf { it.isNotBlank() } ?: account?.thumbnailUrl ?: ""

            if (rawImageUrl.isBlank() && account?.netscapeCookie != null) {
                val accountInfoList = accountRepository.getAccountInfo(account.netscapeCookie!!).firstOrNull()
                rawImageUrl = accountInfoList?.firstOrNull()?.thumbnails?.lastOrNull()?.url ?: ""
                if (rawImageUrl.isNotBlank()) {
                    dataStoreManager.putString("AccountThumbUrl", rawImageUrl)
                }
            }

            val imageUrl = if (rawImageUrl.startsWith("//")) "https:$rawImageUrl" else rawImageUrl

            _localUserId = userId
            jamRepository.createSession(userId, name, imageUrl)
            syncTaste()

            val activeTrack = mediaPlayerHandler.nowPlayingState.value.track
            val fallbackMediaItem = mediaPlayerHandler.nowPlaying.value

            val effectiveVideoId = initialVideoId ?: activeTrack?.videoId ?: fallbackMediaItem?.mediaId
            val effectiveTitle = initialTitle ?: activeTrack?.title ?: fallbackMediaItem?.metadata?.title?.toString()
            val effectiveArtist = initialArtist ?: activeTrack?.artists?.firstOrNull()?.name ?: fallbackMediaItem?.metadata?.artist?.toString()
            val effectiveThumbnailUrl = initialThumbnailUrl ?: activeTrack?.thumbnails?.lastOrNull()?.url ?: fallbackMediaItem?.metadata?.artworkUri?.toString()
            val effectiveDurationMs = initialDurationMs ?: ((activeTrack?.durationSeconds ?: 0) * 1000L)

            // Cache initial track from effective metadata so JamSessionScreen displays it instantly
            if (!effectiveVideoId.isNullOrBlank()) {
                _initialTrack = Track(
                    album = null,
                    artists = if (!effectiveArtist.isNullOrBlank()) listOf(Artist(name = effectiveArtist, id = null)) else emptyList(),
                    duration = "",
                    durationSeconds = ((effectiveDurationMs ?: 0L) / 1000L).toInt(),
                    isAvailable = true,
                    isExplicit = false,
                    likeStatus = null,
                    thumbnails = if (!effectiveThumbnailUrl.isNullOrBlank()) listOf(Thumbnail(url = effectiveThumbnailUrl, width = 544, height = 544)) else emptyList(),
                    title = effectiveTitle ?: "",
                    videoId = effectiveVideoId,
                    videoType = null,
                    category = null,
                    feedbackTokens = null,
                    resultType = null,
                    year = ""
                )
            } else {
                _initialTrack = activeTrack
            }

            if (effectiveVideoId != null) {
                jamRepository.sessionState.first { it != null }
                jamRepository.sendCommand(JamCommand.PlayNow(
                    videoId = effectiveVideoId,
                    title = effectiveTitle ?: "",
                    artist = effectiveArtist ?: "",
                    thumbnailUrl = effectiveThumbnailUrl,
                    durationMs = effectiveDurationMs
                ))
            }
            } catch (e: Exception) {
                // FIX: Reset connection state if the server fails to wake up
                _isConnecting.value = false
                e.printStackTrace()
            }
        }
    }

    fun joinSession(roomId: String) {
        viewModelScope.launch {
            _isConnecting.value = true
            try {
            val account = accountRepository.getUsedGoogleAccount().firstOrNull()
            val dsName = dataStoreManager.getString("AccountName").firstOrNull()
            val dsThumb = dataStoreManager.getString("AccountThumbUrl").firstOrNull()

            val userId = account?.email?.takeIf { it.isNotBlank() } ?: "User-${(1000..9999).random()}"
            val name = dsName?.takeIf { it.isNotBlank() } ?: account?.name?.takeIf { it.isNotBlank() } ?: "Guest"
            var rawImageUrl = dsThumb?.takeIf { it.isNotBlank() } ?: account?.thumbnailUrl ?: ""
            
            if (rawImageUrl.isBlank() && account?.netscapeCookie != null) {
                val accountInfoList = accountRepository.getAccountInfo(account.netscapeCookie!!).firstOrNull()
                rawImageUrl = accountInfoList?.firstOrNull()?.thumbnails?.lastOrNull()?.url ?: ""
                if (rawImageUrl.isNotBlank()) {
                    dataStoreManager.putString("AccountThumbUrl", rawImageUrl)
                }
            }
            
            val imageUrl = if (rawImageUrl.startsWith("//")) "https:$rawImageUrl" else rawImageUrl

            _localUserId = userId
            jamRepository.joinSession(roomId, userId, name, imageUrl)
            syncTaste()
            } catch (e: Exception) {
                _isConnecting.value = false
                e.printStackTrace()
            }
        }
    }

    fun leaveSession() {
        viewModelScope.launch {
            jamRepository.leaveSession()
            _initialTrack = null
        }
    }

    suspend fun leaveSessionAndWait() {
        jamRepository.leaveSession()
        jamRepository.sessionState.first { it == null }
        _initialTrack = null
    }

    fun clearInitialTrack() {
        _initialTrack = null
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    fun updatePermissions(permissions: JamPermissions) {
        viewModelScope.launch { jamRepository.updatePermissions(permissions) }
    }

    // ── Queue actions ─────────────────────────────────────────────────────────

    fun removeFromQueue(queueId: String) {
        val targetVideoId = sessionState.value?.playbackState?.queue
            ?.find { it.queueId == queueId }?.videoId ?: ""
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.RemoveQueueItem(queueId, targetVideoId)) }
    }

    fun addToQueue(videoId: String, title: String, artist: String, thumbnailUrl: String?, durationMs: Long) {
        val currentSongId = sessionState.value?.playbackState?.currentSongId
        
        // FIX: If the Jam room is empty, adding a song should instantly establish it as the current song.
        if (currentSongId.isNullOrBlank()) {
            playNow(videoId, title, artist, thumbnailUrl, durationMs)
        } else {
            viewModelScope.launch {
                jamRepository.sendCommand(JamCommand.AddToQueue(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl,
                    durationMs = durationMs
                ))
            }
        }
    };

    fun playNow(videoId: String, title: String, artist: String, thumbnailUrl: String?, durationMs: Long) {
        pendingOutgoingSongId = sessionState.value?.playbackState?.currentSongId?.cleanId()
        viewModelScope.launch {
            jamRepository.sendCommand(JamCommand.PlayNow(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs
            ))
        }
    }
    fun moveQueueItem(queueId: String, toIndex: Int) {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.MoveQueueItem(queueId, toIndex)) }
    }

    fun voteForSong(queueId: String) {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.Vote(queueId)) }
    }

    // ── Recommendations ───────────────────────────────────────────────────────

    fun toggleRecommendations(enabled: Boolean) {
        viewModelScope.launch {
            jamRepository.sendCommand(JamCommand.EnableRecommendations(enabled))
        }
    }

    fun refreshRecommendations() {
        viewModelScope.launch {
            syncTaste(shuffle = true)
            jamRepository.sendCommand(JamCommand.RefreshRecommendations)
        }
    }

    // ── Playback controls ─────────────────────────────────────────────────────

    fun play() {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.Play) }
    }

    fun pause() {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.Pause) }
    }

    fun setShuffle(enabled: Boolean) {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.SetShuffle(enabled)) }
    }

    fun setRepeat(mode: JamRepeatMode) {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.SetRepeat(mode)) }
    }

    fun skipNext() {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.Skip(1)) }
    }

    fun skipPrevious() {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.Skip(-1)) }
    }

    // ── Chat ──────────────────────────────────────────────────────────────────

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val timestamp = io.ktor.util.date.getTimeMillis()
            val senderName = localUserId ?: if (sessionState.value?.isHost == true) "Host" else "Guest"
            jamRepository.sendCommand(JamCommand.ChatMessage(senderName, text, timestamp))
        }
    }
}
