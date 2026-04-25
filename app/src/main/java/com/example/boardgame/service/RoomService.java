package com.example.boardgame.service;

import com.example.boardgame.model.Player;
import com.example.boardgame.model.Room;
import com.example.boardgame.storage.FirebaseRoomStorage;
import com.example.boardgame.util.RoomCodeGenerator;

import java.util.UUID;

public class RoomService {
    private final FirebaseRoomStorage roomStorage;

    public RoomService(FirebaseRoomStorage roomStorage) {
        this.roomStorage = roomStorage;
    }

    public Room createRoom(String nickname) {
        String roomCode = createUniqueRoomCode();
        Room room = new Room(roomCode);
        Player host = createPlayer(nickname);
        room.addPlayer(host);
        roomStorage.saveRoom(room);
        return room;
    }

    public Room joinRoom(String roomCode, String nickname) {
        Room room = requireRoom(roomCode);
        if (room.getStatus() != Room.Status.WAITING) {
            throw new IllegalStateException("Room is not joinable");
        }
        if (!room.addPlayer(createPlayer(nickname))) {
            throw new IllegalStateException("Room is full");
        }
        roomStorage.saveRoom(room);
        return room;
    }

    public Room setReady(String roomCode, String playerId, boolean ready) {
        Room room = requireRoom(roomCode);
        Player player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found");
        }
        player.setReady(ready);
        room.setStatus(room.canStart() ? Room.Status.READY : Room.Status.WAITING);
        roomStorage.saveRoom(room);
        return room;
    }

    public Room getRoom(String roomCode) {
        return roomStorage.getRoom(roomCode);
    }

    public void saveRoom(Room room) {
        roomStorage.saveRoom(room);
    }

    public boolean canStartGame(String roomCode) {
        Room room = requireRoom(roomCode);
        return room.canStart();
    }

    private Player createPlayer(String nickname) {
        return new Player(UUID.randomUUID().toString(), nickname);
    }

    private String createUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = RoomCodeGenerator.generate();
        } while (roomStorage.getRoom(roomCode) != null);
        return roomCode;
    }

    private Room requireRoom(String roomCode) {
        Room room = roomStorage.getRoom(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }
}
