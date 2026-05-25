# Socket Architecture

Target shape:

```text
Firebase Auth        Android client                  Socket server
     |                    |                               |
     | sign in            |                               |
     |------------------->|                               |
     | ID token           |                               |
     |<-------------------|                               |
                          | ws://LAN_IP:8080/game         |
                          |------------------------------>|
                          | commands                      |
                          |<----------------------------->|
                          | room/game broadcasts          |
```

Firebase Auth identifies the user. The socket server owns live lobby, room, and gameplay state. The server verifies Firebase ID tokens with the Firebase Admin SDK and stores the verified UID; it does not trust UID-like client fields.

## Current Protocol

Messages are JSON text frames. The envelope keeps command values and snapshot payloads under `fields`:

```json
{
  "type": "CREATE_ROOM",
  "requestId": "request-id",
  "fields": {
    "nickname": "Player",
    "firebaseIdToken": "firebase-token"
  }
}
```

Room and game snapshots use nested JSON objects. For example, `ROOM_UPDATED` carries `fields.room.players` as an array of player objects, and `GAME_UPDATED` carries `fields.game.turnOrder` as an array of player IDs.

Transport libraries:

- Android client uses OkHttp WebSocket.
- JVM server uses Java-WebSocket.

Every request contains:

```text
type
requestId
```

Client commands:

```text
CREATE_ROOM   nickname firebaseIdToken
JOIN_ROOM     roomCode nickname firebaseIdToken
MATCHMAKE     nickname firebaseIdToken
SET_READY     ready
START_GAME
ROLL_DICE
APPLY_TILE_EFFECT
START_MINI_GAME          miniGameType
SUBMIT_MINI_GAME_SCORE   score
FINISH_MINI_GAME
SUBMIT_MICRO_GAME_SCORE  score
FINISH_MICRO_GAME
APP_PING
```

Server messages:

```text
REQUEST_OK      requestId roomCode playerId status
REQUEST_ERROR   requestId errorCode details
ROOM_UPDATED    room
GAME_UPDATED    game
APP_PONG
```

`REQUEST_OK` and `REQUEST_ERROR` answer a specific command. `ROOM_UPDATED` and `GAME_UPDATED` are broadcasts and can arrive at any time.

Missing, invalid, expired, or revoked Firebase ID tokens are rejected with `REQUEST_ERROR errorCode=UNAUTHENTICATED`. A verified UID may only be seated in one active room at a time.

## Current Server Behavior

Implemented:

- WebSocket upgrade.
- One `ClientSession` per connection.
- Create room.
- Join room by code.
- Matchmake into an open room or create a new room.
- Ready/unready.
- Start game when at least one player is ready.
- Roll dice for the current player.
- Move player position.
- Apply starter tile effects.
- Start, submit, and finish mini game / micro game flows through dedicated gameplay services.
- Advance turn.
- Broadcast room and game snapshots.
- Remove players on disconnect.
- Reassign host when the host disconnects.
- Verify Firebase ID tokens for room entry commands.
- Reject duplicate active room seats for the same verified UID.

Missing:

- Reconnect/resume.
- Room timeout cleanup.
- Stable protocol errors.
- Protocol versioning and compatibility.
- Dedicated mini-game and micro-game broadcasts.
- Final tile effects, mini games, micro games, scoring, and winner calculation.
- Persistence and observability decisions.
- TLS/WAN deployment.

See `docs/IMPLEMENTATION_PLAN.md` for the detailed implementation checklist.

## LAN Testing

Start the server:

```bash
./gradlew :socket-server:run
```

Connect Android to:

```text
ws://YOUR_LAN_IP:8080/game
```

For emulator-to-host testing:

```text
ws://10.0.2.2:8080/game
```

## Later Firebase Lobby Option

If Firebase is later used for a lightweight room discovery lobby, keep it separate from gameplay. It should only advertise room/server connection information, such as:

```text
roomCode
serverUrl
playerCount
status
createdAt
```

Gameplay state should still remain on the socket server.

## WAN Requirements

Temporary WAN smoke testing:

- Run the local server behind ngrok.
- Connect Android with `wss://<ngrok-domain>/game`.
- See `docs/WAN_NGROK_GUIDE.md`.

Before real production WAN multiplayer:

- Manually verify Firebase Admin credentials and signed-in clients.
- Use `wss://` through a stable domain or deployment target.
- Add reconnect and session resume.
- Add heartbeat timeout detection.
- Add room cleanup after inactivity.
- Add stable protocol errors.
- Add logs and metrics.
- Decide whether match history needs persistence.
