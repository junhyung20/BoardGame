package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MicroGameState;
import com.example.boardgame.server.model.MiniGameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RoomService {
    // Use 1 for local UI testing. Raise to 2 when solo starts should be disallowed.
    public static final int MIN_PLAYERS = 1;
    public static final int MAX_PLAYERS = 4;

    public static class MatchResult {
        private final Room room;
        private final Player player;

        public MatchResult(Room room, Player player) {
            this.room = room;
            this.player = player;
        }

        public Room getRoom() { return room; }
        public Player getPlayer() { return player; }
    }

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    public List<Room> getLobbyRooms() {
        List<Room> lobbyRooms = new ArrayList<>();
        for (Room room: rooms.values()) {
            if (isJoinable(room)) {
                lobbyRooms.add(room);
            }
        }
        lobbyRooms.sort(Comparator.comparingLong(Room::getCreatedAtMillis));
        return lobbyRooms;
    }

    public synchronized Room createRoom(String firebaseUid, String nickname) {
        return createRoom(firebaseUid, nickname, "");
    }

    public synchronized Room createRoom(String firebaseUid, String nickname, String roomPassword) {
        requireUidAvailable(firebaseUid); // 중복 로그인 방지
        Room room = new Room(createUniqueRoomCode());
        room.setPassword(roomPassword);
        room.addPlayer(createPlayer(firebaseUid, nickname));
        rooms.put(room.getCode(), room);
        return room;
    }

    public synchronized Player joinRoom(String roomCode, String firebaseUid, String nickname) {
        return joinRoom(roomCode, firebaseUid, nickname, "");
    }

    public synchronized Player joinRoom(String roomCode, String firebaseUid, String nickname, String roomPassword) {
        requireUidAvailable(firebaseUid);
        Room room = requireRoom(roomCode);

        // 이미 게임이 시작되었거나 끝난 방에는 난입 불가
        if (!isWaitingOrReady(room)) {
            throw new IllegalStateException("Room is already in game");
        }
        if (room.getPlayers().size() >= MAX_PLAYERS) {
            throw new IllegalStateException("Room is full");
        }
        if (!room.passwordMatches(roomPassword)) {
            throw new IllegalArgumentException("Invalid room password");
        }

        Player player = createPlayer(firebaseUid, nickname);
        room.addPlayer(player);
        room.refreshReadyStatus(MIN_PLAYERS);
        return player;
    }

    public synchronized MatchResult matchmake(String firebaseUid, String nickname) {
        // 빈자리가 있는 대기방 자동 탐색
        for (Room room : rooms.values()) {
            if (isJoinable(room) && !room.hasPassword()) {
                Player player = joinRoom(room.getCode(), firebaseUid, nickname);
                return new MatchResult(room, player);
            }
        }

        // 빈 방이 없으면 내가 새로 방을 팜
        Room room = createRoom(firebaseUid, nickname, "");
        Player player = room.getPlayerList().iterator().next();
        return new MatchResult(room, player);
    }

    public synchronized void setReady(String roomCode, String playerId, boolean ready) {
        Room room = requireRoom(roomCode);
        Player player = requirePlayer(room, playerId);
        player.setReady(ready);
        room.refreshReadyStatus(MIN_PLAYERS);
    }

    public synchronized void disconnect(String roomCode, String playerId) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            return;
        }

        GameState gameState = room.getGameState();
        MiniGameState miniGameState = room.getMiniGameState();
        if (miniGameState != null) {
            miniGameState.removeScore(playerId);
        }
        MicroGameState microGameState = room.getMicroGameState();
        if (microGameState != null && microGameState.isForPlayer(playerId)) {
            room.setMicroGameState(null);
        }

        room.removePlayer(playerId);

        if (room.getPlayers().isEmpty()) {
            // 방에 아무도 안 남으면 방 폭파
            rooms.remove(roomCode);
        } else if (Room.IN_GAME.equals(room.getStatus())) {
            if (gameState != null) {
                gameState.removePlayer(playerId);
                if (GameState.FINISHED.equals(gameState.getTurnPhase())
                        || room.getPlayers().size() < MIN_PLAYERS) {
                    gameState.setTurnPhase(GameState.FINISHED);
                    room.setStatus(Room.FINISHED);
                } else {
                    room.touch();
                }
            }
        } else {
            room.refreshReadyStatus(MIN_PLAYERS);
        }
    }

    public boolean isWaitingOrReady(Room room) {
        return (Room.WAITING.equals(room.getStatus()) || Room.READY.equals(room.getStatus()));
    }

    public boolean isJoinable(Room room) {
        return isWaitingOrReady(room) && room.getPlayers().size() < MAX_PLAYERS;
    }

    public Room requireRoom(String roomCode) {
        Room room = rooms.get(roomCode);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }
        return room;
    }

    public Player requirePlayer(Room room, String playerId) {
        Player player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room");
        }
        return player;
    }

    public void requireHost(Room room, String playerId) {
        if (!playerId.equals(room.getHostPlayerId())) {
            throw new IllegalStateException("Only the host can start the game");
        }
    }

    public Collection<Room> getRooms() {
        return Collections.unmodifiableCollection(rooms.values());
    }

    private Player createPlayer(String firebaseUid, String nickname) {
        // Player 고유 ID를 생성하여 반환 (Firebase UID와는 별개의 세션 ID 역할)
        return new Player(UUID.randomUUID().toString(), firebaseUid, nickname);
    }

    private void requireUidAvailable(String firebaseUid) {
        if (firebaseUid == null || firebaseUid.trim().isEmpty()) {
            throw new IllegalArgumentException("Firebase UID is required");
        }

        for (Room room : rooms.values()) {
            for (Player player : room.getPlayerList()) {
                if (firebaseUid.equals(player.getFirebaseUid())) {
                    throw new IllegalStateException("User is already in a room");
                }
            }
        }
    }

    private String createUniqueRoomCode() {
        String roomCode;
        do {
            roomCode = String.valueOf(100000 + random.nextInt(900000)); // 6자리 난수
        } while (rooms.containsKey(roomCode));
        return roomCode;
    }
}
