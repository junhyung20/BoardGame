# Code Dictionary

This document is not a full method-by-method walkthrough. Simple DTOs, getters, XML views, Gradle basics, and obvious button wiring get brief notes; deeper explanation is reserved for code with real coupling or rule behavior.

Use this as a map for the code that has real coupling: socket protocol shape, Firebase identity, room membership, command routing, and game phase transitions.

## System Shape

The project has three runtime boundaries:

- `app`: Android debug client. It owns UI, Firebase sign-in, token fetching, and the OkHttp WebSocket.
- `shared`: protocol code used by both Android and server. It owns message names, JSON wire format, and snapshot mapping.
- `socket-server`: JVM WebSocket server. It owns authoritative room/game state and verifies Firebase ID tokens with Firebase Admin.

The important rule is:

```text
Android sends intentions. Server decides state.
```

The client can ask to create a room, ready up, roll, or submit a score. It does not decide player IDs, room membership, dice results, rewards, turn order, or whether an action is legal.

## End-To-End Command Path

For most commands, the call chain is:

```text
MainActivity button
-> SocketRoomController
-> BoardGameSocketClient.send(SocketMessage)
-> WebSocket text frame
-> BoardGameSocketServer.onMessage(...)
-> GameSocketHandler.handle(...)
-> RoomService / BoardGameService / MiniGameService / MicroGameService
-> REQUEST_OK or REQUEST_ERROR
-> ROOM_UPDATED and maybe GAME_UPDATED broadcast
-> MainActivity.handleSocketMessage(...)
```

The most important dependency is `SocketMessage`: both client and server parse the same JSON text format from `shared`.

## Brief Reference

These files are simple enough that the important point is mostly where they fit.

### Build And App Setup

- `settings.gradle.kts`: includes the `app`, `shared`, and `socket-server` modules and configures dependency repositories.
- `build.gradle.kts`: makes Android and Google Services plugins available to modules.
- `gradle/libs.versions.toml`: central version catalog for Android, Firebase, Gson, OkHttp, Java-WebSocket, and test dependencies.
- `app/build.gradle.kts`: Android module config; applies Google Services only when `app/google-services.json` exists.
- `shared/build.gradle.kts`: plain Java protocol module; exposes Gson because JSON protocol types are part of the shared API.
- `socket-server/build.gradle.kts`: JVM application module; points Gradle's `run` task at `BoardGameSocketServer`.
- `app/src/main/AndroidManifest.xml`: declares `MainActivity`, internet permission, and cleartext traffic for local `ws://` testing.

### Shared Small Types

- `MessageTypes`: one place for command and broadcast string constants. Add new socket message names here first.
- `ConnectionState`: Android-facing socket state enum: disconnected, connecting, connected, closing.
- `SocketEventListener`: callback interface from socket client to UI/controller code.
- `PlayerSnapshot`: client-safe player view: id, nickname, score, position, ready, host.
- `RoomSnapshot`: client-safe room view: code, host ID, status, players.
- `GameSnapshot`: client-safe game view: room code, round, current player, dice, phase, turn order.

### Server Small Types

- `AuthException`: marks auth failures so `GameSocketHandler` can return `UNAUTHENTICATED` instead of a generic bad request.
- `AuthVerifier`: injectable auth boundary. Production uses Firebase Admin; tests can pass a fake verifier.
- `DevAuthVerifier`: local-only auth bypass used when no server credential path env var is set.
- `Player`: server-side player state. It includes Firebase UID, so only `toSnapshot()` output should be sent to clients.
- `MiniGameState`: stores current mini-game type, timer metadata, status, and score submissions.
- `MicroGameState`: stores tile-triggered micro-game metadata, trigger player, status, and score submissions.

### Android Debug UI

- `activity_main.xml`: manual test screen for socket connection, room commands, game commands, score submission, and state display.
- The UI is intentionally plain. It is a debugging surface, not final game UI architecture.

## Protocol Dictionary

### `SocketMessage`

Path:

```text
shared/src/main/java/com/example/boardgame/socket/protocol/SocketMessage.java
```

Purpose:

- One WebSocket frame payload.
- JSON envelope with three top-level fields: `type`, `requestId`, `fields`.
- `fields` is a `JsonObject`, so it can contain simple values or nested objects/arrays.

Example command:

```json
{
  "type": "CREATE_ROOM",
  "requestId": "uuid",
  "fields": {
    "nickname": "Player",
    "firebaseIdToken": "token"
  }
}
```

Example room broadcast:

```json
{
  "type": "ROOM_UPDATED",
  "requestId": "",
  "fields": {
    "room": {
      "code": "123456",
      "hostPlayerId": "player-id",
      "status": "WAITING",
      "players": []
    }
  }
}
```

Key details:

- `getOrDefault`, `getInt`, and `getBoolean` are convenience methods for simple command/response fields.
- `getObject` is used for nested payloads like `fields.room` and `fields.game`.
- `put(String, JsonElement)` exists so snapshots can be sent as JSON objects instead of encoded strings.
- `getFields` returns a defensive copy. Do not mutate it expecting the message to change.

Change risk:

- Any wire-format change must be compatible across Android and server at the same time.
- If you add protocol versioning later, this is the natural place to add a top-level `version`.

### `SnapshotMessageMapper`

Path:

```text
shared/src/main/java/com/example/boardgame/socket/protocol/SnapshotMessageMapper.java
```

Purpose:

- Converts server-side DTO snapshots into nested JSON socket messages.
- Converts incoming nested JSON messages back into typed `RoomSnapshot` and `GameSnapshot`.

Server usage:

```text
GameSocketHandler.publishRoom -> SnapshotMessageMapper.roomUpdated
GameSocketHandler.publishGame -> SnapshotMessageMapper.gameUpdated
```

Android usage:

```text
MainActivity.handleSocketMessage -> SnapshotMessageMapper.toRoomSnapshot
MainActivity.handleSocketMessage -> SnapshotMessageMapper.toGameSnapshot
```

Why this exists:

- Snapshot JSON is shared protocol, not Android-only mapping.
- Keeping it in `shared` prevents Android and server from silently disagreeing about field names.

Change risk:

- If you add fields to `RoomSnapshot`, `GameSnapshot`, or `PlayerSnapshot`, update both directions here.
- Keep missing-field defaults conservative; older clients may receive newer messages or vice versa during development.

## Android Client Dictionary

### `MainActivity`

Path:

```text
app/src/main/java/com/example/boardgame/MainActivity.java
```

Role:

- Temporary debug UI and manual test harness.
- Not production architecture.

Important behavior:

- Initializes Firebase after views are bound, not as a field initializer.
- Wraps room-entry commands with `withIdToken(...)`.
- Renders server snapshots, but does not validate game rules.

Auth flow:

```text
Create/join/matchmake button
-> withIdToken
-> FirebaseAuthTokenProvider.requireIdToken
-> SocketRoomController command with token
```

Error behavior:

- Missing `app/google-services.json` is shown in the debug log instead of crashing.
- Firebase backend setup errors are translated into more useful UI messages where possible.

Change risk:

- Do not create `FirebaseAuth.getInstance()` before Firebase initialization.
- Do not put Admin SDK credentials or service-account paths here. Android only uses `app/google-services.json`.

### `FirebaseAuthTokenProvider`

Path:

```text
app/src/main/java/com/example/boardgame/auth/FirebaseAuthTokenProvider.java
```

Role:

- Produces Firebase ID tokens for room-entry commands.

Important behavior:

- If no user is signed in, it calls anonymous sign-in.
- After sign-in, it requests an ID token with `getIdToken(false)`.
- The ID token is sent to the socket server; the Android client never sends a trusted UID directly.

Dependencies:

- Requires `app/google-services.json`.
- Requires Anonymous auth enabled in Firebase Console.

Change risk:

- If you replace anonymous auth with Google/email/etc., keep the output contract the same: return a Firebase ID token.

### `BoardGameSocketClient`

Path:

```text
app/src/main/java/com/example/boardgame/socket/BoardGameSocketClient.java
```

Role:

- Low-level OkHttp WebSocket wrapper.

Important behavior:

- `connect` ignores duplicate connect attempts while already connecting/connected.
- `send` refuses to send unless state is `CONNECTED`.
- A background heartbeat sends `APP_PING` every 20 seconds.
- Incoming text is parsed immediately with `SocketMessage.parse`.

Change risk:

- Socket callbacks are not guaranteed to run on the Android UI thread. `MainActivity` uses `runOnUiThread` before touching views.
- If parse errors should not kill callbacks, add a try/catch around `SocketMessage.parse` here and report `onError`.

### `SocketRoomController`

Path:

```text
app/src/main/java/com/example/boardgame/controller/socket/SocketRoomController.java
```

Role:

- Thin Android-side command facade.
- Converts UI intent into `SocketMessage` commands.

Important behavior:

- Room-entry commands include `nickname` and `firebaseIdToken`.
- Other commands rely on the server-bound `ClientSession`; they do not resend player ID or UID.

Change risk:

- Do not add client-selected player IDs here. The server assigns and remembers player identity after token verification.

## Server Entry And Session Dictionary

### `BoardGameSocketServer`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/BoardGameSocketServer.java
```

Role:

- WebSocket server lifecycle.
- Owns the map from raw WebSocket connections to `ClientSession`.

Important behavior:

- Creates a `ClientSession` in `onOpen`.
- Parses every text message as `SocketMessage` in `onMessage`.
- Handles `APP_PING` locally by returning `APP_PONG`.
- For all other message types, delegates to `GameSocketHandler`.
- On close, removes the session and tells game logic to disconnect the player.

Change risk:

- This class should stay transport-focused. Game rules belong in services, not here.

### `ClientSession`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/ClientSession.java
```

Role:

- Server-side identity for one WebSocket connection.

State it owns:

- `roomCode`: room bound after create/join/matchmake.
- `playerId`: server-generated player ID.
- `firebaseUid`: verified Firebase UID.

Important behavior:

- `bindPlayer` is called only after Firebase token verification and successful room membership.
- `sendError` preserves the request ID so the client can connect errors to commands.

Change risk:

- Session state is connection-bound. Reconnect/resume will require a new design because a fresh WebSocket creates a fresh `ClientSession`.

## Authentication Dictionary

### `FirebaseAdminAuthVerifier`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/FirebaseAdminAuthVerifier.java
```

Role:

- Converts an Android Firebase ID token into a trusted Firebase UID.

Credential lookup:

```text
FIREBASE_SERVICE_ACCOUNT
or
GOOGLE_APPLICATION_CREDENTIALS
```

Important behavior:

- Empty tokens are rejected.
- `verifyIdToken(token, true)` checks revocation.
- The verified UID is returned to server room logic.

Change risk:

- This file must never load Android `google-services.json`.
- Admin SDK JSON is a server secret and must not be committed.

### `AuthVerifier`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/AuthVerifier.java
```

Role:

- Small interface so auth verification can be injected in tests.

Change risk:

- Keep the interface returning a trusted UID, not a client-provided identity.

### `DevAuthVerifier`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/DevAuthVerifier.java
```

Role:

- Local development bypass for teams that need to test socket/game flow without sharing Firebase Admin credentials.

Activation:

```bash
./gradlew :socket-server:run
```

If `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS` is set, the server uses Firebase Admin auth instead.

Important behavior:

- If the client sends token text, that text is used as the dev UID.
- If the token is empty, the verifier creates a local sequential UID such as `dev-1`.

Change risk:

- This is not authentication. Never use it for production or security testing.
- Because empty tokens become generated local UIDs, reconnecting creates a different dev identity.

## Command Coordinator Dictionary

### `GameSocketHandler`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/GameSocketHandler.java
```

Role:

- Central command coordinator.
- Converts validated socket commands into service calls.
- Sends direct responses and broadcasts snapshots.

Main pattern:

```text
handle
-> handleCommand
-> service method
-> sendOk
-> publishRoom
-> publishGame if needed
```

Authentication boundary:

- `CREATE_ROOM`, `JOIN_ROOM`, and `MATCHMAKE` call `verify(...)`.
- After that, `ClientSession.bindPlayer(...)` stores room/player/UID.
- Later commands trust the bound session, not fields from the client.

Error mapping:

- `AuthException` becomes `REQUEST_ERROR errorCode=UNAUTHENTICATED`.
- `IllegalArgumentException` and `IllegalStateException` become `BAD_REQUEST`.

Broadcast behavior:

- Most successful commands publish `ROOM_UPDATED`.
- Gameplay commands use `Result.roomAndGame(...)`, which also publishes `GAME_UPDATED`.

Change risk:

- If you add a new command, decide whether it requires auth token verification or an already-bound room session.
- Do not let command handlers directly mutate random model state unless the relevant service owns that rule.
- The whole `handle` method is synchronized, which simplifies in-memory state safety. If this becomes high-traffic, concurrency needs a more deliberate design.

## Room Membership Dictionary

### `RoomService`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/service/RoomService.java
```

Role:

- Owns room creation, joining, matchmaking, ready state, and disconnect cleanup.

Important constraints:

- `MIN_PLAYERS = 1` for current testing.
- `MAX_PLAYERS = 4`.
- A Firebase UID can only be seated in one active room.
- Join/matchmake only target rooms in `WAITING` or `READY`.

Room code behavior:

- Room codes are random six-digit strings.
- The generator retries until unused.

Disconnect behavior:

- Removes the player from the room.
- Deletes the room if empty.
- Otherwise recalculates ready status and host ownership through `Room.removePlayer`.

Change risk:

- Reconnect/resume will conflict with immediate removal on disconnect. Add a grace period before changing this behavior.
- `requireUidAvailable` scans all rooms. Fine for LAN testing; replace with an index if persistence/scale matters.

### `Room`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/model/Room.java
```

Role:

- In-memory aggregate for lobby, players, and active game state.

Important behavior:

- First player becomes host.
- When host leaves, another player becomes host.
- `refreshReadyStatus` maps player readiness to room status.
- `toSnapshot` strips server-only details and returns client-safe state.

Change risk:

- Keep Firebase UID and other server-only secrets out of snapshots.

## Main Game Dictionary

### `BoardGameService`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/service/BoardGameService.java
```

Role:

- Owns authoritative board-game flow.

Current starter rules:

- Board size is 16.
- Final round is 3.
- Dice roll is server random, 1-6.
- Tile type is derived from position:
  - `0`: `START`
  - divisible by `8`: `GAME`
  - divisible by `6`: `CARD`
  - divisible by `5`: `QUESTION_MARK`
  - otherwise `NORMAL`

Phase flow:

```text
WAITING_FOR_ROLL
-> rollDice
-> TILE_EFFECT
-> applyTileEffect
-> WAITING_FOR_ROLL, ROUND_END, MICRO_GAME, or FINISHED
```

Special tile behavior:

- `QUESTION_MARK`: random score change, then advance turn.
- `CARD`: adds starter `DOUBLE_DICE`, then advance turn.
- `GAME`: returns `TILE_GAME`; `GameSocketHandler` starts a micro game.
- `NORMAL` / `START`: advance turn.

Validation:

- Player must exist in the room.
- Player must be current turn player for roll/tile effect.
- Game phase must match the command.

Change risk:

- Do not put final board rules into `GameSocketHandler`. This service is the rule boundary.
- If cards become real mechanics, add explicit card state/rules instead of leaving string inventory behavior scattered.

### `GameState`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/model/GameState.java
```

Role:

- Tracks turn order, current round, current player index, phase, and last dice roll.

Important behavior:

- `advanceTurn` moves to the next player.
- End of turn order moves to `ROUND_END`.
- `advanceRound` starts the next round or finishes the game.

Change risk:

- Keep phase transitions explicit. Most command validation depends on `turnPhase`.

## Mini/Micro Game Dictionary

### `MiniGameService`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/service/MiniGameService.java
```

Role:

- End-of-round mini game flow.

Current flow:

```text
ROUND_END
-> startMiniGame
-> MINI_GAME
-> submitMiniGameScore from players
-> finishMiniGame
-> rewards
-> advanceRound
```

Rewards:

```text
rank 1: 30
rank 2: 20
rank 3: 10
rank 4: 5
```

Change risk:

- No timer enforcement exists yet. `durationMillis` is stored but not used to auto-finish.
- Missing submissions are simply absent from the score map.

### `MicroGameService`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/service/MicroGameService.java
```

Role:

- Tile-triggered quick game flow.

Current flow:

```text
TILE_EFFECT on GAME tile
-> startMicroGame
-> MICRO_GAME
-> submitMicroGameScore from players
-> finishMicroGame
-> rewards
-> advanceTurn
```

Rewards:

```text
rank 1: 8
rank 2: 5
rank 3: 3
rank 4: 1
```

Change risk:

- Like mini games, timers are not enforced yet.
- The trigger player is stored, but current scoring allows any room player to submit.

### `ScoreService`

Path:

```text
socket-server/src/main/java/com/example/boardgame/server/service/ScoreService.java
```

Role:

- Shared score ranking and reward application.

Important behavior:

- Higher submitted score ranks first.
- Reward array index maps to rank.
- Rewards are applied to `Player.score`.

Change risk:

- Tie behavior is currently whatever the sort/order produces. Define explicit tie handling before final gameplay.

## Tests Worth Reading

These tests are more useful than most file summaries:

- `FirebaseAdminAuthVerifierTest`: verifies token success/failure behavior without real Firebase.
- `RoomServiceTest`: verifies duplicate UID prevention and room membership rules.
- `SocketMessageTest`: verifies JSON wire serialization and simple field access.
- `SnapshotMessageMapperTest`: verifies nested JSON room/game snapshot round trips.

## Common Change Recipes

### Add A New Client Command

1. Add the string to `MessageTypes`.
2. Add a method to `SocketRoomController`.
3. Add a case in `GameSocketHandler.handleCommand`.
4. Put rule logic in a service, not in the socket server.
5. Decide whether the result is `roomOnly` or `roomAndGame`.
6. Add tests at the service or protocol boundary.

### Add A New Room Snapshot Field

1. Add it to `RoomSnapshot` or `PlayerSnapshot`.
2. Populate it in model `toSnapshot`.
3. Add JSON encode/decode in `SnapshotMessageMapper`.
4. Render it in `MainActivity` only if useful for debug.
5. Add/update `SnapshotMessageMapperTest`.

### Add A New Game Phase

1. Add the phase constant to `GameState`.
2. Decide which service transitions into and out of it.
3. Add `requirePhase` checks for commands that only make sense in that phase.
4. Make sure `publishGame` runs after commands that change the phase.

### Add Real Reconnect

Current disconnect removes a player immediately. Proper reconnect needs:

- A server-issued resume token.
- A grace period before `RoomService.disconnect` removes the player.
- A way to bind a new `ClientSession` to the previous room/player.
- Rules for what happens if the Firebase UID tries to join elsewhere during the grace period.
