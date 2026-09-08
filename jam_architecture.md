# SimpMusic — New Jam Architecture

## Full Implementation Plan

---

# 1. Objective

Rebuild the SimpMusic Jam / Listen Together playback architecture so that:

1. The **server owns the authoritative Jam state**.
2. The **local ExoPlayer remains responsible for actual playback**.
3. The **Jam queue is synchronized independently from ExoPlayer's internal playback state**.
4. Host and authorized members can modify the shared queue/playback according to permissions.
5. Natural track transitions happen locally without unnecessary server round trips.
6. Server synchronization never causes duplicate skips, duplicate playback, or queue duplication.
7. Reconnection correctly restores the current track and playback position.
8. Crossfade and preloading continue to work through the existing `CrossfadeExoPlayerAdapter`.
9. Queue changes do not unnecessarily rebuild the entire ExoPlayer playlist.
10. All state changes are deterministic and resistant to WebSocket feedback loops.

The architecture must treat **Jam synchronization as reconciliation**, not as a second media player.

---

# 2. Core Architecture

The system should have three distinct layers.

```text
                    ┌─────────────────────────┐
                    │      Jam Server         │
                    │                         │
                    │ Authoritative room      │
                    │ playback + queue state  │
                    └────────────┬────────────┘
                                 │
                           WebSocket
                                 │
                ┌────────────────▼────────────────┐
                │       Jam Playback Controller   │
                │                                 │
                │ Reconciles server state with   │
                │ local playback state            │
                └───────────────┬─────────────────┘
                                │
                         Local commands
                                │
                ┌───────────────▼─────────────────┐
                │       CrossfadeExoPlayerAdapter  │
                │                                 │
                │ Actual local playback           │
                │ buffering / preload / crossfade │
                └─────────────────────────────────┘
```

The three layers have different responsibilities.

---

# 3. Server Responsibilities

The server is the authority for the logical Jam room.

The server owns:

* current track
* upcoming queue
* play/pause intent
* authoritative playback position
* server timestamp / clock
* queue version
* playback revision
* track generation
* room members
* host
* permissions
* reconnect session state

The server does **not** directly control ExoPlayer.

The server only describes what the room should logically be doing.

---

# 4. ExoPlayer Responsibilities

ExoPlayer is responsible for:

* loading media
* buffering
* decoding
* actual playback
* pausing
* seeking
* detecting track end
* preloading
* crossfade
* local playback errors

ExoPlayer must NOT become the authority for:

* Jam queue ownership
* room membership
* room permissions
* authoritative current track
* authoritative play/pause state

---

# 5. Jam Playback Controller

Create or maintain a single central controller responsible for reconciliation.

Conceptually:

```text
ListenTogetherPlaybackBridge
        │
        ├── observes Jam server state
        ├── observes local ExoPlayer state
        ├── handles local user commands
        ├── handles natural transitions
        ├── updates Jam queue
        └── reconciles differences
```

Do not spread Jam synchronization logic across:

* ViewModel
* MediaService
* Repository
* ExoPlayer adapter
* UI
* WebSocket callbacks

There must be one clear synchronization layer.

---

# 6. Authoritative Jam State

Represent the room state approximately as:

```text
JamRoomState

currentTrack
queue[]
playbackState
position
serverTimestamp
queueVersion
playbackRevision
trackGeneration
hostId
members[]
permissions[]
```

Where:

```text
playbackState =
    PLAYING
    PAUSED
```

The server should not use `isPlaying == false` as its logical pause state.

---

# 7. Separate Intent From Player State

This distinction is critical.

Jam state:

```text
PLAYING
PAUSED
```

ExoPlayer state:

```text
IDLE
BUFFERING
READY
PLAYING
ENDED
ERROR
```

Never do this:

```kotlin
if (!player.isPlaying) {
    roomPaused = true
}
```

Because this is incorrect while:

* buffering
* preparing
* loading
* switching tracks
* seeking
* recovering from errors

The Jam server stores user intent.

ExoPlayer reports implementation state.

---

# 8. Shared Queue Model

The Jam queue should conceptually look like:

```text
Current:
A

Queue:
B
C
D
```

The current track is NOT also part of the upcoming queue.

The invariant is:

```text
currentTrack != queue[0]
```

unless the architecture explicitly represents a queue differently.

For this implementation use:

```text
Current = currently playing item

Queue = only upcoming items
```

---

# 9. Queue Invariant

This must always remain true.

Before:

```text
Current: A
Queue:   B C D
```

One Next:

```text
Current: B
Queue:   C D
```

Never:

```text
Current: B
Queue:   B C D
```

And never:

```text
Current: C
Queue:   D
```

after a single Next.

A single user action must produce exactly one logical transition.

---

# 10. Queue Ownership

The server owns the logical Jam queue.

The local ExoPlayer adapter owns a local playback window.

These are NOT the same data structure.

Example:

```text
SERVER JAM QUEUE

Current: A
Queue: B C D E F
```

Local player may contain:

```text
A B C
```

or:

```text
A B C D
```

depending on preload requirements.

This is intentional.

---

# 11. Local Playback Window

Maintain a sliding local playback window.

Recommended:

```text
[Current, Next, Next+1]
```

Example:

```text
Server:
Current = B
Queue = C D E F

Local ExoPlayer:
B C D
```

When advancing:

```text
Server:
Current = C
Queue = D E F

Local:
C D E
```

The local playlist should be updated incrementally.

Do NOT recreate the entire player playlist for every server queue update.

---

# 12. Existing CrossfadeExoPlayerAdapter

Reuse the existing:

```text
CrossfadeExoPlayerAdapter
```

Do not create a second Jam-specific media player.

The existing adapter already handles:

* internal playlist
* local current index
* separate ExoPlayer instances
* precaching
* crossfade
* media replacement
* seeking
* player lifecycle

Jam should integrate with this adapter instead of duplicating those systems.

---

# 13. Important CrossfadeExoPlayerAdapter Architecture

The adapter does not behave like a simple:

```text
ExoPlayer.setMediaItems(queue)
```

implementation.

It maintains:

```text
playlist
localCurrentMediaItemIndex
precachedPlayers
currentPlayer
```

and may create separate ExoPlayer instances for tracks.

Therefore Jam code must not assume:

```text
server queue index == ExoPlayer index
```

They are different representations.

---

# 14. Server → Client Synchronization

When a server state update arrives:

```text
WebSocket
   ↓
Jam Repository
   ↓
Jam Playback Controller
   ↓
Reconcile
   ↓
CrossfadeExoPlayerAdapter
```

The server event means:

> "This is the authoritative room state."

It does NOT mean:

> "Press Next."

This distinction prevents double advancement.

---

# 15. Never Treat Server Echo as a Command

Example:

Host presses Next.

Bad implementation:

```text
Host presses Next
        ↓
Local player advances B
        ↓
Server changes currentTrack → B
        ↓
Host receives B
        ↓
Client executes next()
        ↓
Player advances C
```

Result:

```text
A → B → C
```

from one Next.

Correct implementation:

```text
Host presses Next
        ↓
Determine target B
        ↓
Update server to B
        ↓
Local player reconciles to B
        ↓
Server echo confirms B
        ↓
No additional advance
```

The server event must be interpreted as:

```text
SYNC TO B
```

not:

```text
NEXT
```

---

# 16. Transition Types

Every transition should have an explicit reason.

Define:

```text
USER_NEXT
USER_PREVIOUS
USER_SELECT
NATURAL_ADVANCE
CROSSFADE
SERVER_SYNC
QUEUE_RECONCILIATION
INITIAL_LOAD
RECONNECT
ERROR_RECOVERY
```

Do not rely on ambiguous callbacks.

For example:

```kotlin
advanceTo(track, TransitionReason.SERVER_SYNC)
```

is safer than:

```kotlin
next()
```

inside every callback.

---

# 17. User Next Flow

When the host or authorized member presses Next:

```text
UI
 ↓
ViewModel
 ↓
Jam Repository
 ↓
Jam Controller
 ↓
determine next track
 ↓
send authoritative server mutation
```

Do not independently call:

```text
player.next()
```

and:

```text
server.changeTrack()
```

unless the implementation has a deliberate optimistic-state mechanism.

Prefer one logical operation.

---

# 18. Recommended Next Algorithm

Given:

```text
Current = A
Queue = B C D
```

Next should calculate:

```text
target = B
newCurrent = B
newQueue = C D
```

Then send:

```text
CHANGE_TRACK {
    track = B
}
```

or an equivalent authoritative server mutation.

The server must validate and atomically apply:

```text
Current = B
Queue = C D
```

Then broadcast the resulting state.

---

# 19. Server CHANGE_TRACK

The server's track-change operation should not simply replace:

```text
currentTrack = requestedTrack
```

without considering the queue.

For normal Next behavior:

```text
oldCurrent = A
queue = [B,C,D]

target = B

currentTrack = B
queue = [C,D]
```

The operation must be atomic.

Do not allow clients to temporarily observe:

```text
current = B
queue = [B,C,D]
```

---

# 20. Selecting a Specific Queue Item

Suppose:

```text
Current = A
Queue = B C D E
```

User selects D.

The resulting state should be:

```text
Current = D
Queue = E
```

if the intended semantics are "play selected track and discard previous upcoming tracks."

If the desired semantics are instead "play D but preserve later queue," use:

```text
Current = D
Queue = E
```

or the project-defined behavior consistently.

The important requirement is that the selected current track must not remain at the head of the upcoming queue.

---

# 21. Member Permissions

Permissions determine who may mutate shared state.

Example:

```text
Host:
    change track
    play/pause
    seek
    add queue
    remove queue
    reorder queue

Authorized member:
    permissions determined by room settings

Normal member:
    playback actions denied
```

The server must enforce permissions.

Never rely only on the Android UI hiding buttons.

---

# 22. Member Next

When an authorized member presses Next:

```text
Member
 ↓
server CHANGE_TRACK
 ↓
server validates permission
 ↓
server calculates authoritative new state
 ↓
broadcast state
 ↓
all clients reconcile
```

The member's local player should not independently perform a second Next.

This prevents:

```text
Member local → B
Server → B
Client receives B → next() → C
```

---

# 23. Optimistic UI

If desired, the UI may immediately show a loading state after sending a mutation.

However, do not optimistically mutate the logical queue in two separate places.

Use:

```text
pendingOperationId
```

or similar operation tracking.

Example:

```text
operationId = NEXT-123
```

When the server responds:

```text
operationId = NEXT-123
```

the controller knows which local operation the response belongs to.

---

# 24. Operation IDs

Every user-triggered mutation should ideally have an operation ID.

Example:

```text
operationId: 8A71F
type: CHANGE_TRACK
sender: user123
```

This makes debugging and deduplication much easier.

---

# 25. Queue Version

Every queue mutation increments:

```text
queueVersion
```

Example:

```text
queueVersion = 41
```

after one mutation:

```text
queueVersion = 42
```

Clients should ignore stale queue updates.

Example:

```text
received version 41
local version 42
```

Ignore version 41.

---

# 26. Playback Revision

Use a separate:

```text
playbackRevision
```

for playback-state changes.

For example:

```text
queueVersion = 42
playbackRevision = 108
```

This allows queue mutations and playback mutations to be tracked independently.

---

# 27. Track Generation

Each logical current-track generation should have a unique ID.

Example:

```text
trackGeneration = 91A2
```

When:

```text
A → B
```

generate a new generation.

Callbacks from A must never mutate B.

This protects against delayed asynchronous callbacks.

---

# 28. Stale Callback Protection

Before acting on callbacks, verify:

```text
callbackGeneration == currentGeneration
```

If not:

```text
ignore callback
```

This is especially important for:

* old ExoPlayer instances
* crossfade callbacks
* delayed coroutines
* asynchronous remove/add operations
* WebSocket callbacks
* preload completion

---

# 29. Natural Track End

Natural playback should be handled locally.

Example:

```text
Current A
Queue B C D
```

A reaches the end.

The local adapter already has:

```text
A B C
```

Therefore:

```text
A → B
```

should happen locally.

Do NOT wait for:

```text
server → B
```

before allowing playback to continue.

---

# 30. Host Natural Advance

After the local player naturally transitions:

```text
A → B
```

the host should publish the authoritative state:

```text
Current = B
Queue = C D
```

But the host must NOT reload B when its own server update comes back.

The local player is already playing B.

The server echo is only confirmation.

---

# 31. Natural Advance Algorithm

Conceptually:

```text
onLocalNaturalAdvance(B):

    if B is valid local next:
        continue playback locally

        if local user is host:
            publish authoritative transition B

    else:
        request/fallback server transition
```

The important part:

```text
local transition != server transition command
```

---

# 32. Member Natural Advance

A member should not independently mutate the Jam state unless the protocol explicitly allows members to do so.

For example:

```text
Member local A → B
```

can happen because B is already the authoritative next track.

The member should reconcile to the server state rather than independently declaring:

```text
Current = B
```

unless authorized.

---

# 33. Crossfade

Crossfade should remain a local playback concern.

The local adapter may:

```text
preload B
fade A
start B
fade A out
```

The server only needs the resulting logical state.

Do not send server commands for every volume fade or internal player swap.

---

# 34. Crossfade Must Not Cause Double Advance

Crossfade may cause multiple player callbacks.

Only one logical transition may be emitted.

Use:

```text
trackGeneration
transitionId
localCurrentMediaItemIndex
```

to prevent duplicate transition processing.

---

# 35. Queue Updates

When the server queue changes but current track remains unchanged:

```text
Current = A
Queue = B C D
```

becomes:

```text
Current = A
Queue = B D E
```

The local controller should update the local playback window.

Do not:

```text
clear player
set new playlist
prepare
play
```

every time.

---

# 36. Minimal Queue Diff

Compare:

```text
old server queue
new server queue
```

and determine:

* added items
* removed items
* moved items
* unchanged items

Then apply minimal changes to the local adapter.

---

# 37. CrossfadeExoPlayerAdapter Safety

The adapter's internal playlist must be mutated synchronously before asynchronous work can queue another conflicting mutation.

This is particularly important for:

```text
removeMediaItem()
```

because a previous implementation had:

```text
check index
↓
launch coroutine
↓
remove later
```

Multiple queued operations could therefore observe the same old playlist size.

---

# 38. Remove Operation Safety

Bad:

```kotlin
if (index < playlist.size) {
    scope.launch {
        playlist.removeAt(index)
    }
}
```

because another operation may execute before the coroutine.

Instead, make the internal logical playlist mutation immediately consistent.

Conceptually:

```text
validate
↓
mutate internal playlist/index synchronously
↓
schedule player-side asynchronous work
```

---

# 39. Adapter Invariants

At all times:

```text
0 <= localCurrentMediaItemIndex < playlist.size
```

when playlist is non-empty.

When playlist is empty:

```text
localCurrentMediaItemIndex = INVALID
```

Every:

* add
* insert
* remove
* move
* clear
* replace
* set
* seek

operation must preserve this invariant.

---

# 40. Bounds Protection

Any code doing:

```kotlin
playlist[index]
```

must verify:

```kotlin
index in playlist.indices
```

especially in asynchronous callbacks.

Do not assume the playlist is unchanged between scheduling and execution.

---

# 41. Queue Synchronization Algorithm

On every authoritative server state:

```text
receive state
 ↓
validate version
 ↓
validate current generation
 ↓
compare server current with local current
 ↓
compare server queue with local Jam queue
 ↓
calculate minimal changes
 ↓
reconcile local playback window
 ↓
reconcile play/pause
 ↓
reconcile position if required
```

---

# 42. Current Track Reconciliation

If:

```text
serverCurrent == localCurrent
```

do not reload the track.

If:

```text
serverCurrent != localCurrent
```

determine why.

Possible reasons:

```text
USER_NEXT
USER_SELECT
NATURAL_ADVANCE
RECONNECT
SERVER_SYNC
ERROR_RECOVERY
```

Then transition exactly once.

---

# 43. Avoid Unnecessary Seek

When receiving a server state:

```text
Current = B
Position = 37s
```

and local player is already:

```text
B
Position = 36.8s
```

do not seek every time the WebSocket sends a state update.

Only correct meaningful drift.

Example concept:

```text
if abs(localPosition - authoritativePosition) > threshold:
    seek
```

Use a sensible threshold rather than constantly seeking.

---

# 44. Playback Position

Server position must be based on:

```text
authoritative position
+
server elapsed time
```

when room state is PLAYING.

For example:

```text
positionNow =
    storedPosition +
    (serverNow - stateTimestamp)
```

Do not let every client independently become the authority.

---

# 45. Pause Position

When the user pauses:

```text
localPosition = player.currentPosition
```

must be sent with the pause request.

The server should store that position.

Do not reset to:

```text
0
```

when pausing.

---

# 46. Pause Fallback

If a pause request contains an invalid or zero position while the room is playing, the server may calculate the current live position from the authoritative timeline.

This prevents a pause from accidentally resetting playback to the beginning.

---

# 47. Play

When PLAY is requested:

```text
server playbackState = PLAYING
server timestamp = now
```

Clients calculate the current position from the server timeline.

They then:

```text
prepare if necessary
seek if necessary
play
```

---

# 48. Reconnect

When a guest reconnects:

```text
connect
 ↓
authenticate session/token
 ↓
join/recover room
 ↓
request authoritative state
 ↓
receive current track
 ↓
receive queue
 ↓
calculate current position
 ↓
prepare current
 ↓
preload next
 ↓
seek
 ↓
play/pause according to authoritative intent
```

Do not replay every command the guest missed while disconnected.

The room state is the source of truth.

---

# 49. Reconnect Session

Disconnected users may retain a reconnect session/token for a limited period.

The server should:

```text
remove user from active room membership
```

while retaining reconnect information.

When reconnecting:

```text
restore existing member identity
```

rather than creating a duplicate member.

---

# 50. Duplicate Membership Protection

Before adding a reconnecting member:

```text
if user already exists:
    update existing member/session

else:
    create member
```

Never produce:

```text
Mark
Mark
```

as two members because of reconnect.

---

# 51. Disconnect

When a WebSocket disconnects:

```text
remove active user from room.State.Users
broadcast UserLeft
```

If host disconnects:

```text
select/transmit new host
```

according to the existing host-transfer rules.

Preserve reconnect information separately.

---

# 52. Android Lifecycle

`SimpleMediaService` should clean up its Jam session when appropriate.

For example:

```text
onTaskRemoved()
onDestroy()
```

should notify/leave as required.

However, server-side WebSocket disconnect handling must remain the actual authority for membership cleanup.

---

# 53. ViewModel Responsibilities

The ViewModel should primarily handle:

```text
UI events
 ↓
repository/controller
```

Examples:

```text
onNextClicked()
onPreviousClicked()
onPlayClicked()
onPauseClicked()
onQueueItemSelected()
```

It should NOT contain the complete synchronization algorithm.

---

# 54. Repository Responsibilities

Repository/session layer handles:

* WebSocket communication
* room state streams
* sending mutations
* session/token management
* serialization
* server responses

It should not directly manipulate ExoPlayer.

---

# 55. MediaService Responsibilities

MediaService owns the actual local playback integration.

It communicates with:

```text
Jam Playback Controller
```

rather than having independent Jam synchronization logic scattered through MediaService callbacks.

---

# 56. WebSocket Event Handling

All WebSocket state events should pass through one controlled path.

Example:

```text
WebSocket event
 ↓
deserialize
 ↓
validate revision/version
 ↓
update repository state
 ↓
controller receives state
 ↓
reconcile
```

Do not have several independent observers directly changing the player.

---

# 57. Prevent Feedback Loops

Avoid:

```text
server state
 ↓
player change
 ↓
player callback
 ↓
server mutation
 ↓
server state
 ↓
player change
```

unless the transition was genuinely local/natural.

Track the source of every transition.

---

# 58. Source Tracking

Maintain something conceptually like:

```text
lastTransitionSource
```

Possible values:

```text
LOCAL_USER
SERVER
NATURAL
CROSSFADE
RECONNECT
ERROR
```

When applying a server state:

```text
source = SERVER
```

and player callbacks caused by that reconciliation must not publish the same transition back to the server.

---

# 59. Pending Transition

Optionally maintain:

```text
pendingTransition
```

Example:

```text
pending:
    operationId = NEXT-123
    target = B
```

When server confirms:

```text
Current = B
```

clear the pending operation.

If another unexpected transition occurs:

```text
Current = C
```

while B was expected, log it as a synchronization violation.

---

# 60. Debug Logging

Add temporary structured logs.

Use:

```text
[JAM-SKIP-TRACE]
```

for Next/track transitions.

Every transition should log:

```text
operationId
userId
role
transitionType

serverCurrent
serverQueue

localCurrent
localQueue

ExoPlayer playlist
localCurrentMediaItemIndex

queueVersion
playbackRevision
trackGeneration

player state
player position
```

---

# 61. Example Trace

A correct Next should look approximately like:

```text
[JAM-SKIP-TRACE]
op=NEXT-123
source=HOST
type=USER_NEXT
before:
serverCurrent=A
serverQueue=[B,C,D]

target=B

server mutation sent
```

Then:

```text
[JAM-SKIP-TRACE]
op=NEXT-123
type=SERVER_SYNC
serverCurrent=B
serverQueue=[C,D]

localCurrent=B

ACTION=RECONCILE_ONLY
```

There must NOT be another:

```text
NEXT
```

after this.

---

# 62. Error Detection

Log a synchronization violation if:

```text
one user operation
```

causes more than one logical track transition.

Example:

```text
operation NEXT-123

A → B
B → C
```

Flag:

```text
[JAM-SYNC-ERROR] MULTIPLE_ADVANCE
```

---

# 63. Queue Duplication Detection

At every authoritative state:

```text
if currentTrack exists in queue:
    log JAM-SYNC-ERROR DUPLICATE_CURRENT
```

This immediately detects:

```text
Current = B
Queue = B C D
```

---

# 64. Server Atomicity

Queue/current-track updates must be atomic.

Do not broadcast:

```text
current=B
```

before updating:

```text
queue=[C,D]
```

The broadcast should contain one consistent snapshot.

---

# 65. Server State Snapshot

Broadcast one complete logical state whenever practical:

```text
JamRoomState {
    currentTrack
    queue
    playbackState
    position
    timestamp
    queueVersion
    playbackRevision
    trackGeneration
}
```

Clients should reconcile against this snapshot.

---

# 66. Do Not Broadcast Internal Player State

Do not send:

```text
BUFFERING
READY
volume
crossfade progress
local preload state
```

as authoritative Jam state.

Those are local implementation details.

---

# 67. Error Recovery

If the current track fails locally:

```text
player ERROR
 ↓
controller determines whether next track exists
 ↓
attempt local recovery
```

If recovery requires changing the logical current track:

```text
perform one authoritative transition
```

Do not allow the error callback and server callback to independently skip.

---

# 68. Queue Exhaustion

If:

```text
Current = A
Queue = []
```

and A ends:

```text
room has no next track
```

Then:

```text
stop / pause / remain ended
```

according to existing product behavior.

Do not accidentally loop A.

---

# 69. Previous

Previous should follow the same architecture.

Do not simply call:

```text
player.previous()
```

and separately mutate the Jam queue.

Instead:

```text
determine target
 ↓
authoritative state mutation
 ↓
local reconciliation
```

If previous-track history is needed, explicitly model it rather than relying on ExoPlayer's internal history.

---

# 70. Queue Add

When adding:

```text
A
Queue B C
```

and adding D:

```text
A
Queue B C D
```

Server:

```text
queueVersion++
broadcast state
```

Local controller updates only the required local window.

---

# 71. Queue Remove

Removing B:

```text
Before:
A
B C D
```

becomes:

```text
A
C D
```

If B was locally preloaded:

```text
remove B from local playback window
```

unless it is currently playing.

Never remove the current item accidentally.

---

# 72. Queue Reorder

Example:

```text
A
B C D
```

move D to front:

```text
A
D B C
```

If local window becomes:

```text
A B C
```

it should be updated to:

```text
A D B
```

without restarting A.

---

# 73. Current Track Must Be Protected

Queue reconciliation must never accidentally insert the current track into the upcoming queue.

When constructing the local playback window:

```text
current
+
upcoming items
```

must be deduplicated.

---

# 74. Local Playlist Construction

Conceptually:

```kotlin
val localItems =
    listOf(currentTrack) +
    serverQueue.take(PRELOAD_COUNT)
```

Then compare with the existing adapter playlist.

Do not blindly clear and rebuild.

---

# 75. Existing Local Playlist

The adapter may have:

```text
A B C
```

Server changes queue:

```text
A B D
```

Desired result:

```text
A B D
```

not:

```text
clear
load A
load B
load D
```

unless absolutely necessary.

---

# 76. Local Preloading

Use the existing adapter's precaching system.

For:

```text
Current A
Queue B C
```

prepare:

```text
A
B
C
```

as appropriate.

Preloading should not alter the authoritative Jam queue.

---

# 77. Preload Failure

If B fails to preload:

```text
do not remove B from Jam queue
```

unless the media is actually unavailable and product behavior explicitly requires removal.

A preload failure is a local playback issue, not automatically a logical queue mutation.

---

# 78. Server Clock

The server should provide a timestamp/clock reference.

Clients calculate playback position from:

```text
serverTime
```

rather than relying on device clocks being synchronized.

---

# 79. Clock Drift

Do not continuously seek because of tiny clock differences.

Use drift correction thresholds.

Small differences:

```text
ignore
```

Large differences:

```text
seek
```

---

# 80. Initial Jam Join

When entering an existing room:

```text
receive state
```

then:

```text
set local Jam queue
set current
prepare current
preload next
calculate position
seek
apply PLAY/PAUSE
```

Do not simulate the history of the room.

---

# 81. Joining During Playback

Suppose the room is:

```text
Current = B
Queue = C D
Position = 72 seconds
PLAYING
```

A new member joins.

They should start approximately at:

```text
B @ current authoritative position
```

not:

```text
A
```

and not:

```text
B @ 0
```

---

# 82. Joining During Pause

If:

```text
PAUSED
position = 91
```

the new client should:

```text
prepare B
seek 91
remain paused
```

Do not automatically start playback.

---

# 83. Reconnect During Playback

Same rule as initial join.

Use the authoritative snapshot.

Do not replay missed:

```text
PLAY
NEXT
NEXT
PAUSE
PLAY
```

commands.

Just reconstruct the current state.

---

# 84. Membership State

Room state should distinguish:

```text
active members
```

from:

```text
reconnectable sessions
```

Do not keep disconnected users permanently visible in active members.

---

# 85. Permission State

Permission changes should be server authoritative.

If a user loses permission:

```text
UI controls update
```

but more importantly:

```text
server rejects future mutation
```

---

# 86. Server Validation

Every mutation must validate:

```text
room exists
user belongs to room
session valid
permission valid
request valid
```

before modifying state.

---

# 87. Idempotency

Repeated WebSocket messages or retry behavior should not cause repeated mutations.

Where practical, use:

```text
operationId
```

to identify requests.

If the same operation is received twice:

```text
return existing result
```

instead of applying it twice.

---

# 88. One Logical Mutation

A single user action must produce:

```text
ONE server mutation
ONE logical state transition
ONE resulting authoritative snapshot
```

The client may receive that snapshot multiple times, but reconciliation must remain idempotent.

---

# 89. Idempotent Reconciliation

Applying the same server state twice:

```text
State B / Queue C D
```

should produce no additional playback transition after the first application.

This is essential.

---

# 90. Example: Correct Host Next

Initial:

```text
Current = A
Queue = B C D
```

Host presses Next.

```text
1. Determine B
2. Send CHANGE_TRACK(B)
3. Server atomically:
       Current = B
       Queue = C D
4. Broadcast snapshot
5. Host receives snapshot
6. Host sees local player already/needs to become B
7. Reconcile to B
8. Do NOT call next()
```

Final:

```text
Current = B
Queue = C D
```

---

# 91. Example: Correct Member Next

Initial:

```text
Current = A
Queue = B C D
```

Authorized member presses Next.

```text
1. Member sends CHANGE_TRACK(B)
2. Server validates permission
3. Server sets:
       Current = B
       Queue = C D
4. Broadcast
5. Member reconciles to B
6. Host reconciles to B
7. All clients remain synchronized
```

Final:

```text
Current = B
Queue = C D
```

---

# 92. Example: Natural Advance

Initial:

```text
Current = A
Queue = B C D
```

A naturally ends.

Local adapter:

```text
A → B
```

Host:

```text
publish Current=B
```

Server:

```text
Current=B
Queue=C D
```

Clients:

```text
reconcile
```

No client performs an additional Next.

---

# 93. Example: Queue Change During Playback

Initial:

```text
Current=A
Queue=B C D
```

Member adds E.

Server:

```text
Current=A
Queue=B C D E
queueVersion++
```

Local player remains:

```text
A B C
```

and may preload E later depending on window size.

A does not restart.

---

# 94. Example: Queue Remove During Playback

Initial:

```text
Current=A
Queue=B C D
Local=A B C
```

B is removed.

Server:

```text
Current=A
Queue=C D
```

Local:

```text
A C D
```

A keeps playing uninterrupted.

---

# 95. Example: Reconnect

Server:

```text
Current=C
Queue=D E
PLAYING
position=43
```

Guest reconnects.

Client:

```text
load C
preload D
seek C → 43s
play
```

No skipped tracks.

---

# 96. Required Code Changes

Audit the entire Jam playback path.

At minimum inspect:

```text
ListenTogetherPlaybackBridge
ListenTogetherSession
ListenTogetherRepository
ViewModel
MediaService
CrossfadeExoPlayerAdapter
Jam WebSocket client
Go WebSocket server
room state model
queue mutation handlers
CHANGE_TRACK handlers
play/pause handlers
reconnect handlers
disconnect handlers
```

Do not modify unrelated playback behavior.

---

# 97. Search for Competing Next Calls

Search the project for:

```text
next()
seekToNext()
handleAutoAdvance
CHANGE_TRACK
publishTrackChangesAsHost
currentTrack
onMediaItemTransition
STATE_ENDED
Player.STATE_ENDED
```

Map every place that can cause:

```text
Current A → B
```

There must be a single controlled path.

---

# 98. Search for Queue Ownership

Search for every place that modifies:

```text
queue
playlist
currentTrack
localCurrentMediaItemIndex
```

Classify each mutation:

```text
SERVER LOGICAL QUEUE
LOCAL PLAYBACK WINDOW
```

They must not be mixed.

---

# 99. Search for Server Echo Handling

Find every observer of:

```text
watchRoomPlayback
roomState
currentTrack
playbackState
queue
```

Ensure none of them interprets:

```text
currentTrack changed
```

as:

```text
call next()
```

Instead:

```text
reconcileToCurrentTrack()
```

---

# 100. Remove Legacy Jam Playback Paths

Once the new controller owns synchronization, remove or disable old competing logic.

Do not leave:

```text
old Jam auto-advance
+
new Jam auto-advance
```

running simultaneously.

Do not leave:

```text
old queue mutation
+
new queue mutation
```

running simultaneously.

---

# 101. Crossfade Guards

The existing Crossfade adapter previously contained Jam-specific blocking conditions.

Audit and remove obsolete conditions such as:

```text
!inJamRoom
crossfadeSuppressed
```

if they prevent the new architecture from working.

However, do not blindly remove legitimate safety guards.

The rule should be:

```text
Jam synchronization controls logical state.
Crossfade adapter controls local playback.
```

---

# 102. `playTrack()` Behavior

`playTrack()` must distinguish:

```text
target already exists locally
```

from:

```text
target does not exist locally
```

If target is already part of the local playback window:

```text
move/reconcile to target
```

without unnecessary playlist destruction.

If target is not present:

```text
load target
```

and reconstruct the necessary local window.

---

# 103. `handleAutoAdvanceAsHost()`

Natural end handling must be simplified.

If the local playlist has a valid next item:

```text
do not manually trigger another server Next
```

if the adapter already performs the natural transition.

Instead:

```text
observe resulting transition
publish authoritative state
```

Only use server fallback when the local playlist genuinely has no next item.

---

# 104. Queue-Only Changes

When only the Jam queue changes:

```text
Current remains unchanged
```

there should be:

```text
queue reconciliation
```

not:

```text
playTrack(current)
```

unless the local current item is actually missing.

---

# 105. Play/Pause

Member Play/Pause should follow:

```text
UI
 ↓
repository
 ↓
server
 ↓
authoritative playback state
 ↓
all clients reconcile
```

Host may use local optimistic behavior if carefully tracked, but server remains authoritative.

---

# 106. Testing Strategy

Do not test only the happy path.

Test:

```text
host Next
member Next
host rapid Next
member rapid Next
Next while buffering
Next during crossfade
Next during preload
Next during queue mutation
queue remove current-next
queue reorder during playback
natural advance
natural advance during server update
pause during buffering
pause during crossfade
join while playing
join while paused
reconnect while playing
reconnect after multiple transitions
host disconnect
member disconnect
host transfer
duplicate WebSocket message
stale state version
stale playback revision
old player callback
```

---

# 107. Critical Acceptance Test: Single Next

Start:

```text
Current = A
Queue = B C D
```

Press Next exactly once.

Expected:

```text
Current = B
Queue = C D
```

Required:

```text
exactly one logical transition
```

Fail if:

```text
Current = C
```

or:

```text
Current = B
Queue = B C D
```

---

# 108. Critical Acceptance Test: Member Next

Start:

```text
Current = A
Queue = B C D
```

Authorized member presses Next once.

Expected on every client:

```text
Current = B
Queue = C D
```

No duplicate B.

No C.

---

# 109. Critical Acceptance Test: Natural Advance

Start:

```text
Current=A
Queue=B C D
```

Allow A to naturally finish.

Expected:

```text
A → B
```

Final:

```text
Current=B
Queue=C D
```

No extra server-induced skip.

---

# 110. Critical Acceptance Test: Repeated State

Send the exact same authoritative state twice:

```text
Current=B
Queue=C D
```

Expected:

```text
first snapshot → reconcile
second snapshot → no new transition
```

---

# 111. Critical Acceptance Test: Reconnect

Start:

```text
Current=B
Queue=C D
PLAYING
```

Disconnect member.

Advance to:

```text
Current=C
Queue=D
```

Reconnect member.

Expected:

```text
Current=C
Queue=D
```

The member must not replay B.

---

# 112. Critical Acceptance Test: Queue Invariant

After every mutation assert:

```text
currentTrack !in queue
```

unless explicitly required by the product's queue model.

Also assert:

```text
queue contains no duplicate track IDs
```

if duplicates are not intentionally supported.

---

# 113. Critical Acceptance Test: Adapter Bounds

Run:

```text
add
remove
move
clear
replace
set
seek
```

under rapid queue updates.

Ensure there is no:

```text
IndexOutOfBoundsException
```

especially around:

```text
removeMediaItem
```

---

# 114. Logging Acceptance

Every transition should be traceable from:

```text
user action
→ server mutation
→ server state
→ WebSocket event
→ local reconciliation
→ ExoPlayer transition
```

If a skip happens twice, logs must identify the exact second transition source.

---

# 115. Implementation Order

Do NOT attempt to rewrite everything simultaneously.

Use this order:

### Phase 1 — Audit

Map:

```text
UI
ViewModel
Repository
Session
Bridge
MediaService
Adapter
WebSocket
Server
```

and identify all playback/queue mutations.

---

### Phase 2 — Server State

Implement/verify:

```text
currentTrack
queue
playbackState
position
timestamp
queueVersion
playbackRevision
trackGeneration
permissions
members
```

---

### Phase 3 — Server Atomic Mutations

Implement:

```text
CHANGE_TRACK
PLAY
PAUSE
QUEUE_ADD
QUEUE_REMOVE
QUEUE_MOVE
```

with atomic state updates.

---

### Phase 4 — Client State Model

Create the Jam controller state model.

Separate:

```text
authoritative Jam state
local player state
pending operations
```

---

### Phase 5 — Reconciliation

Implement:

```text
server state
 ↓
controller
 ↓
local playback window
```

with idempotent reconciliation.

---

### Phase 6 — Local Natural Playback

Make:

```text
A → B
```

happen locally through the existing adapter.

Host publishes the resulting authoritative state.

---

### Phase 7 — User Commands

Implement:

```text
Next
Previous
Select
Play
Pause
```

through the authoritative mutation path.

---

### Phase 8 — Queue Synchronization

Implement:

```text
add
remove
move
```

using minimal local playlist diffs.

---

### Phase 9 — Reconnect

Implement authoritative snapshot restoration.

---

### Phase 10 — Permissions

Verify host/member permission enforcement on the server.

---

### Phase 11 — Cleanup

Remove obsolete Jam playback paths and duplicate observers.

---

### Phase 12 — Stress Testing

Test rapid actions and asynchronous race conditions.

---

# 116. Important Anti-Patterns

Do NOT implement:

```text
server currentTrack changed → player.next()
```

Do NOT implement:

```text
player.next() + server CHANGE_TRACK
```

as two independent actions.

Do NOT implement:

```text
queue update → clear ExoPlayer playlist
```

every time.

Do NOT implement:

```text
isPlaying=false → Jam paused
```

Do NOT implement:

```text
preload failure → remove Jam queue item
```

without explicit product logic.

Do NOT use:

```text
local ExoPlayer index
```

as the server queue index.

Do NOT replay missed commands after reconnect.

Do NOT allow multiple classes to independently own Jam advancement.

---

# 117. Final Architecture

The finished architecture should effectively behave like:

```text
                 ┌──────────────────────┐
                 │      JAM SERVER      │
                 │                      │
                 │ Current Track        │
                 │ Upcoming Queue       │
                 │ Play/Pause Intent    │
                 │ Position             │
                 │ Versions/Revisions   │
                 │ Permissions          │
                 └──────────┬───────────┘
                            │
                         WebSocket
                            │
                 ┌──────────▼───────────┐
                 │   JAM CONTROLLER     │
                 │                      │
                 │ User Commands        │
                 │ Server Reconcile     │
                 │ Transition Tracking  │
                 │ Queue Diffing        │
                 │ Generation Safety    │
                 └──────────┬───────────┘
                            │
                     Local Playback
                            │
                 ┌──────────▼───────────┐
                 │ CROSSFADE ADAPTER    │
                 │                      │
                 │ ExoPlayer            │
                 │ Preload               │
                 │ Crossfade             │
                 │ Buffering             │
                 │ Local Playlist        │
                 └──────────────────────┘
```

The key rule is:

> **The server decides what the Jam is playing. ExoPlayer decides how the local device plays it. The Jam Controller is the only layer that reconciles the two.**

---

# 118. Definition of Done

The implementation is complete only when all of these are true:

* [ ] Server owns authoritative current track.
* [ ] Server owns authoritative upcoming queue.
* [ ] Current track is not duplicated in upcoming queue.
* [ ] Server updates current + queue atomically.
* [ ] Queue versions exist.
* [ ] Playback revisions exist.
* [ ] Track generations prevent stale callbacks.
* [ ] User operations have traceable IDs.
* [ ] Host Next advances exactly one track.
* [ ] Member Next advances exactly one track.
* [ ] Server echo never causes another Next.
* [ ] Natural transitions happen locally.
* [ ] Natural transition is published once.
* [ ] Crossfade remains local.
* [ ] Existing CrossfadeExoPlayerAdapter is reused.
* [ ] Local playback window is separate from Jam queue.
* [ ] Queue updates use minimal diffs.
* [ ] Current track is not unnecessarily reloaded.
* [ ] Pause preserves current position.
* [ ] Play resumes from authoritative position.
* [ ] Reconnect restores authoritative state.
* [ ] Disconnected members are removed from active membership.
* [ ] Reconnect does not create duplicates.
* [ ] Host transfer works.
* [ ] Server validates permissions.
* [ ] Stale WebSocket states are ignored.
* [ ] Repeated server states are idempotent.
* [ ] Rapid queue changes do not crash the adapter.
* [ ] `IndexOutOfBoundsException` cannot occur from stale playlist indices.
* [ ] No competing Jam auto-advance paths remain.
* [ ] No "server state → player.next()" feedback loop remains.
* [ ] Single Next acceptance test passes.
* [ ] Member Next acceptance test passes.
* [ ] Natural transition acceptance test passes.
* [ ] Reconnect acceptance test passes.
* [ ] Stress tests pass.

---

# 119. Most Important Rule

If debugging reveals:

```text
A → C
```

after one Next, do NOT patch it by adding:

```text
delay()
play()
seek()
extra condition
extra removal
```

Instead trace:

```text
WHO advanced A → B?
WHO advanced B → C?
```

There must be exactly one owner of each logical transition.

Likewise, if the result is:

```text
Current=B
Queue=B C D
```

do not simply remove B from the queue on the client.

Find out why the authoritative server state produced:

```text
Current=B
Queue=B C D
```

and fix the atomic queue/current-track mutation at its source.

The architecture should make these states structurally difficult or impossible to produce rather than hiding them with UI-side patches.
