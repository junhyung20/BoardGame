package com.example.boardgame.socket;

import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class BoardGameSocketClient {
    private static final int HEARTBEAT_SECONDS = 20;
    private static final int NORMAL_CLOSE = 1000;

    private final OkHttpClient okHttpClient;
    private SocketEventListener listener;
    private ScheduledExecutorService heartbeatExecutor;
    private WebSocket webSocket;
    private volatile ConnectionState state = ConnectionState.DISCONNECTED;
    private volatile boolean manualDisconnect = false;

    public BoardGameSocketClient() {
        this(new OkHttpClient());
    }

    public BoardGameSocketClient(OkHttpClient okHttpClient) {
        this.okHttpClient = okHttpClient;
    }

    public void setListener(SocketEventListener listener) {
        this.listener = listener;
    }

    public synchronized void connect(String serverUrl) {
        if (state == ConnectionState.CONNECTED || state == ConnectionState.CONNECTING) {
            return;
        }

        manualDisconnect = false;

        changeState(ConnectionState.CONNECTING);

        Request request = new Request.Builder()
                .url(serverUrl)
                .build();

        webSocket = okHttpClient.newWebSocket(request, new BoardGameWebSocketListener());
    }

    public void send(SocketMessage message) {
        if (state != ConnectionState.CONNECTED || webSocket == null) {
            notifyError(new IllegalStateException("Socket is not connected"));
            return;
        }

        boolean success = webSocket.send(message.toWireText());

        if (!success) {
            notifyError(new IllegalStateException("Failed to send socket message"));
        }
    }

    public synchronized void disconnect() {
        if (state == ConnectionState.DISCONNECTED) {
            return;
        }

        manualDisconnect = true;

        changeState(ConnectionState.CLOSING);

        stopHeartbeat();

        if (webSocket != null) {
            webSocket.close(NORMAL_CLOSE, "client disconnect");
            webSocket = null;
        }

        changeState(ConnectionState.DISCONNECTED);
    }

    public ConnectionState getState() {
        return state;
    }

    private void startHeartbeat() {
        stopHeartbeat();

        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "boardgame-socket-heartbeat");
            thread.setDaemon(true);
            return thread;
        });

        heartbeatExecutor.scheduleWithFixedDelay(() -> {
            if (state == ConnectionState.CONNECTED) {
                send(SocketMessage.builder(MessageTypes.APP_PING)
                        .requestId(UUID.randomUUID().toString())
                        .build());
            }
        }, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    private void stopHeartbeat() {
        if (heartbeatExecutor != null) {
            heartbeatExecutor.shutdownNow();
            heartbeatExecutor = null;
        }
    }

    private void changeState(ConnectionState newState) {
        state = newState;

        SocketEventListener currentListener = listener;

        if (currentListener != null) {
            currentListener.onStateChanged(newState);
        }
    }

    private void notifyError(Throwable throwable) {
        SocketEventListener currentListener = listener;

        if (currentListener != null) {
            currentListener.onError(throwable);
        }
    }

    private class BoardGameWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            changeState(ConnectionState.CONNECTED);
            startHeartbeat();
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            SocketEventListener currentListener = listener;

            if (currentListener == null) {
                return;
            }

            try {
                currentListener.onMessage(SocketMessage.parse(text));
            } catch (Exception e) {
                notifyError(new IllegalStateException(
                        "Failed to parse message: " + text,
                        e
                ));
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            changeState(ConnectionState.CLOSING);
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            stopHeartbeat();

            BoardGameSocketClient.this.webSocket = null;

            changeState(ConnectionState.DISCONNECTED);

            if (!manualDisconnect) {
                notifyError(new IllegalStateException(
                        "서버 연결이 끊겼습니다. (code=" + code + ")"
                ));
            }
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            stopHeartbeat();

            BoardGameSocketClient.this.webSocket = null;

            changeState(ConnectionState.DISCONNECTED);

            notifyError(throwable);
        }
    }
}