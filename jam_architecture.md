# SimpMusic Jam (Listen Together) Architecture

This document provides a comprehensive overview of the "Jam" (Listen Together) architecture in SimpMusic. It is designed to help LLMs (like ChatGPT) or new developers quickly understand how real-time synchronized playback is achieved across multiple clients.

---

## High-Level Overview
The Jam feature allows users to listen to music synchronously in a shared room. It follows a **Client-Server WebSocket** architecture with a strict **Host/Guest (Authoritative)** synchronization model. 

1. **Host**: The room creator. The Host's local audio player is the ultimate source of truth for playback (play, pause, current position, track changes).
2. **Guests (Members)**: Listeners in the room. Guests passively observe the room's state and forcefully apply the Host's playback transport to their local audio players.
3. **Server**: A lightweight Go server that manages WebSockets, broadcasts state to members, and calculates clock drift/latency.

---

## 1. The Server (Go WebSocket Backend)
The backend is a Go server (often referred to as MetroServer in the project) hosted on Render. 
- **Connection**: Clients connect via WebSockets.
- **Role**: It acts as a passive relay and state holder. It keeps track of `Room` objects, user tokens, queues, and permissions.
- **Message Types**: Communication happens via JSON payloads categorized by `MessageTypes`. Common types include:
  - `CREATE_ROOM`, `JOIN_ROOM`, `LEAVE_ROOM`
  - `SYNC_STATE` (The primary state broadcast containing what is currently playing and where).
  - `SYNC_QUEUE` (Broadcasts the list of upcoming songs).
  - `PLAYBACK_ACTION` (Commands like PLAY, PAUSE, SEEK, CHANGE_TRACK).
  - `PING` / `PONG` (Used for latency calculation and clock synchronization).

---

## 2. Client Core: `ListenTogetherSession.kt`
Located in the `core/service/listenTogether` module, this is the network engine for the Jam feature.
- **WebSocket Management**: Uses Ktor to maintain the WebSocket connection.
- **State Holder**: Maintains a `MutableStateFlow<RoomState>` which holds the current `roomCode`, `queue`, `chatMessages`, `members`, and the `currentTrack`.
- **Clock Sync (`ServerClock`)**: 
  - To keep audio perfectly in sync across different devices, the client continuously pings the server. 
  - It calculates the round-trip latency and creates a time offset. When the Host says "I am at 1:30 in the song," the Guest uses the offset to know exactly what time `1:30` means on their local device clock, preventing stutter and drift.

---

## 3. The Brain: `ListenTogetherPlaybackBridge.kt`
Located in `core/data/src/commonMain/...`, this class is the mediator. It sits between the network (`ListenTogetherSession`) and the local audio engine (`MediaPlayerHandler`).

It determines whether the user is a Host or a Guest and acts accordingly:

### The Host's Responsibilities
If the user is the Host, the Bridge actively listens to the local `ExoPlayer` and publishes its state to the server:
- `publishPlayPauseAsHost()`: Listens for play/pause intents. If the host pauses, it sends a `PAUSE` command to the room.
- `publishSeeksAsHost()`: If the host scrubs the timeline, it sends a `SEEK` command.
- `publishTrackChangesAsHost()`: When the host changes the song, it broadcasts `CHANGE_TRACK`.
- `handleAutoAdvanceAsHost()`: Because Jam mode only loads *one track at a time* into ExoPlayer, this function listens for `SimpleMediaState.Ended`. When the song finishes, it automatically grabs the next song from the Jam Queue and plays it.

### The Guest's Responsibilities
If the user is a Guest, the Bridge ignores the local player's organic actions and instead forces it to match the room:
- `watchRoomPlayback()`: Continuously collects the `RoomSnapshot` from the server.
- **Buffer Barrier**: When a new track starts, Guests do not play immediately. They send a `buffer_ready` signal once they have buffered enough of the song (`READY_BUFFER_PERCENT`). The server holds playback until everyone is ready, ensuring everyone starts at the exact same millisecond.
- `applyTransport()`: Applies the Host's playback state to the Guest's player:
  - If the Host is playing, it calls `player.play()`.
  - If the Host is paused, it calls `player.pause()`.
  - It calculates the "drift" between the Host's position and the Guest's position. If the drift exceeds a threshold (`SEEK_TOLERANCE_MS`, usually ~100-200ms), it calls `player.seekTo()` to snap the guest back into perfect sync.

---

## 4. Local Audio Engine (`MediaServiceHandlerImpl.kt`)
The actual audio playback is handled by Android's `Media3 ExoPlayer` (or `JvmMediaPlayerHandlerImpl` on Desktop).
- **Decoupled Architecture**: ExoPlayer has absolutely no idea it is in a "Jam". It just plays the tracks it is handed by the `PlaybackBridge`.
- **Single Track Loading**: In normal SimpMusic use, ExoPlayer is given a whole playlist. In Jam mode, the `PlaybackBridge` clears ExoPlayer's playlist and gives it exactly **one** track at a time. The Jam Queue is maintained purely in `ListenTogetherSession`.

---

## 5. UI and Presentation Layer
- **`ListenTogetherViewModel.kt`**: Bridges the UI to the `ListenTogetherRepository`. Handles user interactions like kicking members, sending chat messages, swiping to delete songs from the queue, or dragging to reorder the queue.
- **Permissions**: The Host can grant permissions to Guests (e.g., `allowQueue`, `allowReorder`, `allowPlayPause`). The UI reflects these capabilities, and the `ListenTogetherViewModel` enforces them before sending commands to the server.

---

## Summary of Data Flow (Example: Host Pauses a Song)
1. User (Host) taps Pause in the UI.
2. `MediaServiceHandlerImpl` pauses ExoPlayer.
3. ExoPlayer fires an event indicating `isPlaying = false`.
4. `ListenTogetherPlaybackBridge.publishPlayPauseAsHost()` detects this change.
5. The Bridge calls `ListenTogetherSession.send(PLAYBACK_ACTION, "PAUSE")`.
6. Go Server receives PAUSE and broadcasts it to all Guests in the room.
7. Guest's `ListenTogetherSession` parses the WebSocket frame and updates the `RoomState`.
8. Guest's `ListenTogetherPlaybackBridge.watchRoomPlayback()` detects the new state.
9. `applyTransport()` is called on the Guest, executing `handler.player.pause()`.
10. The Guest's ExoPlayer pauses perfectly in sync.
