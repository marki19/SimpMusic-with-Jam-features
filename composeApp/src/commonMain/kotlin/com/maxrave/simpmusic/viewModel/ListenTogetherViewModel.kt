package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPlaybackBridge
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.PlayerEvent
import com.maxrave.domain.repository.HomeRepository
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.domain.repository.SearchRepository
import com.maxrave.domain.repository.SongRepository
import com.maxrave.domain.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Screen state for Jam.
 */
class ListenTogetherViewModel(
    private val repository: ListenTogetherRepository,
    private val dataStore: DataStoreManager,
    private val searchRepository: SearchRepository,
    private val songRepository: SongRepository,
    private val homeRepository: HomeRepository,
    private val mediaPlayerHandler: MediaPlayerHandler,
    bridge: ListenTogetherPlaybackBridge,
) : ViewModel() {
    private val _accountThumbUrl = MutableStateFlow<String?>(null)
    val accountThumbUrl: StateFlow<String?> = _accountThumbUrl.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _roomCodeInput = MutableStateFlow("")
    val roomCodeInput: StateFlow<String> = _roomCodeInput.asStateFlow()

    // Jam Add Songs Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SongsResult>>(emptyList())
    val searchResults: StateFlow<List<SongsResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Personalized YouTube Recommendations state
    private val _recommendedSongs = MutableStateFlow<List<RoomTrack>>(emptyList())
    val recommendedSongs: StateFlow<List<RoomTrack>> = _recommendedSongs.asStateFlow()

    private val _isLoadingRecommendations = MutableStateFlow(false)
    val isLoadingRecommendations: StateFlow<Boolean> = _isLoadingRecommendations.asStateFlow()

    private var recommendationsContinuation: String? = null
    private var isFetchingMoreRecommendations = false

    val localLibrarySongs: StateFlow<List<SongEntity>> =
        songRepository
            .getAllSongs(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        bridge.start()
        loadRecommendations()

        // Sync Google Account profile name and picture
        viewModelScope.launch {
            dataStore.getString("AccountName").collectLatest { accountName ->
                if (!accountName.isNullOrBlank() && _displayName.value.isBlank()) {
                    _displayName.value = accountName.take(MAX_USERNAME_LENGTH)
                }
            }
        }

        // Apply host settings automatically when becoming host
        viewModelScope.launch {
            repository.room
                .map { it.inRoom && it.isHost }
                .distinctUntilChanged()
                .filter { it }
                .collectLatest {
                    delay(500)
                    val allowQueue = dataStore.getString(ListenTogetherPrefs.JAM_ALLOW_QUEUE).first()?.equals(ListenTogetherPrefs.TRUE, true) ?: true
                    val allowReorder = dataStore.getString(ListenTogetherPrefs.JAM_ALLOW_REORDER).first()?.equals(ListenTogetherPrefs.TRUE, true) ?: false
                    val allowPlayDirect = dataStore.getString(ListenTogetherPrefs.JAM_ALLOW_PLAY_DIRECT).first()?.equals(ListenTogetherPrefs.TRUE, true) ?: false
                    val allowSeek = dataStore.getString(ListenTogetherPrefs.JAM_ALLOW_SEEK).first()?.equals(ListenTogetherPrefs.TRUE, true) ?: false
                    val allowPlayPause = dataStore.getString(ListenTogetherPrefs.JAM_ALLOW_PLAY_PAUSE).first()?.equals(ListenTogetherPrefs.TRUE, true) ?: false

                    repository.updateJamPermissions(
                        allowQueue = allowQueue,
                        allowReorder = allowReorder,
                        allowPlayDirect = allowPlayDirect,
                        allowSeek = allowSeek,
                        allowPlayPause = allowPlayPause
                    )
                }
        }
        viewModelScope.launch {
            dataStore.getString("AccountThumbUrl").collectLatest { thumbUrl ->
                _accountThumbUrl.value = thumbUrl
            }
        }

        // Host conveniences from settings
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_JOINS).collect {
                repository.autoApproveJoins = it == ListenTogetherPrefs.TRUE
            }
        }
        viewModelScope.launch {
            dataStore.getString(ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS).collect {
                repository.autoApproveSuggestions = it == ListenTogetherPrefs.TRUE
            }
        }
    }

    val state: StateFlow<ListenTogetherRoom> =
        repository.room.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListenTogetherRoom())

    val serverUrl: StateFlow<String> =
        dataStore
            .getString(ListenTogetherPrefs.SERVER_URL)
            .map { it.orEmpty() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    fun onDisplayNameChange(value: String) {
        _displayName.value = value.take(MAX_USERNAME_LENGTH)
    }

    fun onRoomCodeChange(value: String) {
        _roomCodeInput.value =
            value.uppercase().filter { it.isLetterOrDigit() }.take(ROOM_CODE_MAX_LENGTH)
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()

    fun createRoom() {
        repository.createRoom(_displayName.value, _accountThumbUrl.value)
    }

    fun joinRoom() {
        repository.joinRoom(_roomCodeInput.value, _displayName.value, _accountThumbUrl.value)
    }

    fun leaveRoom() {
        repository.leaveRoom()
        _roomCodeInput.value = ""
    }

    fun endRoom() {
        repository.endRoom()
        _roomCodeInput.value = ""
    }

    fun approveJoin(userId: String) {
        repository.approveJoin(userId)
    }

    fun rejectJoin(userId: String) {
        repository.rejectJoin(userId)
    }

    fun approveSuggestion(id: String) {
        repository.approveSuggestion(id)
    }

    fun rejectSuggestion(id: String) {
        repository.rejectSuggestion(id)
    }

    fun kickUser(userId: String) {
        repository.kickUser(userId)
    }

    fun blockAndKick(
        userId: String,
        username: String,
    ) {
        viewModelScope.launch {
            val current =
                dataStore
                    .getString(ListenTogetherPrefs.BLOCKLIST)
                    .first()
                    .orEmpty()
                    .split(ListenTogetherPrefs.BLOCKLIST_SEPARATOR)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            if (current.none { it.equals(username, ignoreCase = true) }) {
                dataStore.putString(
                    ListenTogetherPrefs.BLOCKLIST,
                    (current + username).joinToString(ListenTogetherPrefs.BLOCKLIST_SEPARATOR),
                )
            }
            repository.kickUser(userId)
        }
    }

    fun transferHost(userId: String) {
        repository.transferHost(userId)
    }

    fun play() = viewModelScope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause) }

    fun pause() = viewModelScope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause) }

    fun togglePlayPause() {
        viewModelScope.launch {
            mediaPlayerHandler.onPlayerEvent(PlayerEvent.PlayPause)
        }
    }

    fun seekTo(position: Long) = viewModelScope.launch { mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateProgress(position.toFloat())) }

    fun skipPrevious() {
        viewModelScope.launch {
            // Seek to 0 so the host publishes the seek, and all members rewind.
            mediaPlayerHandler.onPlayerEvent(PlayerEvent.UpdateProgress(0f))
        }
    }

    fun skipNext() {
        val currentQueue = repository.room.value.queue
        if (currentQueue.isNotEmpty()) {
            val nextTrack = currentQueue.first()
            repository.playTrackDirect(nextTrack)
            repository.removeQueueItem(0)
        }
    }

    fun cancelJoin() = repository.cancelJoin()

    fun clearError() = repository.clearError()

    // ─────────────────────── In-Jam Messaging ───────────────────────

    fun sendChatMessage(
        text: String,
        replyToId: String? = null,
        replyToText: String? = null,
        replyToSenderName: String? = null,
    ) {
        repository.sendChatMessage(text, replyToId, replyToText, replyToSenderName)
    }

    fun reactToMessage(
        messageId: String,
        emoji: String,
    ) {
        repository.reactToMessage(messageId, emoji)
    }

    // ─────────────────────── Jam Queue Operations ───────────────────────

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            searchRepository.getSearchDataSong(query).collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        _searchResults.value = res.data.orEmpty()
                        _isSearching.value = false
                    }
                    is Resource.Error -> {
                        _isSearching.value = false
                    }
                }
            }
        }
    }

    fun loadRecommendations() {
        viewModelScope.launch {
            _isLoadingRecommendations.value = true
            homeRepository.getHomeData(params = null, viewString = "views", songString = "Song").collectLatest { res ->
                when (res) {
                    is Resource.Success -> {
                        val pair = res.data
                        recommendationsContinuation = pair?.first
                        val homeItems = pair?.second.orEmpty()
                        val tracks =
                            homeItems.flatMap { item ->
                                item.contents.filterIsInstance<Content>().filter { !it.videoId.isNullOrBlank() }
                            }.distinctBy { it.videoId }.map { content ->
                                RoomTrack(
                                    id = content.videoId.orEmpty(),
                                    title = content.title,
                                    artist = content.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "YouTube Music" },
                                    album = content.album?.name.orEmpty(),
                                    durationMs = (content.durationSeconds ?: 0).toLong() * 1000L,
                                    thumbnail = content.thumbnails.lastOrNull()?.url.orEmpty(),
                                )
                            }
                        if (tracks.isNotEmpty()) {
                            _recommendedSongs.value = tracks
                        } else {
                            _recommendedSongs.value = localLibrarySongs.value.map { it.toRoomTrack() }
                        }
                        _isLoadingRecommendations.value = false
                    }
                    is Resource.Error -> {
                        if (_recommendedSongs.value.isEmpty()) {
                            _recommendedSongs.value = localLibrarySongs.value.map { it.toRoomTrack() }
                        }
                        _isLoadingRecommendations.value = false
                    }
                }
            }
        }
    }

    fun loadMoreRecommendations() {
        val continuation = recommendationsContinuation ?: return
        if (isFetchingMoreRecommendations) return
        isFetchingMoreRecommendations = true
        viewModelScope.launch {
            homeRepository.getHomeDataContinue(continueParam = continuation, viewString = "views", songString = "Song").collectLatest { res ->
                if (res is Resource.Success) {
                    val pair = res.data
                    recommendationsContinuation = pair?.first
                    val homeItems = pair?.second.orEmpty()
                    val newTracks =
                        homeItems.flatMap { item ->
                            item.contents.filterIsInstance<Content>().filter { !it.videoId.isNullOrBlank() }
                        }.distinctBy { it.videoId }.map { content ->
                            RoomTrack(
                                id = content.videoId.orEmpty(),
                                title = content.title,
                                artist = content.artists?.joinToString(", ") { it.name }.orEmpty().ifBlank { "YouTube Music" },
                                album = content.album?.name.orEmpty(),
                                durationMs = (content.durationSeconds ?: 0).toLong() * 1000L,
                                thumbnail = content.thumbnails.lastOrNull()?.url.orEmpty(),
                            )
                        }
                    val current = _recommendedSongs.value
                    _recommendedSongs.value = (current + newTracks).distinctBy { it.id }
                }
                isFetchingMoreRecommendations = false
            }
        }
    }

    fun addSongToJam(track: RoomTrack) {
        repository.addToQueue(track)
    }

    fun addSongToJam(song: SongsResult) {
        val durationMs =
            (song.durationSeconds?.toLong()?.times(1000L))
                ?: (song.duration?.let { parseDurationToMs(it) } ?: 0L)
        val track =
            RoomTrack(
                id = song.videoId,
                title = song.title.orEmpty(),
                artist = song.artists?.joinToString(", ") { it.name }.orEmpty(),
                album = song.album?.name.orEmpty(),
                durationMs = durationMs,
                thumbnail = song.thumbnails?.lastOrNull()?.url.orEmpty(),
            )
        repository.addToQueue(track)
    }

    fun addSongToJam(song: SongEntity) {
        repository.addToQueue(song.toRoomTrack())
    }

    fun playDirectInJam(track: RoomTrack) {
        val currentRoom = state.value
        val canControl = currentRoom.isHost || currentRoom.permissions.allowPlayDirect
        if (!canControl) return

        if (currentRoom.isHost) {
            viewModelScope.launch {
                mediaPlayerHandler.loadMediaItem(track, "add_songs", null)
            }
        } else {
            repository.playTrackDirect(track)
        }
    }

    fun playDirectInJam(song: SongsResult) {
        val currentRoom = state.value
        val canControl = currentRoom.isHost || currentRoom.permissions.allowPlayDirect
        if (!canControl) return

        if (currentRoom.isHost) {
            viewModelScope.launch {
                mediaPlayerHandler.loadMediaItem(song, "add_songs", null)
            }
        } else {
            val durationMs =
                (song.durationSeconds?.toLong()?.times(1000L))
                    ?: (song.duration?.let { parseDurationToMs(it) } ?: 0L)
            val track =
                RoomTrack(
                    id = song.videoId,
                    title = song.title.orEmpty(),
                    artist = song.artists?.joinToString(", ") { it.name }.orEmpty(),
                    album = song.album?.name.orEmpty(),
                    durationMs = durationMs,
                    thumbnail = song.thumbnails?.lastOrNull()?.url.orEmpty(),
                )
            repository.playTrackDirect(track)
        }
    }

    fun playDirectInJam(song: SongEntity) {
        val currentRoom = state.value
        val canControl = currentRoom.isHost || currentRoom.permissions.allowPlayDirect
        if (!canControl) return

        if (currentRoom.isHost) {
            viewModelScope.launch {
                mediaPlayerHandler.loadMediaItem(song, "add_songs", null)
            }
        } else {
            repository.playTrackDirect(song.toRoomTrack())
        }
    }

    private fun SongEntity.toRoomTrack(): RoomTrack =
        RoomTrack(
            id = videoId,
            title = title,
            artist = artistName?.joinToString(", ").orEmpty(),
            album = albumName.orEmpty(),
            durationMs = durationSeconds.toLong() * 1000L,
            thumbnail = thumbnails.orEmpty(),
        )

    fun reorderJamQueue(
        fromIndex: Int,
        toIndex: Int,
    ) {
        repository.reorderQueue(fromIndex, toIndex)
    }

    fun removeSongFromJam(index: Int) {
        repository.removeQueueItem(index)
    }

    private fun parseDurationToMs(durationStr: String): Long {
        return try {
            val parts = durationStr.split(":").map { it.trim().toLong() }
            when (parts.size) {
                2 -> (parts[0] * 60 + parts[1]) * 1000L
                3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
                else -> 0L
            }
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        const val ROOM_CODE_MIN_LENGTH = 6
        const val ROOM_CODE_MAX_LENGTH = 6
        private const val MAX_USERNAME_LENGTH = 50
    }
}
