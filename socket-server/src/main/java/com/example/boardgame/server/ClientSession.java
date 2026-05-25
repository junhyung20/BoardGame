package com.example.boardgame.server;

import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketMessage;

import org.java_websocket.WebSocket;

public class ClientSession {
    private final WebSocket webSocket;
    private volatile String roomCode = "";
    private volatile String playerId = "";
    private volatile String firebaseUid = "";

    ClientSession(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public void bindPlayer(String roomCode, String playerId, String firebaseUid) {
        this.roomCode = roomCode == null ? "" : roomCode;
        this.playerId = playerId == null ? "" : playerId;
        this.firebaseUid = firebaseUid == null ? "" : firebaseUid;
    }

    public void clearRoomBinding() {
        this.roomCode = "";
        this.playerId = "";
    }

    void send(SocketMessage message) {
        if (webSocket.isOpen()) {
            webSocket.send(message.toWireText());
        }
    }

    void sendError(SocketMessage request, String errorCode, String details) {
        sendError(request.getRequestId(), errorCode, details);
    }

    void sendError(String requestId, String errorCode, String details) {
        send(SocketMessage.builder(MessageTypes.REQUEST_ERROR)
                .requestId(requestId)
                .put("errorCode", errorCode)
                .put("details", details)
                .build());
    }

    void close() {
        webSocket.close();
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }
}
