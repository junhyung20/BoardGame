# WAN ngrok Guide

Use this for temporary WAN testing from phones or remote devices.

ngrok provides the public TLS endpoint. The Java socket server still runs plain `ws://` locally.

```text
Android wss://<ngrok-domain>/game
        |
        v
ngrok public HTTPS/WebSocket endpoint
        |
        v
local ws://localhost:8080/game
```

## 1. Start Server

For Firebase Auth testing:

```bash
FIREBASE_SERVICE_ACCOUNT=/absolute/path/to/service-account.json BOARDGAME_NETWORK=WAN ./gradlew :socket-server:run
```

For temporary game-flow testing without Firebase Admin:

```bash
BOARDGAME_NETWORK=WAN ./gradlew :socket-server:run
```

Rules:

- `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS` set: Firebase Admin auth.
- No credential path set: dev auth.
- `BOARDGAME_NETWORK=WAN`: bind local server to `127.0.0.1` for ngrok.
- `BOARDGAME_NETWORK` unset: bind local server to `0.0.0.0` for LAN.

The service-account JSON file can have any filename. The env var points to its path.

Expected server output includes:

```text
BoardGame socket server listening on ws://127.0.0.1:8080/game
BoardGame network=WAN auth=FIREBASE
For ngrok WAN testing: ngrok http 8080 then connect Android to wss://<ngrok-domain>/game
```

On connect, Android should log a `SERVER_HELLO` summary like:

```text
Server FIREBASE / WAN / protocol 1
```

## 2. Start ngrok

In another terminal:

```bash
ngrok http 8080
```

ngrok will print a public HTTPS URL like:

```text
https://abc123.ngrok-free.app
```

Use the same host with `wss://` in Android:

```text
wss://abc123.ngrok-free.app/game
```

Do not use:

```text
https://abc123.ngrok-free.app/game
```

The Android socket client needs `wss://`, not `https://`.

## 3. Android Setup

In the app `Server URL` input, replace the local URL:

```text
ws://10.0.2.2:8080/game
```

with:

```text
wss://<ngrok-domain>/game
```

Use the same ngrok URL on every test device.

The debug client also has quick preset buttons:

- `Emu`: `ws://10.0.2.2:8080/game`
- `LAN`: `ws://YOUR_LAN_IP:8080/game`
- `WAN`: `wss://sandworm-ferret-bath.ngrok-free.dev/game`

Current default WAN preset:

```text
wss://sandworm-ferret-bath.ngrok-free.dev/game
```

The Android default is still emulator/local. Tap `WAN` to use the ngrok address.

## 4. Smoke Test Checklist

Use two physical phones, or one phone plus one emulator.

1. Start the local socket server.
2. Start `ngrok http 8080`.
3. Open the Android app on client A.
4. Set `Server URL` to `wss://<ngrok-domain>/game`.
5. Tap `Connect`.
6. Confirm the server logs `event=socket_open`.
7. Tap `Create`.
8. Confirm client A receives room state and a room code.
9. Open client B.
10. Set the same `Server URL`.
11. Tap `Connect`.
12. Enter client A's room code.
13. Tap `Join`.
14. Confirm both clients see the same room player list.
15. Set both clients ready.
16. Host taps `Start`.
17. Current player rolls and applies tile effect.
18. If a password room is used, verify wrong password returns `REQUEST_ERROR` with `INVALID_ROOM_PASSWORD`.
19. Kill or disconnect one client and confirm the other client receives an updated room/game state.

For meaningful Firebase Auth testing, set `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS`.

## 5. What To Watch

Server logs should look like:

```text
event=socket_open remote=/127.0.0.1:...
event=command_failed type=JOIN_ROOM requestId=... roomCode=- playerId=- errorCode=ROOM_NOT_FOUND cause=IllegalArgumentException
event=socket_close remote=/127.0.0.1:... code=1000 remoteClosed=true reason=-
```

Android request errors should use stable `errorCode` values, not raw server exception details.

Useful expected errors:

```text
ROOM_NOT_FOUND
ROOM_FULL
INVALID_ROOM_PASSWORD
NOT_HOST
NOT_YOUR_TURN
INVALID_PHASE
UNAUTHENTICATED
MALFORMED_MESSAGE
```

## 6. Troubleshooting

If Android cannot connect:

- Confirm the URL starts with `wss://`.
- Confirm it ends with `/game`.
- Confirm ngrok is still running.
- Confirm the local server is still running.
- Restart ngrok and update the Android URL if using a free random domain.

If connect works but commands fail with `UNAUTHENTICATED`:

- Check Android Firebase sign-in.
- Check `app/google-services.json`.
- Check `FIREBASE_SERVICE_ACCOUNT` or `GOOGLE_APPLICATION_CREDENTIALS` on the server.
- For non-auth gameplay testing only, restart the server without a credential path env var.

If only local emulator works:

- Make sure the phone has internet access.
- Use the ngrok `wss://` URL, not `ws://10.0.2.2:8080/game`.
- Do not point a physical phone at `127.0.0.1`; that means the phone itself.

If ngrok shows connections but the app does not update:

- Check server logs for `event=malformed_message` or `event=command_failed`.
- Check Android logcat for WebSocket close/error messages.
- Confirm both clients are using the same current ngrok domain.

## Notes

- Free ngrok domains can change each time you restart ngrok.
- `wss://` is enough for Android TLS transport; no Android cleartext exception is needed for the ngrok URL.
- The local server does not need native TLS for this flow because ngrok terminates public TLS.
- Keep real Firebase Admin verification enabled for meaningful WAN testing.
- Running without a credential path uses dev auth and is only for local/demo testing.

## Still Not Solved

This is WAN smoke testing, not production hosting.

Still needed later:

- reconnect/resume
- inactive non-empty room cleanup
- stronger heartbeat timeout policy if Java-WebSocket connection timeout is not enough
- metrics
- basic rate limiting/origin controls
- a stable domain or real deployment target
