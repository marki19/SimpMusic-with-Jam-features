# Jam / Listen Together Bug Analysis - FIXED

## Executive Summary

All bugs have been fixed:

1. **BUG #1 (Skip/NEXT doesn't auto-play)**: FIXED - Client was setting `isPlaying=false` on `CHANGE_TRACK`
2. **BUG #2 (Natural end doesn't auto-play)**: FIXED - Same root cause as BUG #1
3. **BUG #3 (Member join interrupts playback)**: FIXED - Same root cause as BUG #1
4. **BUG #4 (Ghost members / Host replacement timing)**: FIXED - Host transfer now happens after grace period

---

## Bug #1 & #2: Skip/Natural End Don't Auto-Play

### Root Cause

**File**: `core/service/listenTogether/src/commonMain/kotlin/org/simpmusic/listentogether/ListenTogetherSession.kt`, line 775

The client session was **explicitly setting `isPlaying = false` on every `CHANGE_TRACK` action**:

```kotlin
// BEFORE (BUG):
isPlaying = when (p.action) {
    PlaybackActions.PAUSE -> false
    PlaybackActions.PLAY -> true
    PlaybackActions.CHANGE_TRACK -> false  // ← BUG!
    else -> it.isPlaying
}
```

This contradicted the server (`playback.go`) which **preserves** `IsPlaying` across track changes.

### Fix Applied

Removed the `CHANGE_TRACK -> false` line. The server preserves playback intent, and the host sends a follow-up PLAY/PAUSE action on the same coroutine to restore the correct state.

```kotlin
// AFTER (FIXED):
// NOTE: CHANGE_TRACK does NOT set isPlaying here. The server preserves
// the previous playback intent across track changes, and the host sends a
// follow-up PLAY/PAUSE action on the same coroutine to restore the correct state.
isPlaying = when (p.action) {
    PlaybackActions.PAUSE -> false
    PlaybackActions.PLAY -> true
    else -> it.isPlaying
}
```

### Verification

The existing code in `playQueuedTrack()` already captures and restores playback intent:
```kotlin
val wasPlaying = _state.value.isPlaying  // Capture intent
// Send CHANGE_TRACK...
// Send PLAY/PAUSE based on wasPlaying...
```

And `publishTrackChangesAsHost()` sends PLAY immediately:
```kotlin
session.sendPlaybackAction(action = CHANGE_TRACK, ...)
session.sendPlaybackAction(action = PLAY, position = 0L, ...)  // Immediate
```

---

## Bug #3: Member Join Interrupts Playback

### Root Cause

Same root cause as BUG #1. When a new member joins and receives a `SYNC_PLAYBACK` with `CHANGE_TRACK` action, their client was incorrectly setting `isPlaying = false`.

The fix for BUG #1 also fixes this bug.

---

## Bug #4: Ghost Members / Host Replacement Timing

### Root Cause

**File**: `metroserver/internal/server/lifecycle.go`

The server was transferring host **immediately** on disconnect (line 286), not waiting for the 15-minute grace period.

This meant:
1. Host disconnects
2. Host is transferred to another member immediately
3. Original host reconnects within 15 minutes
4. Original host reclaims host status (because `room.Host == nil`)
5. Second member loses host status

This created confusion about who was actually host.

### Fix Applied

**1. Removed immediate host transfer from `handleClientDisconnect`:**
```go
// BEFORE: Transfer host immediately
if wasHost {
    room.Host = newHost  // ← Removed
    ...
}

// AFTER: Just track that they were host, transfer happens later
// Track if host disconnected - host transfer is delayed until session expires
// Host transfer happens in cleanupExpiredSessionsOnce() when the session is removed.
```

**2. Host transfer now happens in `cleanupExpiredSessionsOnce`:**
```go
// Host is transferred only when the session actually expires
if expiredWasHost {
    // Find new host and transfer...
}
```

**3. Grace period changed from 15 minutes to 5 minutes** (per spec):
```go
ReconnectGracePeriod = 5 * time.Minute
```

**4. Original host can reclaim host status on reconnect:**
```go
if session.IsHost || room.State.HostID == session.UserID {
    // Reclaim host status...
    // Notify other members of host change...
}
```

### Verification

- Session cleanup runs every 1 minute (`SessionCleanupInterval`)
- After 5 minutes of disconnection, session is removed and host is transferred
- If original host reconnects within grace period, they reclaim host status
- Members are notified of host changes via `MsgTypeHostChanged`

---

## Files Modified

### Server (Go)
1. `metroserver/internal/server/lifecycle.go`
   - Removed immediate host transfer from `handleClientDisconnect`
   - Added host reclamation logic to `handleReconnect`
   - Added `host_changed` notification on host reclamation

2. `metroserver/internal/server/server.go`
   - Changed `ReconnectGracePeriod` from 15 to 5 minutes

### Client (Kotlin)
1. `core/service/listenTogether/src/commonMain/kotlin/org/simpmusic/listentogether/ListenTogetherSession.kt`
   - Removed `CHANGE_TRACK -> false` from `isPlaying` update

---

## Why Existing Synchronization Is Preserved

The existing synchronization mechanisms remain untouched:

1. **ServerClock** - RTT measurement and timestamp synchronization
2. **Buffer barrier** - All members wait at barrier until ready
3. **Position sync** - `livePlaybackPosition()` calculates correct position
4. **Seek handling** - Remote seeks via `ActionSeek`
5. **Drift correction** - Periodic and pong-based correction

Only the `isPlaying` update logic on `CHANGE_TRACK` was modified, and it was modified to NOT contradict the server's preserved state.

---

## Test Scenarios Covered

| Scenario | Expected Behavior |
|----------|-------------------|
| Host skips while playing | Next track starts playing |
| Host skips while paused | Next track stays paused |
| Natural track end while playing | Next track starts playing |
| Natural track end while paused | Next track stays paused |
| Member joins while playing | New member syncs and plays |
| Member disconnects | Grace period starts (5 min) |
| Member reconnects within grace | Same identity, continues |
| Host disconnects | Grace period starts (5 min) |
| Host reconnects within grace | Host status restored |
| Host doesn't reconnect within grace | Host transferred to next member |
| Paused member stays connected | No action needed |
