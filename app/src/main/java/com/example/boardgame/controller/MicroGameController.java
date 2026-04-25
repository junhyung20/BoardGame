package com.example.boardgame.controller;

import com.example.boardgame.model.GameState;
import com.example.boardgame.model.MicroGameState;
import com.example.boardgame.model.Room;
import com.example.boardgame.service.MicroGameService;
import com.example.boardgame.service.RoomService;
import com.example.boardgame.service.ScoreService;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.storage.FirebaseListener;
import com.example.boardgame.storage.FirebaseRoomStorage;
import com.example.boardgame.util.Constants;

import java.util.Map;

public class MicroGameController {
    private final FirebaseGameStorage gameStorage;
    private final RoomService roomService;
    private final MicroGameService microGameService;
    private final ScoreService scoreService;

    public MicroGameController() {
        this(new FirebaseRoomStorage(), new FirebaseGameStorage());
    }

    public MicroGameController(FirebaseRoomStorage roomStorage, FirebaseGameStorage gameStorage) {
        this.gameStorage = gameStorage;
        this.roomService = new RoomService(roomStorage);
        this.microGameService = new MicroGameService(gameStorage);
        this.scoreService = new ScoreService();
    }

    public MicroGameState startMicroGame(String roomCode) {
        GameState gameState = requireGameState(roomCode);
        return microGameService.startMicroGame(gameState);
    }

    public MicroGameState submitMicroGameScore(String roomCode, String playerId, int score) {
        MicroGameState microGameState = requireMicroGameState(roomCode);
        return microGameService.submitScore(microGameState, playerId, score);
    }

    public boolean allPlayersSubmitted(String roomCode) {
        MicroGameState microGameState = requireMicroGameState(roomCode);
        return microGameService.allPlayersSubmitted(microGameState, Constants.MAX_PLAYERS);
    }

    public Map<String, Integer> finishMicroGame(String roomCode) {
        Room room = requireRoom(roomCode);
        MicroGameState microGameState = requireMicroGameState(roomCode);
        Map<String, Integer> scoreDeltasByPlayerId = microGameService.finishMicroGame(microGameState);
        scoreService.applyScoreDeltas(room, scoreDeltasByPlayerId);
        roomService.saveRoom(room);
        return scoreDeltasByPlayerId;
    }

    public void listenToMicroGameState(String roomCode, FirebaseListener<MicroGameState> listener) {
        gameStorage.listenToMicroGameState(roomCode, listener);
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

    private MicroGameState requireMicroGameState(String roomCode) {
        MicroGameState microGameState = gameStorage.getMicroGameState(roomCode);
        if (microGameState == null) {
            throw new IllegalArgumentException("Micro game state not found");
        }
        return microGameState;
    }
}
