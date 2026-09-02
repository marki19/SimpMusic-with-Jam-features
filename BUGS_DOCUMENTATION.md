# SimpMusic Bugs & Debug Documentation

> Comprehensive documentation of all known bugs, root causes, and debug attempts.
> Last updated: 2026-09-02

---

## Table of Contents

1. [Listen Together / Jam Playback Sync Bugs](#1-listen-together--jam-playback-sync-bugs)
2. [Crossfade Guard Bugs](#2-crossfade-guard-bugs)
3. [Desktop Media Playback Bugs](#3-desktop-media-playback-bugs)
4. [Database & Migration Bugs](#4-database--migration-bugs)
5. [UI/UX Bugs](#5-uix-bugs)
6. [Audio Processing Bugs](#6-audio-processing-bugs)
7. [Third-Party Integration Bugs](#7-third-party-integration-bugs)
8. [Platform-Specific Bugs](#8-platform-specific-bugs)

---

## 1. Listen Together / Jam Playback Sync Bugs

### Bug 1.1: Auto-advance Starts at 3-5s Instead of 0:00

**Severity**: Critical  
**Status**: FIXED (2026-09-02)  
**Date Discovered**: 2026-09-02  
**Date Fixed**: 2026-09-02

#### Symptoms
- When a track ends and auto-advance plays the next track, playback starts at 3-5 seconds instead of 0:00
- Guests in the room experience the track starting at the wrong position

#### Root Cause
**Client-side bug** in `ListenTogetherSession.kt:775` - the session was explicitly setting `isPlaying = false` on every `CHANGE_TRACK` action, contradicting the server which preserves playback intent across track changes.

The server's `ActionChangeTrack` handler does NOT modify `IsPlaying` - it only sets `Position = 0`. The client was incorrectly overriding this.

#### Files Modified
- `core/service/listenTogether/src/commonMain/kotlin/org/simpmusic/listentogether/ListenTogetherSession.kt`
  - Removed `CHANGE_TRACK -> false` from `isPlaying` update logic

#### Fix
```kotlin
// AFTER FIX:
// NOTE: CHANGE_TRACK does NOT set isPlaying here. The server preserves
// the previous playback intent across track changes.
isPlaying = when (p.action) {
    PlaybackActions.PAUSE -> false
    PlaybackActions.PLAY -> true
    else -> it.isPlaying
}
```

---

### Bug 1.2: NEXT Button Pauses Playback

**Severity**: Critical  
**Status**: FIXED (2026-09-02)  
**Date Discovered**: 2026-09-02  
**Date Fixed**: 2026-09-02

#### Symptoms
- Clicking the NEXT button in a Listen Together room pauses playback instead of continuing
- The track advances but audio stops

#### Root Cause
Same root cause as Bug 1.1 - the client session was setting `isPlaying = false` on `CHANGE_TRACK`.

#### Fix
Same as Bug 1.1 - removed the `CHANGE_TRACK -> false` line.

---

### Bug 1.3: Member Join Interrupts Playback

**Severity**: Critical  
**Status**: FIXED (2026-09-02)  
**Date Discovered**: 2026-09-02  
**Date Fixed**: 2026-09-02

#### Symptoms
- When a member joins a room that is already playing, existing playback gets interrupted/paused

#### Root Cause
Same root cause as Bug 1.1 - the new member's client was incorrectly setting `isPlaying = false` on `CHANGE_TRACK` from the room state.

#### Fix
Same as Bug 1.1.

---

### Bug 1.4: Ghost Members / Host Replacement Timing

**Severity**: High  
**Status**: FIXED (2026-09-02)  
**Date Discovered**: 2026-09-02  
**Date Fixed**: 2026-09-02

#### Symptoms
- Host disconnect causes immediate host transfer to another member
- If original host reconnects within grace period, they reclaim host status
- This creates confusion about who is actually host

#### Root Cause
**Server-side bug** in `lifecycle.go:286` - host transfer was happening immediately on disconnect, not waiting for the grace period.

#### Files Modified
- `metroserver/internal/server/lifecycle.go`
  - Removed immediate host transfer from `handleClientDisconnect`
  - Added host reclamation logic to `handleReconnect`
  - Added `host_changed` notification on host reclamation
- `metroserver/internal/server/server.go`
  - Changed `ReconnectGracePeriod` from 15 to 5 minutes (per spec)

#### Fix
Host transfer now happens in `cleanupExpiredSessionsOnce` only when the session actually expires (after 5 minutes). If the original host reconnects within the grace period, they automatically reclaim host status.

---

## 2. Crossfade Guard Bugs

### Bug 2.1: Crossfade Skipped When Shouldn't (Dead Code)

**Severity**: Medium  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Crossfade was being skipped under certain conditions unexpectedly

#### Root Cause
The crossfade guards existed on only ONE of two trigger paths:
1. Position-polling job (`timeRemaining in 1..crossfadeDuration + prep`)
2. `handleTrackEndInternal()` on EOF

The EOF path returned early when `isCrossfading` was set, so conditions added only there were dead code.

#### Fix
Added guards to BOTH trigger paths - following the pattern of the video checks which were correctly on both paths.

#### Files Modified
- `CrossfadeExoPlayerAdapter.kt` (Android)
- `MpvPlayerAdapter.kt` (Desktop)

---

### Bug 2.2: Crossfade Skipping Short Tracks

**Severity**: Low  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Crossfade was applied to very short tracks (<20 seconds)

#### Root Cause
No minimum duration check was in place.

#### Fix
Added check: skip crossfade when current track is shorter than `max(20 s, crossfadeDuration × 3)`

---

### Bug 2.3: Same Album Crossfade Option

**Severity**: Low  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Crossfade was applied between tracks of the same album, breaking continuous playback

#### Root Cause
No album-aware skip logic existed.

#### Fix
Added opt-in setting to skip crossfade between tracks of the same album. Album recognition via `PlaylistType.ALBUM` snapshot.

---

### Bug 2.4: Crossfade Video Handling

**Severity**: Medium  
**Status**: Fixed (2026-08-01, refined 2026-08-05)  
**Date Fixed**: 2026-08-01

#### Symptoms
- Video tracks were causing issues during crossfade transitions

#### Root Cause
Crossfade setup was attempting to merge audio+video sources mid-fade, which is error-prone.

#### Fix
Both `CrossfadeExoPlayerAdapter` (Android) and `MpvPlayerAdapter` (Desktop) now skip the crossfade path when the NEXT track will play as video (`isVideo()` + watch-video setting on).

Added `isCurrentTrackVideo()` check to ensure current video also plays out to its last frame instead of fading out under incoming song.

---

## 3. Desktop Media Playback Bugs

### Bug 3.1: VLC → mpv Migration Caused Audio Loss

**Severity**: Critical  
**Status**: Fixed (2026-07-27)  
**Date Fixed**: 2026-07-27

#### Symptoms
- Desktop app had no audio after migrating from VLCJ to libmpv

#### Root Cause
Multiple issues in the migration:
1. JNA open flags were POSIX-only, not working on Windows
2. Library path resolution was incorrect
3. Audio output not properly configured

#### Fix
- Rewrote `MpvLibrary.kt` with correct JNA bindings
- Fixed `MpvPlayer.kt` audio output configuration
- Corrected library path resolution via `MpvLibrary.bundledLibraryDirs()`

---

### Bug 3.2: Desktop Video Rendering Issues

**Severity**: Medium  
**Status**: Fixed (2026-08-01)  
**Date Fixed**: 2026-08-01

#### Symptoms
- Video playback had z-order issues with always-on-top windows
- One-frame-late repositioning while scrolling
- Video randomly missing until next/prev

#### Root Cause
`SwingPanel` embedding had issues with:
- AWT's single-parent rule
- Repositioning lag

#### Fix
Replaced `MpvVideoSurfacePanel` (JPanel + SwingPanel) with `MpvVideoFrameSource` - mpv SW render loop publishes immutable `BufferedImage` snapshots via StateFlow, drawn by plain Compose `Image`.

---

### Bug 3.3: macOS CoreAudio Crash

**Severity**: Critical  
**Status**: Fixed (2026-08-01)  
**Date Fixed**: 2026-08-01

#### Symptoms
- Process crash on macOS when audio device appears/disappears
- `EXC_BAD_ACCESS` on `HALC_ProxyNotification Call Listener Queue`

#### Root Cause
Upstream mpv bug: `ao_coreaudio.c` registers a hotplug listener on the system object but fails to unregister it if `init_audiounit` fails later. Orphaned listener outlives the handle.

**Why SimpMusic hits this and plain mpv does not**: mpv initializes one ao per session; SimpMusic creates one handle per media item and runs two at once during crossfade.

#### Fix
Pinned `ao` to `"avfoundation,"` on macOS. Trailing comma keeps mpv's auto-probe as fallback.

---

### Bug 3.4: Bundled glib Broke Java AWT Desktop on Linux

**Severity**: Critical  
**Status**: Workaround Applied (2026-07-31)  
**Date Fixed**: 2026-07-31

#### Symptoms
- `Desktop.getDesktop()` threw `UnsupportedOperationException` on Linux
- `openUrl()` silently did nothing

#### Root Cause
Bundled `libglib-2.0.so.0` (glib 2.72) claimed the glib soname, breaking system gio/gobject linking on hosts with glib 2.80 (Ubuntu 24.04).

#### Fix
Called `Desktop.isDesktopSupported()` at the top of `runDesktopApp` before libmpv loads. Added per-OS launcher fallback for `openUrl()`.

**Long-term Fix Required**: Stop bundling glib - add to `SYSTEM_LIBS` in Linux tarball.

---

### Bug 3.5: Pitch Control Missing on Desktop

**Severity**: Low  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Pitch control row was hidden on Desktop

#### Root Cause
Stale comment from VLC era: "LibVLC doesn't support independent pitch control" - no longer true with mpv.

#### Fix
Re-enabled pitch control. mpv shifts pitch with `rubberband` filter. Hidden during crossfade since crossfade owns the `af` chain.

---

## 4. Database & Migration Bugs

### Bug 4.1: Clear Listening History Swept Wrong Data

**Severity**: High  
**Status**: Fixed (2026-08-16)  
**Date Fixed**: 2026-08-16

#### Symptoms
- Clear history deleted 0 songs despite having thousands of plays
- Orphaned data remained in database

#### Root Cause
Multiple issues:
1. `song.liked` and `song.downloadState` were NOT foreign keys, so literal "referenced by nothing" reading deleted nothing
2. `NOT IN` over nullable column silently matched nothing: `x NOT IN (…, NULL, …)` is NULL, never TRUE
3. `_` is a LIKE wildcard - videoIds contain underscores and weren't escaped

#### Fix
- Delete by `downloadState = 0` instead of "nothing references it"
- Added `WHERE <col> IS NOT NULL` for subqueries
- Escaped underscores with `ESCAPE '\'`

---

### Bug 4.2: VACUUM Fails in Room RawQuery

**Severity**: Medium  
**Status**: Fixed (2026-08-16)  
**Date Fixed**: 2026-08-16

#### Symptoms
- Clear history couldn't VACUUM the database after deleting

#### Root Cause
`@RawQuery` routes to a read-only connection (`isReadOnly = true`). Room opens readers with `PRAGMA query_only = 1`.

#### Fix
Moved `vacuum()` to `MusicDatabase` as `useWriterConnection { it.execSQL("VACUUM") }`.

---

### Bug 4.3: videoType Column Never Read from API

**Severity**: Medium  
**Status**: Fixed (2026-08-16)  
**Date Fixed**: 2026-08-16

#### Symptoms
- `song.videoType` held random values depending on which screen wrote the row
- "Is this a video?" couldn't be answered reliably

#### Root Cause
No parser ever read YouTube's `musicVideoType` field. Every call site invented its own label. `ResultVideo.toTrack()` smuggled view count into `videoType` column.

#### Fix
Read `musicVideoType` from `watchEndpointMusicSupportedConfigs`. Added `MusicVideoType` normalization in `core/domain`.

---

### Bug 4.4: Wrapped Queue Rows Dropped (82% Loss)

**Severity**: High  
**Status**: Fixed (2026-08-16)  
**Date Fixed**: 2026-08-16

#### Symptoms
- 82% of logged-in radio tracks were missing
- 161 of 197 rows across four pages were dropped

#### Root Cause
`YouTube.next()` read `it.playlistPanelVideoRenderer` only. Every row shipped as `playlistPanelVideoWrapperRenderer` resolved to null in the `mapNotNull`.

#### Fix
Read `Content.track` (bare renderer, else `primaryRenderer`) instead.

---

## 5. UI/UX Bugs

### Bug 5.1: Analytics Screen Force Dark Content

**Severity**: Medium  
**Status**: Fixed (2026-08-22)  
**Date Fixed**: 2026-08-22

#### Symptoms
- On light theme, Analytics screen showed dark-on-dark labels

#### Root Cause
`ForceDarkContent` was applied per-destination in the nav graph. Analytics was the only immersive screen never wrapped.

#### Fix
Added `ForceDarkContent` wrapper to Analytics screen.

---

### Bug 5.2: Portrait Header Hardcoded to Desktop

**Severity**: Medium  
**Status**: Fixed (2026-08-17)  
**Date Fixed**: 2026-08-17

#### Symptoms
- Android portrait showed desktop layout (280dp artwork, Apple Music style)
- Android landscape/tablet lost the old layout entirely

#### Root Cause
Migration shipped with `val isMobilePortrait = true` hardcoded.

#### Fix
Changed gate to `val isPortrait = screenInfo.wDP < screenInfo.hDP`.

---

### Bug 5.3: Liquid Glass Rim Missing on Small Buttons

**Severity**: Low  
**Status**: Fixed (2026-08-17, refined 2026-08-26)  
**Date Fixed**: 2026-08-17

#### Symptoms
- Small round buttons looked rimless

#### Root Cause
`Highlight.Default` carries `HighlightStyle.Default` with directional highlight that small circles don't catch well.

#### Fix
Small round buttons now use `Highlight(width = 1.dp)`, NOT `Highlight.Plain`.

---

### Bug 5.4: HazeProgressive Crashes on skiko

**Severity**: High  
**Status**: Fixed (2026-08-17)  
**Date Fixed**: 2026-08-17

#### Symptoms
- Process crash when ArtistScreen's bottom fade was rendered on Desktop

#### Root Cause
haze 1.7.2's progressive path calls `ShaderBrush.createShader(Size)` with a mangled signature incompatible with pinned skiko version.

#### Fix
Guarded `HazeProgressive` with `getPlatform() == Platform.Android`. Desktop loses only the blur.

---

## 6. Audio Processing Bugs

### Bug 6.1: Playback State Published Inverted

**Severity**: High  
**Status**: Fixed (2026-08-16)  
**Date Fixed**: 2026-08-16

#### Symptoms
- Loading spinner shown over playing audio
- Every track start and resume ended with spinner

#### Root Cause
Multiple issues:
1. `onIsLoadingChanged` wrote 2-3 times in a row, and `StateFlow` settled on the LAST write (Loading when buffering finished)
2. `startBufferedUpdate()` not cancelling predecessor, leaking 500ms Loading emitters
3. Desktop compared `bufferedPercentage * duration` against `currentPosition`, off by ~100×

#### Fix
- Rewrote state publication to only set Loading when actually starting to load
- Cancelled predecessor on each new update
- Fixed percentage calculation on Desktop

---

### Bug 6.2: Sleep Timer Abrupt Stop

**Severity**: Low  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Sleep timer ended with abrupt audio cut

#### Root Cause
Timer ended with bare `player.pause()`.

#### Fix
Ramp to silence over 5 seconds on equal-power (cosine) curve, then hold silence for 800ms before stopping. Uses separate `sleepFadeFactor` line, not `volume`.

---

### Bug 6.3: Seeking Mid-Crossfade Wrong Track

**Severity**: High  
**Status**: Fixed (2026-08-14)  
**Date Fixed**: 2026-08-14

#### Symptoms
- Seeking during crossfade sought the wrong track
- Outgoing track kept playing

#### Root Cause
`seekTo(positionMs)` didn't handle `isCrossfading`. Seek went to wrong player while position updates read from secondary.

#### Fix
Seek now commits the incoming track as current first, then seeks.

---

## 7. Third-Party Integration Bugs

### Bug 7.1: Last.fm Invalid Method Signature

**Severity**: Medium  
**Status**: Fixed (2026-07-30)  
**Date Fixed**: 2026-07-30

#### Symptoms
- Last.fm scrobbling failed with "Invalid method signature supplied" (code 13)

#### Root Cause
`format` parameter was included in `api_sig` calculation. Last.fm docs say to exclude it.

#### Fix
Exclude `format` (and `callback`) from signature calculation.

---

### Bug 7.2: Last.fm Wrong Auth Flow

**Severity**: High  
**Status**: Fixed (2026-07-30)  
**Date Fixed**: 2026-07-30

#### Symptoms
- Last.fm auth never completed - stuck on "return to application" page

#### Root Cause
Used desktop flow (`auth.getToken` first) instead of web flow. Desktop flow tells Last.fm app already holds token, so Last.fm shows "return to application" and never redirects.

#### Fix
Switched to web flow: send user to `last.fm/api/auth/?api_key=X` with NO token. Last.fm mints it and redirects to callback with `?token=`.

---

### Bug 7.3: PipePipe Decoder Offline

**Severity**: High  
**Status**: Fixed (2026-08-28)  
**Date Fixed**: 2026-08-28

#### Symptoms
- Playback failed when offline or when api.pipepipe.dev was down

#### Root Cause
Single point of failure - relied entirely on remote decoder.

#### Fix
Implemented three-tier decoding:
1. Farady table on-device
2. api.pipepipe.dev fallback
3. BravePipe final fallback

---

## 8. Platform-Specific Bugs

### Bug 8.1: Android 11 Splash Screen Scaling

**Severity**: Low  
**Status**: Fixed (2026-08-28)  
**Date Fixed**: 2026-08-28

#### Symptoms
- Splash screen displayed incorrectly on Android 11

#### Root Cause
Splash screen API behavior differs on Android 11.

#### Fix
Added `androidx.core:core-splashscreen` support with Android 11-specific handling.

---

### Bug 8.2: Desktop URL Schemes Not Registered

**Severity**: High  
**Status**: Fixed (2026-07-31)  
**Date Fixed**: 2026-07-31

#### Symptoms
- Deep links (`simpmusic://`, `simpmusic.org`) didn't work on packaged builds
- Last.fm callback never received

#### Root Cause
`url-schemes` was written at `mac.url-schemes` / `windows.url-schemes` / `linux.url-schemes` instead of at top level of `app` in conveyor.conf.

#### Fix
Moved `url-schemes` to correct location in HOCON hierarchy.

---

### Bug 8.3: Windows SMTC Crash

**Severity**: Critical  
**Status**: Fixed (2026-07)  
**Date Fixed**: 2026-07

#### Symptoms
- Desktop app crashed on Windows when SMTC was accessed
- ~95k Sentry events (SIMPMUSIC-DESKTOP-7)

#### Root Cause
COM apartment issues with `RPC_E_CHANGED_MODE`.

#### Fix
Hardened `SMTCAdapter.dll` with:
- Exception guards on every exported call
- MediaPlayer kept alive process-wide
- Dedicated thread off AWT EDT

---

## Debug Patterns & Anti-Patterns

### Patterns That Have Caused Bugs

1. **Dead Code on One of Two Paths**
   - Crossfade guards existed on only one trigger path
   - Always verify logic exists on ALL paths

2. **NOT IN Over Nullable Column**
   - `x NOT IN (…, NULL, …)` is NULL, never TRUE
   - Always add `WHERE col IS NOT NULL`

3. **LIKEsing Underscores**
   - VideoIds contain `_` which is a LIKE wildcard
   - Always escape with `ESCAPE '\'`

4. **Upstream Bug Appearing Only Under Load**
   - macOS CoreAudio crash appeared because SimpMusic runs 2 handles during crossfade
   - Test edge cases, not just happy paths

5. **Timezone Arithmetic Twice**
   - Writing LocalDateTime with UTC, then decoding with system default
   - Use converters consistently, bypass never

---

## Appendix: Bug Fix Checklist

When debugging new issues, check:

- [ ] Is this logic on ALL trigger paths, or just one?
- [ ] Are there nullable columns in NOT IN subqueries?
- [ ] Are LIKE wildcards properly escaped?
- [ ] Does this work with TWO concurrent instances (crossfade)?
- [ ] Is timezone conversion applied consistently?
- [ ] Does this work when the feature is OPTED OUT?
- [ ] Does this work on all platforms (Android, Desktop, iOS)?
- [ ] Does this work with the FOSS build vs Full build?

---

*Document generated for memory and debugging reference.*
