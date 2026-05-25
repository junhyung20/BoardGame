# BoardGame

Android board-game project rebuilt around Firebase Auth plus a custom WebSocket server.

Firebase is no longer used for Realtime Database gameplay state. The old RTDB controllers, services, storage classes, models, utilities, and rules file have been removed. Live room state and gameplay state now belong to the socket server.

## Current Status

This is still starter structure code, not a finished multiplayer game.

Done:

- `app`: Android debug UI with Firebase Auth helper and OkHttp WebSocket client.
- `shared`: JSON socket protocol DTOs shared by app/server.
- `socket-server`: JVM server using Java-WebSocket and Firebase Admin auth for room creation, joining, matchmaking, ready state, start game, dice rolls, starter tile effects, and starter mini/micro score flows.
- Balanced server layout: `GameSocketHandler`, `RoomService`, `BoardGameService`, `MiniGameService`, `MicroGameService`, `ScoreService`, and minimal models.
- Removed old Firebase Realtime Database gameplay code and dependencies.
- Added request IDs, basic errors, room/game broadcasts, and heartbeat messages.
- Prevented one verified Firebase UID from joining multiple active rooms at the same time.

Still needed:

- Manual Firebase service-account and device verification.
- Reconnect/resume support.
- Polished board-game rules on the server.
- Final tile effects, mini games, micro games, scoring, and winner calculation.
- Production hosting beyond temporary ngrok WAN testing.

## Modules

```text
app
```

Android client. It owns Firebase sign-in, token fetching, socket connection, and UI integration.

```text
shared
```

Plain Java shared code. It has no Android, Firebase, or server-only dependencies.

```text
socket-server
```

Plain JVM LAN server. It owns authoritative live room/game state.

## LAN Socket Test

Start the server:

```bash
./gradlew :socket-server:run
```

If no server credential path is set, the server uses dev auth automatically. For Firebase Admin auth:

```bash
FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json ./gradlew :socket-server:run
```

Connect Android to:

```text
ws://YOUR_LAN_IP:8080/game
```

Android emulator host shortcut:

```text
ws://10.0.2.2:8080/game
```

## WAN Socket Test With ngrok

Run the local server:

```bash
FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json BOARDGAME_NETWORK=WAN ./gradlew :socket-server:run
```

If `FIREBASE_SERVICE_ACCOUNT` is omitted, the server uses dev auth.

In another terminal:

```bash
ngrok http 8080
```

If ngrok prints `https://abc123.ngrok-free.app`, connect Android to:

```text
wss://abc123.ngrok-free.app/game
```

The debug client also has `Emu`, `LAN`, and `WAN` server preset buttons.
After connecting, the log should show the server auth mode, network mode, and protocol version from `SERVER_HELLO`.

See [WAN ngrok Guide](docs/WAN_NGROK_GUIDE.md).

## Firebase Config

Firebase is currently used for Auth only.

Put the real config here:

```text
app/google-services.json
```

The socket server verifies client ID tokens with Firebase Admin. Set one of these before running the server:

```bash
export FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json
```

or configure `GOOGLE_APPLICATION_CREDENTIALS`.

If neither credential path is set, the socket server uses `DevAuthVerifier` so local gameplay can run without Firebase Admin credentials.

Dev auth bypasses Firebase token verification and is not safe for production.

## Docs

- [Code Structure](docs/CODE_STRUCTURE.md)
- [Code Walkthrough](docs/CODE_WALKTHROUGH.md)
- [Socket Architecture](docs/SOCKET_ARCHITECTURE.md)
- [Implementation Plan](docs/IMPLEMENTATION_PLAN.md)
- [WAN ngrok Guide](docs/WAN_NGROK_GUIDE.md)
- [Team Assignment](docs/TEAM_ASSIGNMENT.md)
