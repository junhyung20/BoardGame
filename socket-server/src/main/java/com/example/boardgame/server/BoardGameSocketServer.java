package com.example.boardgame.server;

import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.SocketMessage;
import com.example.boardgame.socket.protocol.ErrorCodes;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BoardGameSocketServer extends WebSocketServer {
    private static final int DEFAULT_PORT = 8080;
    private static final int PROTOCOL_VERSION = 1;
    private static final String NETWORK_ENV = "BOARDGAME_NETWORK";
    private static final String WAN_NETWORK = "WAN";
    private static final String LAN_BIND_HOST = "0.0.0.0";
    private static final String WAN_BIND_HOST = "127.0.0.1";

    private final Map<WebSocket, ClientSession> sessions = new ConcurrentHashMap<>();
    private final GameSocketHandler gameSocketHandler;

    public BoardGameSocketServer(int port) {
        this(resolveBindHost(System.getenv()), port, createAuthVerifier(System.getenv()));
    }

    BoardGameSocketServer(int port, AuthVerifier authVerifier) {
        this(LAN_BIND_HOST, port, authVerifier);
    }

    BoardGameSocketServer(String bindHost, int port, AuthVerifier authVerifier) {
        super(new InetSocketAddress(bindHost, port));
        gameSocketHandler = new GameSocketHandler(this, authVerifier);
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? parsePort(args[0]) : DEFAULT_PORT;
        new BoardGameSocketServer(port).start();
    }

    static int parsePort(String rawPort) {
        int port = Integer.parseInt(rawPort.trim());
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        return port;
    }

    static String resolveBindHost(Map<String, String> env) {
        return isWan(env) ? WAN_BIND_HOST : LAN_BIND_HOST;
    }

    static boolean isFirebaseAuthConfigured(Map<String, String> env) {
        return hasValue(env, FirebaseAdminAuthVerifier.SERVICE_ACCOUNT_ENV)
                || hasValue(env, FirebaseAdminAuthVerifier.GOOGLE_APPLICATION_CREDENTIALS_ENV);
    }

    private static boolean isWan(Map<String, String> env) {
        return WAN_NETWORK.equalsIgnoreCase(value(env, NETWORK_ENV));
    }

    private static AuthVerifier createAuthVerifier(Map<String, String> env) {
        if (isFirebaseAuthConfigured(env)) {
            return new FirebaseAdminAuthVerifier();
        }
        System.out.println("WARNING: Firebase service-account env is not set. Firebase token verification is disabled.");
        return new DevAuthVerifier();
    }

    private static boolean hasValue(Map<String, String> env, String key) {
        return !value(env, key).isEmpty();
    }

    private static String value(Map<String, String> env, String key) {
        if (env == null || key == null) {
            return "";
        }
        String value = env.get(key);
        return value == null ? "" : value.trim();
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        ClientSession session = new ClientSession(connection);
        sessions.put(connection, session);
        System.out.println("event=socket_open remote=" + remoteAddress(connection));
        session.send(serverHello());
        gameSocketHandler.sendLobbySnapshot(session);
    }

    @Override
    public void onMessage(WebSocket connection, String text) {
        ClientSession session = sessions.get(connection);
        if (session == null) {
            return;
        }

        SocketMessage message;
        try {
            message = SocketMessage.parse(text);
        } catch (RuntimeException e) {
            System.err.println("event=malformed_message remote=" + remoteAddress(connection)
                    + " cause=" + e.getClass().getSimpleName());
            session.sendError("", ErrorCodes.MALFORMED_MESSAGE, "Malformed socket message");
            return;
        }
        if (MessageTypes.APP_PING.equals(message.getType())) {
            session.send(SocketMessage.builder(MessageTypes.APP_PONG)
                    .requestId(message.getRequestId())
                    .build());
            return;
        }
        gameSocketHandler.handle(session, message);
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        ClientSession session = sessions.remove(connection);
        if (session != null) {
            gameSocketHandler.disconnect(session);
        }
        System.out.println("event=socket_close remote=" + remoteAddress(connection)
                + " code=" + code
                + " remoteClosed=" + remote
                + " reason=" + sanitizeLogValue(reason));
    }

    @Override
    public void onError(WebSocket connection, Exception exception) {
        System.err.println("event=socket_error remote=" + remoteAddress(connection)
                + " cause=" + exception.getClass().getSimpleName()
                + " message=" + sanitizeLogValue(exception.getMessage()));
    }

    @Override
    public void onStart() {
        System.out.println("BoardGame socket server listening on ws://" + getAddress().getHostString()
                + ":" + getPort() + "/game");
        System.out.println("BoardGame network=" + (isWan(System.getenv()) ? "WAN" : "LAN")
                + " auth=" + (isFirebaseAuthConfigured(System.getenv()) ? "FIREBASE" : "DEV"));
        System.out.println("For ngrok WAN testing: ngrok http " + getPort()
                + " then connect Android to wss://<ngrok-domain>/game");
        setConnectionLostTimeout(30);
    }

    void sendToRoom(String roomCode, SocketMessage message) {
        for (ClientSession session : sessions.values()) {
            if (roomCode.equals(session.getRoomCode())) {
                session.send(message);
            }
        }
    }

    void sendToLobby(SocketMessage message) {
        for (ClientSession session : sessions.values()) {
            if (session.getRoomCode().isEmpty()) {
                session.send(message);
            }
        }
    }

    private SocketMessage serverHello() {
        Map<String, String> env = System.getenv();
        return SocketMessage.builder(MessageTypes.SERVER_HELLO)
                .put("protocolVersion", PROTOCOL_VERSION)
                .put("serverTimeMillis", System.currentTimeMillis())
                .put("network", isWan(env) ? "WAN" : "LAN")
                .put("authMode", isFirebaseAuthConfigured(env) ? "FIREBASE" : "DEV")
                .build();
    }

    private String remoteAddress(WebSocket connection) {
        if (connection == null || connection.getRemoteSocketAddress() == null) {
            return "-";
        }
        return sanitizeLogValue(connection.getRemoteSocketAddress().toString());
    }

    private String sanitizeLogValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.replaceAll("\\s+", "_");
    }
}
