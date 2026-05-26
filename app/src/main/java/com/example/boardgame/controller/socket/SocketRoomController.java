package com.example.boardgame.controller.socket;

import com.example.boardgame.socket.BoardGameSocketClient;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.UUID;

public class SocketRoomController {

    private final BoardGameSocketClient socketClient;

    public SocketRoomController() {
        this(new BoardGameSocketClient());
    }

    public SocketRoomController(BoardGameSocketClient socketClient) {
        this.socketClient = socketClient;
    }

    public void setListener(SocketEventListener listener) {
        socketClient.setListener(listener);
    }

    public void connect(String serverUrl) {
        socketClient.connect(serverUrl);
    }

    public void disconnect() {
        socketClient.disconnect();
    }

    public boolean isConnected() {
        return socketClient.getState() == ConnectionState.CONNECTED;
    }

    public void createRoom(String nickname, String firebaseIdToken, String roomPassword) {
        socketClient.send(
                commandBuilder(MessageTypes.CREATE_ROOM, nickname, firebaseIdToken)
                        .put("roomPassword", roomPassword)
                        .build()
        );
    }

    public void joinRoom(String roomCode, String nickname, String firebaseIdToken, String roomPassword) {
        socketClient.send(
                commandBuilder(MessageTypes.JOIN_ROOM, nickname, firebaseIdToken)
                        .put("roomCode", roomCode)
                        .put("roomPassword", roomPassword)
                        .build()
        );
    }

    public void leaveRoom() {
        socketClient.send(
                SocketMessage.builder(MessageTypes.LEAVE_ROOM)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );
    }

    public void setReady(boolean ready) {
        socketClient.send(
                SocketMessage.builder(MessageTypes.SET_READY)
                        .requestId(UUID.randomUUID().toString())
                        .put("ready", ready)
                        .build()
        );
    }

    public void startGame() {
        socketClient.send(
                SocketMessage.builder(MessageTypes.START_GAME)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );
    }

    public void rollDice(int diceRoll) {
        socketClient.send(
                SocketMessage.builder(MessageTypes.ROLL_DICE)
                        .requestId(UUID.randomUUID().toString())
                        .put("diceRoll", diceRoll)
                        .build()
        );
    }

    public void applyTileEffect() {
        socketClient.send(
                SocketMessage.builder(MessageTypes.APPLY_TILE_EFFECT)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );
    }

    public void startMiniGame(String miniGameType) {
        socketClient.send(
                SocketMessage.builder(MessageTypes.START_MINI_GAME)
                        .requestId(UUID.randomUUID().toString())
                        .put("miniGameType", miniGameType)
                        .build()
        );
    }

    public void submitMiniGameScore(int score) {
        socketClient.send(
                SocketMessage.builder(MessageTypes.SUBMIT_MINI_GAME_SCORE)
                        .requestId(UUID.randomUUID().toString())
                        .put("score", score)
                        .build()
        );
    }

    public void finishMiniGame() {
        socketClient.send(
                SocketMessage.builder(MessageTypes.FINISH_MINI_GAME)
                        .requestId(UUID.randomUUID().toString())
                        .build()
        );
    }

    public void submitMicroGameScore(int score) {
        socketClient.send(
                SocketMessage.builder(MessageTypes.SUBMIT_MICRO_GAME_SCORE)
                        .requestId(UUID.randomUUID().toString())
                        .put("score", score)
                        .build()
        );
    }

    private SocketMessage.Builder commandBuilder(
            String type,
            String nickname,
            String firebaseIdToken
    ) {
        return SocketMessage.builder(type)
                .requestId(UUID.randomUUID().toString())
                .put("nickname", nickname)
                .put("firebaseIdToken", firebaseIdToken);
    }
}
