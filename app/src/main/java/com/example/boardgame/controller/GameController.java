package com.example.boardgame.controller;

import com.example.boardgame.model.BoardTile;
import com.example.boardgame.model.GameState;
import com.example.boardgame.model.Room;
import com.example.boardgame.service.GameService;
import com.example.boardgame.service.RoomService;
import com.example.boardgame.service.TileService;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.storage.FirebaseListener;
import com.example.boardgame.storage.FirebaseRoomStorage;

public class GameController {
    private final FirebaseGameStorage gameStorage;
    private final RoomService roomService;
    private final GameService gameService;

    public GameController() {
        this(new FirebaseRoomStorage(), new FirebaseGameStorage());
    }

    public GameController(FirebaseRoomStorage roomStorage, FirebaseGameStorage gameStorage) {
        this.gameStorage = gameStorage;
        this.roomService = new RoomService(roomStorage);
        this.gameService = new GameService(gameStorage, roomStorage, new TileService());
    }

    public GameState startGame(String roomCode) {
        Room room = requireRoom(roomCode);
        return gameService.startGame(room);
    }

    public int rollDice(String roomCode) {
        Room room = requireRoom(roomCode);
        GameState gameState = requireGameState(roomCode);
        return gameService.rollDice(gameState, room);
    }

    public BoardTile applyTileEffect(String roomCode) {
        Room room = requireRoom(roomCode);
        GameState gameState = requireGameState(roomCode);
        return gameService.applyCurrentTileEffect(gameState, room);
    }

    public void startMiniGamePhase(String roomCode) {
        gameService.startMiniGamePhase(requireGameState(roomCode));
    }

    public void startNextRound(String roomCode) {
        gameService.startNextRound(requireGameState(roomCode));
    }

    public void continueAfterMicroGame(String roomCode) {
        gameService.continueAfterMicroGame(requireGameState(roomCode));
    }

    public GameState getGameState(String roomCode) {
        return gameService.getGameState(roomCode);
    }

    public void listenToGameState(String roomCode, FirebaseListener<GameState> listener) {
        gameStorage.listenToGameState(roomCode, listener);
    }

    private Room requireRoom(String roomCode) {
        Room room = roomService.getRoom(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }

    private GameState requireGameState(String roomCode) {
        GameState gameState = gameService.getGameState(roomCode);
        if (gameState == null) {
            throw new IllegalArgumentException("Game state not found");
        }
        return gameState;
    }
}
