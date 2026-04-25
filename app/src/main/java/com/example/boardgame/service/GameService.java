package com.example.boardgame.service;

import com.example.boardgame.model.BoardTile;
import com.example.boardgame.model.GameState;
import com.example.boardgame.model.Player;
import com.example.boardgame.model.Room;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.storage.FirebaseRoomStorage;
import com.example.boardgame.util.Constants;
import com.example.boardgame.util.RandomUtil;

import java.util.ArrayList;
import java.util.List;

public class GameService {
    private final FirebaseGameStorage gameStorage;
    private final FirebaseRoomStorage roomStorage;
    private final TileService tileService;

    public GameService(FirebaseGameStorage gameStorage, FirebaseRoomStorage roomStorage, TileService tileService) {
        this.gameStorage = gameStorage;
        this.roomStorage = roomStorage;
        this.tileService = tileService;
    }

    public GameState startGame(Room room) {
        if (room == null || !room.canStart()) {
            throw new IllegalStateException("Four ready players are required to start");
        }

        GameState gameState = new GameState(room.getCode());
        List<String> turnOrder = new ArrayList<>();
        for (Player player : room.getPlayerList()) {
            turnOrder.add(player.getId());
        }
        gameState.setTurnOrder(turnOrder);

        room.setStatus(Room.Status.IN_GAME);
        roomStorage.saveRoom(room);
        gameStorage.saveGameState(gameState);
        return gameState;
    }

    public int rollDice(GameState gameState, Room room) {
        requireTurnPhase(gameState, GameState.TurnPhase.WAITING_FOR_ROLL);
        Player currentPlayer = requireCurrentPlayer(gameState, room);

        int diceRoll = RandomUtil.rollDice();
        currentPlayer.moveBy(diceRoll, Constants.BOARD_SIZE);
        gameState.setLastDiceRoll(diceRoll);
        gameState.setTurnPhase(GameState.TurnPhase.TILE_EFFECT);

        roomStorage.saveRoom(room);
        gameStorage.saveGameState(gameState);
        return diceRoll;
    }

    public BoardTile applyCurrentTileEffect(GameState gameState, Room room) {
        requireTurnPhase(gameState, GameState.TurnPhase.TILE_EFFECT);
        Player currentPlayer = requireCurrentPlayer(gameState, room);
        BoardTile tile = tileService.getTile(currentPlayer.getPosition());

        tileService.applyTileEffect(currentPlayer, tile);
        if (tile.getType() == BoardTile.Type.GAME) {
            gameState.setTurnPhase(GameState.TurnPhase.MICRO_GAME);
        } else {
            gameState.advanceTurn();
        }

        roomStorage.saveRoom(room);
        gameStorage.saveGameState(gameState);
        return tile;
    }

    public void startMiniGamePhase(GameState gameState) {
        gameState.setTurnPhase(GameState.TurnPhase.MINI_GAME);
        gameStorage.saveGameState(gameState);
    }

    public void startNextRound(GameState gameState) {
        gameState.advanceRound();
        gameStorage.saveGameState(gameState);
    }

    public void continueAfterMicroGame(GameState gameState) {
        requireTurnPhase(gameState, GameState.TurnPhase.MICRO_GAME);
        gameState.advanceTurn();
        gameStorage.saveGameState(gameState);
    }

    public GameState getGameState(String roomCode) {
        return gameStorage.getGameState(roomCode);
    }

    private Player requireCurrentPlayer(GameState gameState, Room room) {
        if (gameState == null || room == null) {
            throw new IllegalArgumentException("Game state and room are required");
        }

        Player player = room.getPlayers().get(gameState.getCurrentPlayerId());
        if (player == null) {
            throw new IllegalStateException("Current player not found");
        }
        return player;
    }

    private void requireTurnPhase(GameState gameState, GameState.TurnPhase expectedPhase) {
        if (gameState == null || gameState.getTurnPhase() != expectedPhase) {
            throw new IllegalStateException("Invalid turn phase");
        }
    }
}
