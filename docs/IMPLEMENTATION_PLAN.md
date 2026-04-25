# Implementation Plan

This plan keeps the first working version small: room sync, board turns, mini games, micro games, and scoring.

## Phase 1: Firebase Project Setup

1. Create a Firebase project.
2. Add an Android app with package name:

```text
com.example.boardgame
```

3. Download `google-services.json`.
4. Place it at:

```text
app/google-services.json
```

5. Sync Gradle in Android Studio. The Google Services plugin is applied automatically once the file exists.
6. Create a Realtime Database.
7. For early development only, use the open rules in:

```text
firebase-database-rules.dev.json
```

Production rules must be stricter after Firebase Auth or another authorization strategy is added.

## Phase 2: Room Flow

Goal:

```text
nickname -> create/join room -> four players ready
```

Implementation tasks:

- Validate nickname.
- Create room with generated room code.
- Join room by code.
- Reject joining full, in-game, or finished rooms.
- Ready/unready players.
- Mark room `READY` only when exactly four players are ready.
- Write room changes to `rooms/{roomCode}`.
- Listen for room changes.

## Phase 3: Core Game Flow

Goal:

```text
start game -> player turns -> tile effect -> round end
```

Implementation tasks:

- Start game only from a `READY` room.
- Build turn order from room players.
- Roll dice on the server side.
- Move the current player.
- Apply tile effect.
- Advance to next player.
- Set `ROUND_END` after player 4 finishes.
- Set `FINISHED` after the final round.
- Write game state to `games/{roomCode}`.

Important rule:

The client should never decide dice value, random event result, card draw, score reward, or winner.

## Phase 4: Tile Effects

Tile behavior:

```text
START
  No effect.

NORMAL
  No effect.

QUESTION_MARK
  Server picks a random event.

CARD
  Server gives one item card.

GAME
  Starts a micro game.
```

Implementation tasks:

- Finalize random event list.
- Finalize card list and effects.
- Decide whether card effects happen immediately or are held by the player.
- When a player lands on `GAME`, set turn phase to `MICRO_GAME`.

## Phase 5: Mini Games

Mini games happen at the end of each round.

Mini game types:

```text
COLOR_GUESSING
PASSWORD_GUESSING
PHONE_TILT_MAZE
```

Timing:

```text
30-60 seconds target
45 seconds default
```

Implementation tasks:

- Start mini game from `ROUND_END`.
- Choose mini game type.
- Save state to `miniGames/{roomCode}`.
- Accept one raw score per player.
- Finish after all players submit or timeout.
- Convert raw scores into board-game score rewards.
- Start the next round.

Default mini game rewards:

```text
1st: +30
2nd: +20
3rd: +10
4th: +5
```

## Phase 6: Micro Games

Micro games happen only when a player lands on a `GAME` tile.

Timing:

```text
10 seconds target
10 seconds default
```

Implementation tasks:

- Start micro game from `MICRO_GAME` phase.
- Save state to `microGames/{roomCode}`.
- Accept raw scores.
- Finish after all required players submit or timeout.
- Apply smaller score rewards.
- Continue the interrupted turn flow with `continueAfterMicroGame`.

Default micro game rewards:

```text
1st: +8
2nd: +5
3rd: +3
4th: +1
```

Decision needed:

```text
Does a micro game involve only the player who landed on the tile,
or all four players?
```

The current skeleton assumes all four can submit, because it mirrors mini-game scoring.

## Phase 7: Validation and Edge Cases

Add validation for:

- Non-current player trying to roll.
- Player rolling twice.
- Applying tile effect in the wrong phase.
- Starting mini game outside `ROUND_END`.
- Starting micro game outside `MICRO_GAME`.
- Duplicate score submission.
- Player disconnect during ready, turn, mini game, or micro game.
- Timeout handling.
- Tie handling.

## Phase 8: Tests

Recommended service tests:

- Room can be created.
- Room rejects fifth player.
- Game cannot start before four ready players.
- Dice roll moves current player.
- Question-mark tile changes score or position.
- Card tile adds an item card.
- Game tile sets phase to `MICRO_GAME`.
- Mini game uses 45 second duration.
- Micro game uses 10 second duration.
- Mini game rewards are larger than micro game rewards.
- Final winner is highest score.
