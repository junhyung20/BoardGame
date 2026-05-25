package com.example.boardgame;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.boardgame.auth.FirebaseAuthTokenProvider;
import com.example.boardgame.controller.socket.SocketRoomController;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.LobbySnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotMessageMapper;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public final class ServerSession {
    public interface Listener {
        default void onConnectionStateChanged(ConnectionState state) {
        }

        default void onServerHello(SocketMessage message) {
        }

        default void onLobbyUpdated(LobbySnapshot lobby) {
        }

        default void onRoomUpdated(RoomSnapshot room) {
        }

        default void onGameUpdated(GameSnapshot game) {
        }

        default void onRequestOk(String commandType, String roomCode, String playerId) {
        }

        default void onServerError(String errorCode, String details) {
        }
    }

    public static final String DEFAULT_LAN_URL = "ws://10.0.2.2:8080/game";
    public static final String DEFAULT_WAN_URL = "wss://sandworm-ferret-bath.ngrok-free.dev/game";

    private static final String PREFS_NAME = "boardgame_server_session";
    private static final String KEY_SERVER_URL = "key_server_url";

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Set<Listener> LISTENERS = new CopyOnWriteArraySet<>();
    private static final SocketRoomController CONTROLLER = new SocketRoomController();

    private static volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;
    private static volatile LobbySnapshot latestLobbySnapshot = new LobbySnapshot(null);
    private static volatile RoomSnapshot latestRoomSnapshot;
    private static volatile GameSnapshot latestGameSnapshot;
    private static volatile String currentRoomCode = "";
    private static volatile String currentPlayerId = "";
    private static volatile String pendingCommandType = "";
    private static volatile Runnable pendingConnectedAction;

    static {
        CONTROLLER.setListener(new SocketEventListener() {
            @Override
            public void onStateChanged(ConnectionState state) {
                connectionState = state;
                if (state == ConnectionState.CONNECTED) {
                    Runnable action = pendingConnectedAction;
                    pendingConnectedAction = null;
                    if (action != null) {
                        action.run();
                    }
                }
                dispatch(listener -> listener.onConnectionStateChanged(state));
            }

            @Override
            public void onMessage(SocketMessage message) {
                handleMessage(message);
            }

            @Override
            public void onError(Throwable throwable) {
                String details = throwable == null ? "Socket error" : throwable.getMessage();
                dispatch(listener -> listener.onServerError("CLIENT_SOCKET_ERROR", details));
            }
        });
    }

    private ServerSession() {
    }

    public static void addListener(Listener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static ConnectionState getConnectionState() {
        return connectionState;
    }

    public static LobbySnapshot getLatestLobbySnapshot() {
        return latestLobbySnapshot;
    }

    public static RoomSnapshot getLatestRoomSnapshot() {
        return latestRoomSnapshot;
    }

    public static GameSnapshot getLatestGameSnapshot() {
        return latestGameSnapshot;
    }

    public static String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public static String getCurrentRoomCode() {
        return currentRoomCode;
    }

    public static String getServerUrl(Context context) {
        return prefs(context).getString(KEY_SERVER_URL, DEFAULT_LAN_URL);
    }

    public static void setServerUrl(Context context, String serverUrl) {
        String normalizedUrl = normalizeServerUrl(serverUrl);
        prefs(context).edit().putString(KEY_SERVER_URL, normalizedUrl).apply();
        if (connectionState != ConnectionState.DISCONNECTED) {
            disconnect();
        }
    }

    public static String normalizeServerUrl(String serverUrl) {
        String value = serverUrl == null ? "" : serverUrl.trim();
        if (value.isEmpty()) {
            return DEFAULT_LAN_URL;
        }
        if (!value.endsWith("/game")) {
            value = value.endsWith("/") ? value + "game" : value + "/game";
        }
        return value;
    }

    public static void useDefaultLan(Context context) {
        setServerUrl(context, DEFAULT_LAN_URL);
    }

    public static void useDefaultWan(Context context) {
        setServerUrl(context, DEFAULT_WAN_URL);
    }

    public static void connect(Context context) {
        if (connectionState == ConnectionState.CONNECTED || connectionState == ConnectionState.CONNECTING) {
            return;
        }
        CONTROLLER.connect(getServerUrl(context));
    }

    public static void disconnect() {
        pendingConnectedAction = null;
        currentRoomCode = "";
        currentPlayerId = "";
        latestRoomSnapshot = null;
        latestGameSnapshot = null;
        CONTROLLER.disconnect();
    }

    public static void createRoom(Context context, String nickname, String roomPassword) {
        runWhenConnected(context, () -> withIdToken(context, token -> {
            pendingCommandType = MessageTypes.CREATE_ROOM;
            CONTROLLER.createRoom(nickname, token, roomPassword);
        }));
    }

    public static void joinRoom(Context context, String roomCode, String nickname, String roomPassword) {
        runWhenConnected(context, () -> withIdToken(context, token -> {
            pendingCommandType = MessageTypes.JOIN_ROOM;
            CONTROLLER.joinRoom(roomCode, nickname, token, roomPassword);
        }));
    }

    public static void matchmake(Context context, String nickname) {
        runWhenConnected(context, () -> withIdToken(context, token -> {
            pendingCommandType = MessageTypes.MATCHMAKE;
            CONTROLLER.matchmake(nickname, token);
        }));
    }

    public static void leaveRoom() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.LEAVE_ROOM;
        CONTROLLER.leaveRoom();
        currentRoomCode = "";
        currentPlayerId = "";
        latestRoomSnapshot = null;
        latestGameSnapshot = null;
    }

    public static void setReady(boolean ready) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.SET_READY;
        CONTROLLER.setReady(ready);
    }

    public static void startGame() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.START_GAME;
        CONTROLLER.startGame();
    }

    public static void rollDice() {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.ROLL_DICE;
        CONTROLLER.rollDice();
    }

    public static void submitMiniGameScore(int score) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.SUBMIT_MINI_GAME_SCORE;
        CONTROLLER.submitMiniGameScore(score);
    }

    public static void submitMicroGameScore(int score) {
        if (!CONTROLLER.isConnected()) {
            return;
        }
        pendingCommandType = MessageTypes.SUBMIT_MICRO_GAME_SCORE;
        CONTROLLER.submitMicroGameScore(score);
    }

    private static void runWhenConnected(Context context, Runnable action) {
        if (connectionState == ConnectionState.CONNECTED) {
            action.run();
            return;
        }
        pendingConnectedAction = action;
        connect(context.getApplicationContext());
    }

    private static void withIdToken(Context context, TokenConsumer consumer) {
        Context appContext = context.getApplicationContext();
        try {
            new FirebaseAuthTokenProvider().requireIdToken(new FirebaseAuthTokenProvider.TokenCallback() {
                @Override
                public void onToken(String idToken) {
                    consumer.accept(idToken);
                }

                @Override
                public void onError(Exception exception) {
                    consumer.accept(SessionPrefs.getOrCreateDevClientToken(appContext));
                }
            });
        } catch (RuntimeException exception) {
            consumer.accept(SessionPrefs.getOrCreateDevClientToken(appContext));
        }
    }

    private static void handleMessage(SocketMessage message) {
        String type = message.getType();
        if (MessageTypes.SERVER_HELLO.equals(type)) {
            dispatch(listener -> listener.onServerHello(message));
            return;
        }
        if (MessageTypes.LOBBY_UPDATED.equals(type)) {
            latestLobbySnapshot = SnapshotMessageMapper.toLobbySnapshot(message);
            dispatch(listener -> listener.onLobbyUpdated(latestLobbySnapshot));
            return;
        }
        if (MessageTypes.ROOM_UPDATED.equals(type)) {
            latestRoomSnapshot = SnapshotMessageMapper.toRoomSnapshot(message);
            currentRoomCode = latestRoomSnapshot.getCode();
            dispatch(listener -> listener.onRoomUpdated(latestRoomSnapshot));
            return;
        }
        if (MessageTypes.GAME_UPDATED.equals(type)) {
            latestGameSnapshot = SnapshotMessageMapper.toGameSnapshot(message);
            dispatch(listener -> listener.onGameUpdated(latestGameSnapshot));
            return;
        }
        if (MessageTypes.REQUEST_OK.equals(type)) {
            String roomCode = message.getOrDefault("roomCode", "");
            String playerId = message.getOrDefault("playerId", "");
            if (!roomCode.isEmpty()) {
                currentRoomCode = roomCode;
            }
            if (!playerId.isEmpty()) {
                currentPlayerId = playerId;
            }
            String commandType = pendingCommandType;
            pendingCommandType = "";
            dispatch(listener -> listener.onRequestOk(commandType, roomCode, playerId));
            return;
        }
        if (MessageTypes.REQUEST_ERROR.equals(type)) {
            String commandType = pendingCommandType;
            pendingCommandType = "";
            if (MessageTypes.LEAVE_ROOM.equals(commandType)) {
                currentRoomCode = "";
                currentPlayerId = "";
                latestRoomSnapshot = null;
                latestGameSnapshot = null;
            }
            dispatch(listener -> listener.onServerError(
                    message.getOrDefault("errorCode", "REQUEST_ERROR"),
                    message.getOrDefault("details", "Request failed")
            ));
        }
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void dispatch(DispatchAction action) {
        MAIN.post(() -> {
            for (Listener listener : LISTENERS) {
                action.dispatch(listener);
            }
        });
    }

    private interface DispatchAction {
        void dispatch(Listener listener);
    }

    private interface TokenConsumer {
        void accept(String token);
    }
}
