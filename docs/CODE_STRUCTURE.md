# Code Structure

This project is the server/domain side of a 4-player online board game. The code is intentionally split into small layers so three members can work without stepping on each other.

## Naming Rule

Use these names consistently:

- **Mini game**: the 30-60 second game played at the end of each round.
- **Micro game**: the short 10 second game triggered by landing on a `GAME` tile.

Do not call tile-triggered games "mini games" in code, docs, UI text, or Firebase paths.

## Package Layout

```text
app/src/main/java/com/example/boardgame/
├── controller/
│   ├── RoomController.java
│   ├── GameController.java
│   ├── MiniGameController.java
│   └── MicroGameController.java
├── service/
│   ├── RoomService.java
│   ├── GameService.java
│   ├── TileService.java
│   ├── ScoreService.java
│   ├── MiniGameService.java
│   └── MicroGameService.java
├── storage/
│   ├── FirebaseRoomStorage.java
│   ├── FirebaseGameStorage.java
│   └── FirebaseListener.java
├── model/
│   ├── Player.java
│   ├── Room.java
│   ├── GameState.java
│   ├── BoardTile.java
│   ├── MiniGameState.java
│   └── MicroGameState.java
└── util/
    ├── Constants.java
    ├── FirebasePaths.java
    ├── RandomUtil.java
    └── RoomCodeGenerator.java
```

## Layer Responsibilities

`controller`

- Public entry point for UI, networking, or Firebase callbacks.
- Should be thin.
- Validates that required state exists.
- Calls services and returns models.

`service`

- Owns game rules.
- Rolls dice, moves players, applies tile effects, ranks scores, and advances phases.
- Should not import Firebase SDK classes directly.

`storage`

- Owns Firebase Realtime Database access.
- Converts Firebase listener updates into local cached model state.
- Firebase imports should stay here.

`model`

- Plain state objects.
- Must stay simple enough for Firebase serialization.
- Needs empty constructors and getters/setters.

`util`

- Shared constants, Firebase path names, random helpers, and room code generation.

## Core Models

`Player`

- `id`
- `nickname`
- `score`
- `position`
- `ready`
- `host`
- `itemCards`

`Room`

- `code`
- `hostPlayerId`
- `status`
- `players`

Room statuses:

```text
WAITING
READY
IN_GAME
FINISHED
```

`GameState`

- `roomCode`
- `currentRound`
- `finalRound`
- `currentPlayerIndex`
- `lastDiceRoll`
- `turnPhase`
- `turnOrder`

Turn phases:

```text
WAITING_FOR_ROLL
MOVING
TILE_EFFECT
MINI_GAME
MICRO_GAME
ROUND_END
FINISHED
```

`BoardTile`

Tile types:

```text
START
NORMAL
QUESTION_MARK
CARD
GAME
```

Tile behavior:

- `QUESTION_MARK`: server picks a random event.
- `CARD`: server gives the player an item card.
- `GAME`: starts a micro game.

`MiniGameState`

- End-of-round game only.
- Duration target: 30-60 seconds.
- Current default: 45 seconds.

Mini game types:

```text
COLOR_GUESSING
PASSWORD_GUESSING
PHONE_TILT_MAZE
```

`MicroGameState`

- Tile-triggered game only.
- Duration target: about 10 seconds.
- Current default: 10 seconds.

## Firebase Structure

Firebase Realtime Database paths:

```text
rooms/{roomCode}
games/{roomCode}
miniGames/{roomCode}
microGames/{roomCode}
```

Path names are defined in:

```text
util/FirebasePaths.java
```

The real Firebase config file must be downloaded from Firebase Console and placed here:

```text
app/google-services.json
```

Use package name:

```text
com.example.boardgame
```

An example format is kept at:

```text
app/google-services.json.example
```

The Google Services Gradle plugin is applied only when `app/google-services.json` exists. This lets Android Studio resolve Firebase SDK classes before the real Firebase project file is added.

## Public Controller Surface

Room:

```java
Room createRoom(String nickname)
Room joinRoom(String roomCode, String nickname)
Room setReady(String roomCode, String playerId, boolean ready)
boolean canStartGame(String roomCode)
void listenToRoom(String roomCode, FirebaseListener<Room> listener)
```

Game:

```java
GameState startGame(String roomCode)
int rollDice(String roomCode)
BoardTile applyTileEffect(String roomCode)
void startMiniGamePhase(String roomCode)
void startNextRound(String roomCode)
void continueAfterMicroGame(String roomCode)
void listenToGameState(String roomCode, FirebaseListener<GameState> listener)
```

Mini game:

```java
MiniGameState startMiniGame(String roomCode)
MiniGameState submitMiniGameScore(String roomCode, String playerId, int score)
boolean allPlayersSubmitted(String roomCode)
Map<String, Integer> finishMiniGame(String roomCode)
void listenToMiniGameState(String roomCode, FirebaseListener<MiniGameState> listener)
```

Micro game:

```java
MicroGameState startMicroGame(String roomCode)
MicroGameState submitMicroGameScore(String roomCode, String playerId, int score)
boolean allPlayersSubmitted(String roomCode)
Map<String, Integer> finishMicroGame(String roomCode)
void listenToMicroGameState(String roomCode, FirebaseListener<MicroGameState> listener)
```
