package com.maxrave.simpmusic.viewModel.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.domain.data.entities.SongEntity
import com.maxrave.domain.data.model.browse.album.Track
import com.maxrave.domain.data.model.home.Content
import com.maxrave.domain.data.model.listentogether.RoomTrack
import com.maxrave.domain.data.model.searchResult.songs.SongsResult
import com.maxrave.domain.data.model.searchResult.songs.Thumbnail
import com.maxrave.domain.mediaservice.handler.MediaPlayerHandler
import com.maxrave.domain.mediaservice.handler.QueueData
import com.maxrave.domain.repository.ListenTogetherRepository
import com.maxrave.logger.LogLevel
import com.maxrave.logger.Logger
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import multiplatform.network.cmptoast.ToastDuration
import multiplatform.network.cmptoast.ToastGravity
import multiplatform.network.cmptoast.showToast
import org.jetbrains.compose.resources.StringResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.loading

abstract class BaseViewModel :
    androidx.lifecycle.ViewModel(),
    KoinComponent {
    protected val mediaPlayerHandler: MediaPlayerHandler by inject<MediaPlayerHandler>()
    protected val listenTogetherRepository: ListenTogetherRepository by inject<ListenTogetherRepository>()
    private val _nowPlayingVideoId: MutableStateFlow<String> = MutableStateFlow("")

    /**
     * Get now playing video id
     * If empty, no video is playing
     */
    val nowPlayingVideoId: StateFlow<String> get() = _nowPlayingVideoId

    /**
     * Tag for logging
     */
    protected val tag: String = this::class.simpleName ?: "BaseViewModel"

    /**
     * Log with viewModel tag
     */
    protected fun log(
        message: String,
        logType: LogLevel = LogLevel.WARN,
    ) {
        when (logType) {
            LogLevel.DEBUG -> Logger.d(tag, message)
            LogLevel.INFO -> Logger.i(tag, message)
            LogLevel.WARN -> Logger.w(tag, message)
            LogLevel.ERROR -> Logger.e(tag, message)
        }
    }

    /**
     * Cancel all jobs
     */
    override fun onCleared() {
        super.onCleared()
        viewModelScope.cancel()
        log("ViewModel cleared", LogLevel.WARN)
    }

    init {
        getNowPlayingVideoId()
    }

    fun makeToast(message: String?) {
        showToast(
            message = message ?: "NO MESSAGE",
            duration = ToastDuration.Short,
            gravity = ToastGravity.Bottom,
        )
    }

    protected fun getString(resId: StringResource): String =
        runBlocking {
            org.jetbrains.compose.resources
                .getString(resId)
        }

    // Loading dialog
    private val _showLoadingDialog: MutableStateFlow<Pair<Boolean, String>> = MutableStateFlow(false to getString(Res.string.loading))
    val showLoadingDialog: StateFlow<Pair<Boolean, String>> get() = _showLoadingDialog

    fun showLoadingDialog(message: String? = null) {
        _showLoadingDialog.value = true to (message ?: getString(Res.string.loading))
    }

    fun hideLoadingDialog() {
        _showLoadingDialog.value = false to getString(Res.string.loading)
    }

    private fun getNowPlayingVideoId() {
        viewModelScope.launch {
            combine(mediaPlayerHandler.nowPlayingState, mediaPlayerHandler.controlState) { nowPlayingState, controlState ->
                Pair(nowPlayingState, controlState)
            }.collect { (nowPlayingState, controlState) ->
                if (controlState.isPlaying) {
                    _nowPlayingVideoId.value = nowPlayingState.songEntity?.videoId ?: ""
                } else {
                    _nowPlayingVideoId.value = ""
                }
            }
        }
    }

    /**
     * Communicate with SimpleMediaServiceHandler to load media item
     */
    fun setQueueData(queueData: QueueData.Data) {
        val roomState = listenTogetherRepository.room.value
        if (roomState.inRoom) {
            val canControl = roomState.isHost || roomState.permissions.allowQueue
            if (!canControl) {
                makeToast("You don't have permission to change music in this Jam room.")
                return
            }
            if (!roomState.isHost) {
                val roomTrack = queueData.firstPlayedTrack?.toRoomTrack()
                if (roomTrack != null) {
                    listenTogetherRepository.playTrackDirect(roomTrack)
                    makeToast("Playing track in Jam room.")
                }
                return
            }
        }

        mediaPlayerHandler.reset()
        mediaPlayerHandler.setQueueData(queueData)
    }

    fun <T> loadMediaItem(
        anyTrack: T,
        type: String,
        index: Int? = null,
    ) {
        viewModelScope.launch {
            val roomState = listenTogetherRepository.room.value
            if (roomState.inRoom) {
                val canControl = roomState.isHost || roomState.permissions.allowPlayDirect
                if (!canControl) {
                    makeToast("You don't have permission to play music in this Jam room.")
                    return@launch
                }
                if (!roomState.isHost) {
                    val roomTrack = anyTrack.toRoomTrack()
                    if (roomTrack != null) {
                        listenTogetherRepository.playTrackDirect(roomTrack)
                        return@launch
                    }
                }
            }

            mediaPlayerHandler.loadMediaItem(
                anyTrack = anyTrack,
                type = type,
                index = index,
            )
        }
    }

    private fun parseDurationToMs(durationStr: String): Long {
        val parts = durationStr.split(":")
        var seconds = 0L
        for (part in parts) {
            seconds = seconds * 60 + part.toLong()
        }
        return seconds * 1000L
    }

    protected fun Any?.toRoomTrack(): RoomTrack? {
        return when (this) {
            is SongEntity -> RoomTrack(
                id = videoId,
                title = title,
                artist = artistName?.joinToString(", ").orEmpty(),
                album = albumName.orEmpty(),
                durationMs = durationSeconds.toLong() * 1000L,
                thumbnail = thumbnails.orEmpty(),
            )
            is SongsResult -> RoomTrack(
                id = videoId,
                title = title ?: "",
                artist = artists?.joinToString(", ") { it.name }.orEmpty(),
                album = album?.name.orEmpty(),
                durationMs = (durationSeconds?.toLong()?.times(1000L)) ?: (duration?.let { parseDurationToMs(it) } ?: 0L),
                thumbnail = thumbnails?.lastOrNull()?.url ?: "",
            )
            is Track -> RoomTrack(
                id = videoId,
                title = title,
                artist = artists?.joinToString(", ") { it.name }.orEmpty(),
                album = album?.name.orEmpty(),
                durationMs = durationSeconds?.toLong()?.times(1000L) ?: 0L,
                thumbnail = thumbnails?.lastOrNull()?.url ?: "",
            )
            is Content -> RoomTrack(
                id = videoId ?: "",
                title = title,
                artist = artists?.joinToString(", ") { it.name }.orEmpty(),
                album = album?.name.orEmpty(),
                durationMs = 0L,
                thumbnail = thumbnails.lastOrNull()?.url ?: "",
            )
            else -> null
        }
    }

    fun shufflePlaylist(firstPlayIndex: Int = 0) {
        mediaPlayerHandler.shufflePlaylist(firstPlayIndex)
    }
}