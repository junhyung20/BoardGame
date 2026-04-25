package com.example.boardgame.controller;

import com.example.boardgame.model.GameState;
import com.example.boardgame.model.MiniGameState;
import com.example.boardgame.model.Room;
import com.example.boardgame.service.MiniGameService;
import com.example.boardgame.service.RoomService;
import com.example.boardgame.service.ScoreService;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.storage.FirebaseListener;
import com.example.boardgame.storage.FirebaseRoomStorage;
import com.example.boardgame.util.Constants;

import java.util.Map;

public class MiniGameController {
    private final FirebaseGameStorage gameStorage;
    private final RoomService roomService;
    private final MiniGameService miniGameService;
    private final ScoreService scoreService;

    public MiniGameController() {
        this(new FirebaseRoomStorage(), new FirebaseGameStorage());
    }

    public MiniGameController(FirebaseRoomStorage roomStorage, FirebaseGameStorage gameStorage) {
        this.gameStorage = gameStorage;
        this.roomService = new RoomService(roomStorage);
        this.miniGameService = new MiniGameService(gameStorage);
        this.scoreService = new ScoreService();
    }

    public MiniGameState startMiniGame(String roomCode) {
        GameState gameState = requireGameState(roomCode);
        return miniGameService.startMiniGame(gameState);
    }

    public MiniGameState submitMiniGameScore(String roomCode, String playerId, int score) {
        MiniGameState miniGameState = requireMiniGameState(roomCode);
        return miniGameService.submitScore(miniGameState, playerId, score);
    }

    public boolean allPlayersSubmitted(String roomCode) {
        MiniGameState miniGameState = requireMiniGameState(roomCode);
        return miniGameService.allPlayersSubmitted(miniGameState, Constants.MAX_PLAYERS);
    }

    public Map<String, Integer> finishMiniGame(String roomCode) {
        Room room = requireRoom(roomCode);
        MiniGameState miniGameState = requireMiniGameState(roomCode);
        Map<String, Integer> scoreDeltasByPlayerId = miniGameService.finishMiniGame(miniGameState);
        scoreService.applyScoreDeltas(room, scoreDeltasByPlayerId);
        roomService.saveRoom(room);
        return scoreDeltasByPlayerId;
    }

    public void listenToMiniGameState(String roomCode, FirebaseListener<MiniGameState> listener) {
        gameStorage.listenToMiniGameState(roomCode, listener);
    }

    private Room requireRoom(String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }

    private GameState requireGameState(String roomCode) {
        GameState gameState = gameStorage.getGameState(roomCode);
        if (gameState == null) {
            throw new IllegalArgumentException("Game state not found");
        }
        return gameState;
    }

    private MiniGameState requireMiniGameState(String roomCode) {
        MiniGameState miniGameState = gameStorage.getMiniGameState(roomCode);
        if (miniGameState == null) {
            throw new IllegalArgumentException("Mini-game state not found");
        }
        return miniGameState;
    }
}
