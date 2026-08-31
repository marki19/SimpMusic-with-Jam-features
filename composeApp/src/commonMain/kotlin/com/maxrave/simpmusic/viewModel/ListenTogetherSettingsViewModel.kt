package com.maxrave.simpmusic.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxrave.data.listentogether.ListenTogetherPrefs
import com.maxrave.domain.data.model.listentogether.ListenTogetherRoom
import com.maxrave.domain.manager.DataStoreManager
import com.maxrave.domain.repository.ListenTogetherRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Jam settings preferences and live room controls.
 */
class ListenTogetherSettingsViewModel(
    private val dataStore: DataStoreManager,
    private val repository: ListenTogetherRepository,
) : ViewModel() {
    val room: StateFlow<ListenTogetherRoom> = repository.room

    val autoApproveJoins: StateFlow<Boolean> = boolFlow(KEY_AUTO_APPROVE_JOINS)
    val autoApproveSuggestions: StateFlow<Boolean> = boolFlow(KEY_AUTO_APPROVE_SUGGESTIONS)
    val followHostVolume: StateFlow<Boolean> = boolFlow(KEY_FOLLOW_HOST_VOLUME, default = true)

    // Jam permissions — these are host-scoped and govern what members may do in a hosted Jam.
    val jamAllowQueue: StateFlow<Boolean> = boolFlow(KEY_JAM_ALLOW_QUEUE, default = true)
    val jamAllowReorder: StateFlow<Boolean> = boolFlow(KEY_JAM_ALLOW_REORDER, default = false)
    val jamAllowPlayDirect: StateFlow<Boolean> = boolFlow(KEY_JAM_ALLOW_PLAY_DIRECT, default = false)
    val jamAllowSeek: StateFlow<Boolean> = boolFlow(KEY_JAM_ALLOW_SEEK, default = false)
    val jamAllowPlayPause: StateFlow<Boolean> = boolFlow(KEY_JAM_ALLOW_PLAY_PAUSE, default = false)
    val jamAutoplay: StateFlow<Boolean> = boolFlow(KEY_JAM_AUTOPLAY, default = true)

    val blockedNames: StateFlow<List<String>> =
        dataStore
            .getString(KEY_BLOCKLIST)
            .map { raw -> raw.orEmpty().split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setAutoApproveJoins(value: Boolean) = putBool(KEY_AUTO_APPROVE_JOINS, value)

    fun setAutoApproveSuggestions(value: Boolean) = putBool(KEY_AUTO_APPROVE_SUGGESTIONS, value)

    fun setFollowHostVolume(value: Boolean) = putBool(KEY_FOLLOW_HOST_VOLUME, value)

    fun setJamAllowQueue(value: Boolean) {
        putBool(KEY_JAM_ALLOW_QUEUE, value)
        syncLivePermissions(allowQueue = value)
    }

    fun setJamAllowReorder(value: Boolean) {
        putBool(KEY_JAM_ALLOW_REORDER, value)
        syncLivePermissions(allowReorder = value)
    }

    fun setJamAllowPlayDirect(value: Boolean) {
        putBool(KEY_JAM_ALLOW_PLAY_DIRECT, value)
        syncLivePermissions(allowPlayDirect = value)
    }

    fun setJamAllowSeek(value: Boolean) {
        putBool(KEY_JAM_ALLOW_SEEK, value)
        syncLivePermissions(allowSeek = value)
    }

    fun setJamAllowPlayPause(value: Boolean) {
        putBool(KEY_JAM_ALLOW_PLAY_PAUSE, value)
        syncLivePermissions(allowPlayPause = value)
    }

    fun setJamAutoplay(value: Boolean) = putBool(KEY_JAM_AUTOPLAY, value)

    fun leaveRoom() {
        repository.leaveRoom()
    }

    fun endRoom() {
        repository.endRoom()
    }

    fun unblock(name: String) =
        viewModelScope.launch {
            val remaining = blockedNames.value.filterNot { it.equals(name, ignoreCase = true) }
            dataStore.putString(KEY_BLOCKLIST, remaining.joinToString(SEPARATOR))
        }

    private fun syncLivePermissions(
        allowQueue: Boolean = jamAllowQueue.value,
        allowReorder: Boolean = jamAllowReorder.value,
        allowPlayDirect: Boolean = jamAllowPlayDirect.value,
        allowSeek: Boolean = jamAllowSeek.value,
        allowPlayPause: Boolean = jamAllowPlayPause.value,
    ) {
        val currentRoom = repository.room.value
        if (currentRoom.inRoom && currentRoom.isHost) {
            repository.updateJamPermissions(
                allowQueue = allowQueue,
                allowReorder = allowReorder,
                allowPlayDirect = allowPlayDirect,
                allowSeek = allowSeek,
                allowPlayPause = allowPlayPause,
            )
        }
    }

    private fun boolFlow(
        key: String,
        default: Boolean = false,
    ): StateFlow<Boolean> =
        dataStore
            .getString(key)
            .map { it?.equals(TRUE, ignoreCase = true) ?: default }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), default)

    private fun putBool(
        key: String,
        value: Boolean,
    ) = viewModelScope.launch { dataStore.putString(key, if (value) TRUE else FALSE) }

    companion object {
        const val KEY_SERVER_URL = ListenTogetherPrefs.SERVER_URL
        const val KEY_AUTO_APPROVE_JOINS = ListenTogetherPrefs.AUTO_APPROVE_JOINS
        const val KEY_AUTO_APPROVE_SUGGESTIONS = ListenTogetherPrefs.AUTO_APPROVE_SUGGESTIONS
        const val KEY_FOLLOW_HOST_VOLUME = ListenTogetherPrefs.FOLLOW_HOST_VOLUME
        const val KEY_BLOCKLIST = ListenTogetherPrefs.BLOCKLIST
        const val KEY_JAM_ALLOW_QUEUE = ListenTogetherPrefs.JAM_ALLOW_QUEUE
        const val KEY_JAM_ALLOW_REORDER = ListenTogetherPrefs.JAM_ALLOW_REORDER
        const val KEY_JAM_ALLOW_PLAY_DIRECT = ListenTogetherPrefs.JAM_ALLOW_PLAY_DIRECT
        const val KEY_JAM_ALLOW_SEEK = ListenTogetherPrefs.JAM_ALLOW_SEEK
        const val KEY_JAM_ALLOW_PLAY_PAUSE = ListenTogetherPrefs.JAM_ALLOW_PLAY_PAUSE
        const val KEY_JAM_AUTOPLAY = ListenTogetherPrefs.JAM_AUTOPLAY

        private const val TRUE = ListenTogetherPrefs.TRUE
        private const val FALSE = ListenTogetherPrefs.FALSE
        private const val SEPARATOR = ListenTogetherPrefs.BLOCKLIST_SEPARATOR
    }
}
