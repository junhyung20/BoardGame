# Team Assignment

The project has three members. Each member should own a clear area and avoid editing another member's files without coordination.

## Member 1: Room and Firebase Room Sync

Primary files:

```text
controller/RoomController.java
service/RoomService.java
storage/FirebaseRoomStorage.java
storage/FirebaseListener.java
model/Room.java
model/Player.java
util/RoomCodeGenerator.java
```

Responsibilities:

- Nickname validation.
- Room creation.
- Room code generation.
- Join room.
- Leave room.
- Host reassignment.
- Ready/unready state.
- Room status transitions.
- Firebase room listener.

Owned Firebase path:

```text
rooms/{roomCode}
```

Definition of done:

- Four players can join one room.
- All players can ready up.
- Room becomes `READY` only when all four players are ready.
- Room state syncs through Firebase listener.

## Member 2: Board, Turn, Tile, and Game State

Primary files:

```text
controller/GameController.java
service/GameService.java
service/TileService.java
storage/FirebaseGameStorage.java
model/GameState.java
model/BoardTile.java
util/Constants.java
util/RandomUtil.java
```

Responsibilities:

- Start game.
- Turn order.
- Dice rolling.
- Player movement.
- Tile effects.
- Round transitions.
- Final round handling.
- Game state listener.

Owned Firebase path:

```text
games/{roomCode}
```

Important interfaces with Member 3:

- End of round sets phase to `ROUND_END`.
- Starting the end-of-round mini game sets phase to `MINI_GAME`.
- Landing on a `GAME` tile sets phase to `MICRO_GAME`.
- After the micro game finishes, `continueAfterMicroGame` resumes normal turn order.

Definition of done:

- Player turns advance in order.
- Player 4 ending a turn moves the game to `ROUND_END`.
- `GAME` tile starts micro-game flow, not mini-game flow.
- Final round ends the game.

## Member 3: Mini Game, Micro Game, Score, and Ranking

Primary files:

```text
controller/MiniGameController.java
controller/MicroGameController.java
service/MiniGameService.java
service/MicroGameService.java
service/ScoreService.java
storage/FirebaseGameStorage.java
model/MiniGameState.java
model/MicroGameState.java
```

Responsibilities:

- End-of-round mini-game state.
- Tile-triggered micro-game state.
- Raw score submission.
- Score ranking.
- Score rewards.
- Leaderboard.
- Winner.

Owned Firebase paths:

```text
miniGames/{roomCode}
microGames/{roomCode}
```

Mini game scope:

- Happens after every round.
- Uses `MiniGameState`.
- Uses `MiniGameController`.
- Takes about 30-60 seconds.
- Current default duration is 45 seconds.
- Types are `COLOR_GUESSING`, `PASSWORD_GUESSING`, and `PHONE_TILT_MAZE`.

Micro game scope:

- Happens only from a `GAME` tile.
- Uses `MicroGameState`.
- Uses `MicroGameController`.
- Takes about 10 seconds.
- Rewards should be smaller than mini-game rewards.

Definition of done:

- Mini game can start, receive scores, finish, and update room scores.
- Micro game can start, receive scores, finish, and update room scores.
- Mini-game and micro-game code paths are not mixed.
- Leaderboard returns players sorted by total board-game score.

## Shared Rules

- Do not put Firebase SDK calls in services.
- Do not trust client-provided dice rolls, random outcomes, card draws, or final score rewards.
- Keep models Firebase-serializable with empty constructors and getters/setters.
- Use `MiniGame*` names only for end-of-round games.
- Use `MicroGame*` names only for tile-triggered games.
- Before changing a file owned by another member, agree on the change first.

