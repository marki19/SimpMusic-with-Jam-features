package com.marki19.simpmusic.viewModel.jam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marki19.domain.jam.JamCommand
import com.marki19.domain.jam.JamPermissions
import com.marki19.domain.jam.JamRepository
import com.marki19.domain.jam.JamRepeatMode
import com.marki19.domain.jam.JamSessionState
import com.maxrave.domain.repository.AccountRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.logger.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
class JamViewModel(
    private val jamRepository: JamRepository,
    private val songRepository: SongRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val sessionState: StateFlow<JamSessionState?> = jamRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val chatMessages: StateFlow<List<JamCommand.ChatMessage>> = jamRepository.chatMessages
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    var localUserId: String? = null
        private set

    // ── UI-only state ─────────────────────────────────────────────────────────

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    /** Non-null when we should show a host-transfer snackbar. */
    private val _hostTransferNotice = MutableStateFlow<String?>(null)
    val hostTransferNotice: StateFlow<String?> = _hostTransferNotice.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // ── Heartbeat ─────────────────────────────────────────────────────────────

    private var heartbeatJob: kotlinx.coroutines.Job? = null

    init {
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

    private fun syncTaste() {
        viewModelScope.launch {
            var topSongs = songRepository.getMostPlayedSongs().firstOrNull() ?: emptyList()
            if (topSongs.isEmpty()) {
                topSongs = songRepository.getRecentSong(20, 0)
            }
            if (topSongs.isEmpty()) {
                topSongs = songRepository.getLikedSongs().firstOrNull() ?: emptyList()
            }
            
            val tasteTracks = topSongs.take(20).map {
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

    fun createSession() {
        viewModelScope.launch {
            _isConnecting.value = true
            val account = accountRepository.getUsedGoogleAccount().firstOrNull()
            val userId = account?.email?.takeIf { it.isNotBlank() } ?: "User-${(1000..9999).random()}"
            val name = account?.name?.takeIf { it.isNotBlank() } ?: "Host"
            val imageUrl = account?.thumbnailUrl ?: ""
            localUserId = userId
            jamRepository.createSession(userId, name, imageUrl)
        }
    }

    fun joinSession(roomId: String) {
        viewModelScope.launch {
            _isConnecting.value = true
            val account = accountRepository.getUsedGoogleAccount().firstOrNull()
            val userId = account?.email?.takeIf { it.isNotBlank() } ?: "User-${(1000..9999).random()}"
            val name = account?.name?.takeIf { it.isNotBlank() } ?: "Guest"
            val imageUrl = account?.thumbnailUrl ?: ""
            localUserId = userId
            jamRepository.joinSession(roomId, userId, name, imageUrl)
        }
    }

    fun leaveSession() {
        viewModelScope.launch { jamRepository.leaveSession() }
    }

    // ── Permissions ───────────────────────────────────────────────────────────

    fun updatePermissions(permissions: JamPermissions) {
        viewModelScope.launch { jamRepository.updatePermissions(permissions) }
    }

    // ── Queue actions ─────────────────────────────────────────────────────────

    fun removeFromQueue(queueId: String) {
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.RemoveQueueItem(queueId)) }
    }

    fun addToQueue(videoId: String, title: String, artist: String, thumbnailUrl: String?, durationMs: Long) {
        viewModelScope.launch {
            jamRepository.sendCommand(JamCommand.AddToQueue(
                videoId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                durationMs = durationMs
            ))
        }
    };

    fun playNow(videoId: String, title: String, artist: String, thumbnailUrl: String?, durationMs: Long) {
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
        viewModelScope.launch { jamRepository.sendCommand(JamCommand.RefreshRecommendations) }
    }

    // ── Playback controls ─────────────────────────────────────────────────────

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
