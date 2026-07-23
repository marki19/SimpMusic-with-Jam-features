package com.marki19.simpmusic.ui.navigation.destination.jam

import kotlinx.serialization.Serializable

@Serializable
data object JamMenuDestination

@Serializable
data class JamHostDestination(
    val initialVideoId: String? = null,
    val initialTitle: String? = null,
    val initialArtist: String? = null,
    val initialThumbnailUrl: String? = null,
    val initialDurationMs: Long? = null
)

@Serializable
data object JamGuestDestination

@Serializable
data class JamSessionDestination(val roomCode: String)
