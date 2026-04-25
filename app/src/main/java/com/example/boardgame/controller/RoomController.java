package com.example.boardgame.controller;

import com.example.boardgame.model.Room;
import com.example.boardgame.service.RoomService;
import com.example.boardgame.storage.FirebaseListener;
import com.example.boardgame.storage.FirebaseRoomStorage;

public class RoomController {
    private final RoomService roomService;
    private final FirebaseRoomStorage roomStorage;

    public RoomController() {
        this(new FirebaseRoomStorage());
    }

    public RoomController(FirebaseRoomStorage roomStorage) {
        this.roomStorage = roomStorage;
        this.roomService = new RoomService(roomStorage);
    }

    public Room createRoom(String nickname) {
        return roomService.createRoom(nickname);
    }

    public Room joinRoom(String roomCode, String nickname) {
        return roomService.joinRoom(roomCode, nickname);
    }

    public Room setReady(String roomCode, String playerId, boolean ready) {
        return roomService.setReady(roomCode, playerId, ready);
    }

    public boolean canStartGame(String roomCode) {
        return roomService.canStartGame(roomCode);
    }

    public Room getRoom(String roomCode) {
        return roomService.getRoom(roomCode);
    }

    public void listenToRoom(String roomCode, FirebaseListener<Room> listener) {
        roomStorage.listenToRoom(roomCode, listener);
    }
}
