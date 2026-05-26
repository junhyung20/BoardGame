package com.example.boardgame.server;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;
import com.example.boardgame.server.service.BoardGameService;
import com.example.boardgame.server.service.MicroGameService;
import com.example.boardgame.server.service.MiniGameService;
import com.example.boardgame.server.service.RoomService;
import com.example.boardgame.server.service.ScoreService;
import com.example.boardgame.socket.protocol.ErrorCodes;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.LobbySnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotMessageMapper;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.ArrayList;

public class GameSocketHandler {
    private final BoardGameSocketServer socketServer;
    private final AuthVerifier authVerifier;
    private final RoomService roomService = new RoomService();
    private final ScoreService scoreService = new ScoreService();
    private final BoardGameService boardGameService = new BoardGameService();
    private final MiniGameService miniGameService = new MiniGameService(boardGameService, scoreService);
    private final MicroGameService microGameService = new MicroGameService(boardGameService);

    public GameSocketHandler(BoardGameSocketServer socketServer, AuthVerifier authVerifier) {
        this.socketServer = socketServer;
        this.authVerifier = authVerifier;
    }

    public synchronized void handle(ClientSession session, SocketMessage message) {
        try {
            handleCommand(session, message);
        } catch (AuthException e) {
            ServerError error = ServerErrorMapper.from(e);
            logCommandFailure(session, message, error, e);
            session.sendError(message, error.code(), error.details());
        } catch (IllegalArgumentException | IllegalStateException e) {
            ServerError error = ServerErrorMapper.from(e);
            logCommandFailure(session, message, error, e);
            session.sendError(message, error.code(), error.details());
        } catch (RuntimeException e) {
            ServerError error = new ServerError(ErrorCodes.INVALID_STATE, "Server failed to process command");
            logCommandFailure(session, message, error, e);
            session.sendError(message, error.code(), error.details());
        }
    }

    public synchronized void disconnect(ClientSession session) {
        String roomCode = session.getRoomCode();
        String playerId = session.getPlayerId();
        if (roomCode.isEmpty() || playerId.isEmpty()) {
            return;
        }

        roomService.disconnect(roomCode, playerId);
        try {
            Room room = roomService.requireRoom(roomCode);
            publishRoom(room);
            publishGame(room);
        } catch (IllegalArgumentException ignored) {
            // The room was removed because the last player disconnected.
        }
        publishLobby();
    }

    public synchronized void sendLobbySnapshot(ClientSession session) {
        session.send(SnapshotMessageMapper.lobbyUpdated(lobbySnapshot()));
    }

    public synchronized RoomService.CleanupResult cleanupStaleRooms() {
        RoomService.CleanupResult result = roomService.cleanupStaleRooms(System.currentTimeMillis());
        if (result.hasChanges()) {
            publishLobby();
        }
        return result;
    }

    private void handleCommand(ClientSession session, SocketMessage message) {
        switch (message.getType()) {
            case MessageTypes.CREATE_ROOM -> createRoom(session, message);
            case MessageTypes.JOIN_ROOM -> joinRoom(session, message);
            case MessageTypes.LEAVE_ROOM -> leaveRoom(session, message);
            case MessageTypes.SET_READY -> setReady(session, message);
            case MessageTypes.START_GAME -> startGame(session, message);
            case MessageTypes.ROLL_DICE -> rollDice(session, message);
            case MessageTypes.APPLY_TILE_EFFECT -> applyTileEffect(session, message);
            case MessageTypes.START_MINI_GAME -> startMiniGame(session, message);
            case MessageTypes.SUBMIT_MINI_GAME_SCORE -> submitMiniGameScore(session, message);
            case MessageTypes.FINISH_MINI_GAME -> finishMiniGame(session, message);
            case MessageTypes.SUBMIT_MICRO_GAME_SCORE -> submitMicroGameScore(session, message);
            default -> throw new IllegalArgumentException("Unsupported type: " + message.getType());
        }
    }

    private void createRoom(ClientSession session, SocketMessage message) {
        requireNotInRoom(session);
        String firebaseUid = verify(message);
        Room room = roomService.createRoom(
                firebaseUid,
                message.getOrDefault("nickname", "Player"),
                message.getOrDefault("roomPassword", "")
        );
        Player player = room.getPlayerList().iterator().next();
        session.bindPlayer(room.getCode(), player.getId(), firebaseUid);
        sendOk(session, message, room);
        publishRoom(room);
        publishLobby();
    }

    private void joinRoom(ClientSession session, SocketMessage message) {
        requireNotInRoom(session);
        String roomCode = message.getOrDefault("roomCode", "");
        String firebaseUid = verify(message);
        Player player = roomService.joinRoom(
                roomCode,
                firebaseUid,
                message.getOrDefault("nickname", "Player"),
                message.getOrDefault("roomPassword", "")
        );
        Room room = roomService.requireRoom(roomCode);
        session.bindPlayer(room.getCode(), player.getId(), firebaseUid);
        sendOk(session, message, room);
        publishRoom(room);
        publishLobby();
    }

    private void setReady(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.setReady(room.getCode(), session.getPlayerId(), message.getBoolean("ready", false));
        sendOk(session, message, room);
        publishRoom(room);
        publishLobby();
    }

    private void startGame(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
        publishLobby();
    }

    private void rollDice(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        boardGameService.rollDice(room, session.getPlayerId(), message.getInt("diceRoll", 0));
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void applyTileEffect(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        String tileType = boardGameService.applyTileEffect(room, session.getPlayerId());
        if (BoardGameService.TILE_AD.equals(tileType)) {
            microGameService.startMicroGame(room, session.getPlayerId());
        }
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void startMiniGame(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        miniGameService.startMiniGame(room);
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void submitMiniGameScore(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        miniGameService.submitMiniGameScore(room, session.getPlayerId(), message.getInt("score", 0));
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void finishMiniGame(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        roomService.requireHost(room, session.getPlayerId());
        miniGameService.finishMiniGame(room);
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void submitMicroGameScore(ClientSession session, SocketMessage message) {
        Room room = requireBoundRoom(session);
        microGameService.submitMicroGameScore(room, session.getPlayerId(), message.getInt("score", 0));
        sendOk(session, message, room);
        publishRoom(room);
        publishGame(room);
    }

    private void leaveRoom(ClientSession session, SocketMessage message) {
        String roomCode = session.getRoomCode();
        String playerId = session.getPlayerId();
        if (roomCode.isEmpty() || playerId.isEmpty()) {
            throw new IllegalStateException("Connection is not in a room");
        }

        Room room = roomService.requireRoom(roomCode);
        if (Room.IN_GAME.equals(room.getStatus())) {
            throw new IllegalStateException("Cannot leave after game has started");
        }

        roomService.disconnect(roomCode, playerId);
        session.clearRoomBinding();
        sendOk(session, message);
        try {
            publishRoom(roomService.requireRoom(roomCode));
        } catch (IllegalArgumentException ignored) {
            // The room was removed because the last player left.
        }
        publishLobby();
    }

    private void sendOk(ClientSession session, SocketMessage request, Room room) {
        SocketMessage.Builder builder = SocketMessage.builder(MessageTypes.REQUEST_OK)
                .requestId(request.getRequestId());
        if (!session.getPlayerId().isEmpty()) {
            builder.put("roomCode", room.getCode())
                    .put("playerId", session.getPlayerId())
                    .put("status", room.getStatus());
        }
        session.send(builder.build());
    }

    private void sendOk(ClientSession session, SocketMessage request) {
        session.send(SocketMessage.builder(MessageTypes.REQUEST_OK)
                .requestId(request.getRequestId())
                .build());
    }

    private void publishRoom(Room room) {
        RoomSnapshot snapshot = room.toSnapshot();
        socketServer.sendToRoom(room.getCode(), SnapshotMessageMapper.roomUpdated(snapshot));
    }

    private void publishGame(Room room) {
        GameState gameState = room.getGameState();
        if (gameState == null) {
            return;
        }
        GameSnapshot snapshot = gameState.toSnapshot();
        socketServer.sendToRoom(room.getCode(), SnapshotMessageMapper.gameUpdated(snapshot));
    }

    private void publishLobby() {
        socketServer.sendToLobby(SnapshotMessageMapper.lobbyUpdated(lobbySnapshot()));
    }

    private LobbySnapshot lobbySnapshot() {
        ArrayList<LobbySnapshot.RoomListInfo> rooms = new ArrayList<>();
        for (Room room: roomService.getLobbyRooms()) {
            rooms.add(room.toRoomListInfo());
        }
        return new LobbySnapshot(rooms);
    }

    private Room requireBoundRoom(ClientSession session) {
        return roomService.requireRoom(session.getRoomCode());
    }

    private void requireNotInRoom(ClientSession session) {
        if (!session.getRoomCode().isEmpty()) {
            throw new IllegalStateException("Connection is already in a room");
        }
    }

    private String verify(SocketMessage message) {
        return authVerifier.verify(message.getOrDefault("firebaseIdToken", ""));
    }

    private void logCommandFailure(ClientSession session, SocketMessage message, ServerError error, RuntimeException e) {
        System.err.println("event=command_failed"
                + " type=" + message.getType()
                + " requestId=" + message.getRequestId()
                + " roomCode=" + emptyAsDash(session.getRoomCode())
                + " playerId=" + emptyAsDash(session.getPlayerId())
                + " errorCode=" + error.code()
                + " cause=" + e.getClass().getSimpleName());
    }

    private String emptyAsDash(String value) {
        return value == null || value.isEmpty() ? "-" : value;
    }

}
