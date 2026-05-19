package com.example.boardgame;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.boardgame.auth.FirebaseAuthTokenProvider;
import com.example.boardgame.controller.socket.SocketRoomController;
import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.MessageTypes;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.socket.protocol.SnapshotMessageMapper;
import com.example.boardgame.socket.protocol.SocketEventListener;
import com.example.boardgame.socket.protocol.SocketMessage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuthException;

public class MainActivity extends AppCompatActivity {

    private final SocketRoomController socketController =
            new SocketRoomController();

    private FirebaseAuthTokenProvider authTokenProvider;

    private EditText serverUrlInput;
    private EditText nicknameInput;
    private EditText roomCodeInput;
    private EditText scoreInput;

    private TextView connectionStateText;
    private TextView myPlayerText;
    private TextView roomStateText;
    private TextView gameStateText;
    private TextView eventLogText;

    private String myPlayerId = "";

    private int currentRound = 1;

    private final StringBuilder eventLog =
            new StringBuilder("Log:");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                });

        bindViews();

        initializeFirebase();

        bindSocket();

        bindButtons();
    }

    @Override
    protected void onDestroy() {
        socketController.disconnect();
        super.onDestroy();
    }

    private void bindViews() {

        serverUrlInput = findViewById(R.id.serverUrlInput);

        nicknameInput = findViewById(R.id.nicknameInput);

        roomCodeInput = findViewById(R.id.roomCodeInput);

        scoreInput = findViewById(R.id.scoreInput);

        connectionStateText =
                findViewById(R.id.connectionStateText);

        myPlayerText =
                findViewById(R.id.myPlayerText);

        roomStateText =
                findViewById(R.id.roomStateText);

        gameStateText =
                findViewById(R.id.gameStateText);

        eventLogText =
                findViewById(R.id.eventLogText);
    }

    private void initializeFirebase() {

        try {

            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this);
            }

            if (!FirebaseApp.getApps(this).isEmpty()) {

                authTokenProvider =
                        new FirebaseAuthTokenProvider();

            } else {

                appendLog(
                        "Auth error: missing app/google-services.json"
                );
            }

        } catch (IllegalStateException e) {

            appendLog("Auth error: " + e.getMessage());
        }
    }

    private void bindSocket() {

        socketController.setListener(new SocketEventListener() {

            @Override
            public void onStateChanged(ConnectionState state) {

                runOnUiThread(() ->
                        connectionStateText.setText(
                                "Connection: " + state.name()));
            }

            @Override
            public void onMessage(SocketMessage message) {

                runOnUiThread(() ->
                        handleSocketMessage(message));
            }

            @Override
            public void onError(Throwable throwable) {

                runOnUiThread(() -> {

                    String errorMessage =
                            throwable.getMessage();

                    appendLog(
                            "Error: " +
                                    (errorMessage == null
                                            ? throwable.getClass().getSimpleName()
                                            : errorMessage)
                    );
                });
            }
        });
    }

    private void bindButtons() {

        findViewById(R.id.connectButton)
                .setOnClickListener(v -> {

                    if (socketController.isConnected()) {

                        appendLog("Already connected");

                        return;
                    }

                    String serverUrl =
                            textOf(serverUrlInput);

                    if (serverUrl.isEmpty()) {

                        appendLog("Server URL is empty");

                        return;
                    }

                    socketController.connect(serverUrl);
                });

        findViewById(R.id.disconnectButton)
                .setOnClickListener(v ->
                        socketController.disconnect());

        findViewById(R.id.createRoomButton)
                .setOnClickListener(v ->
                        withIdToken(token ->
                                socketController.createRoom(
                                        nickname(),
                                        token
                                )));

        findViewById(R.id.joinRoomButton)
                .setOnClickListener(v -> {

                    String roomCode =
                            textOf(roomCodeInput);

                    if (roomCode.isEmpty()) {

                        appendLog("Room code is empty");

                        return;
                    }

                    withIdToken(token ->
                            socketController.joinRoom(
                                    roomCode,
                                    nickname(),
                                    token
                            ));
                });

        findViewById(R.id.matchmakeButton)
                .setOnClickListener(v ->
                        withIdToken(token ->
                                socketController.matchmake(
                                        nickname(),
                                        token
                                )));

        findViewById(R.id.readyButton)
                .setOnClickListener(v ->
                        socketController.setReady(true));

        findViewById(R.id.unreadyButton)
                .setOnClickListener(v ->
                        socketController.setReady(false));

        findViewById(R.id.startGameButton)
                .setOnClickListener(v ->
                        socketController.startGame());

        findViewById(R.id.rollDiceButton)
                .setOnClickListener(v ->
                        socketController.rollDice());

        findViewById(R.id.applyTileButton)
                .setOnClickListener(v ->
                        socketController.applyTileEffect());

        findViewById(R.id.startMiniGameButton)
                .setOnClickListener(v ->
                        socketController.startMiniGame(
                                getMiniGameTypeForRound(currentRound)
                        ));

        findViewById(R.id.submitMiniScoreButton)
                .setOnClickListener(v ->
                        socketController.submitMiniGameScore(
                                score()
                        ));

        findViewById(R.id.finishMiniGameButton)
                .setOnClickListener(v ->
                        socketController.finishMiniGame());

        findViewById(R.id.submitMicroScoreButton)
                .setOnClickListener(v ->
                        socketController.submitMicroGameScore(
                                score()
                        ));

        findViewById(R.id.finishMicroGameButton)
                .setOnClickListener(v ->
                        socketController.finishMicroGame());
    }

    private void handleSocketMessage(SocketMessage message) {

        if (message == null) {

            appendLog("Received null socket message");

            return;
        }

        if (MessageTypes.REQUEST_OK.equals(message.getType())) {

            myPlayerId =
                    message.getOrDefault(
                            "playerId",
                            myPlayerId
                    );

            String roomCode =
                    message.getOrDefault(
                            "roomCode",
                            ""
                    );

            if (!roomCode.isEmpty()) {
                roomCodeInput.setText(roomCode);
            }

            myPlayerText.setText(
                    "Player: " + shortId(myPlayerId)
            );

            appendLog(
                    "OK " +
                            message.getOrDefault(
                                    "status",
                                    ""
                            )
            );

            socketController.onRollResponseReceived();

        } else if (MessageTypes.REQUEST_ERROR.equals(message.getType())) {

            appendLog(
                    "Request error: " +
                            message.getOrDefault(
                                    "details",
                                    ""
                            )
            );

            socketController.onRollResponseReceived();

        } else if (MessageTypes.ROOM_UPDATED.equals(message.getType())) {

            renderRoom(
                    SnapshotMessageMapper.toRoomSnapshot(message)
            );

        } else if (MessageTypes.GAME_UPDATED.equals(message.getType())) {

            renderGame(
                    SnapshotMessageMapper.toGameSnapshot(message)
            );

        } else if (MessageTypes.MINI_GAME_UPDATED.equals(message.getType())) {

            appendLog(
                    "미니게임 업데이트: " +
                            message.getOrDefault(
                                    "status",
                                    ""
                            )
            );

        } else if (MessageTypes.MICRO_GAME_UPDATED.equals(message.getType())) {

            appendLog(
                    "마이크로게임 업데이트: " +
                            message.getOrDefault(
                                    "status",
                                    ""
                            )
            );

        } else if (MessageTypes.SERVER_NOTICE.equals(message.getType())) {

            appendLog(
                    "서버 알림: " +
                            message.getOrDefault(
                                    "message",
                                    ""
                            )
            );

        } else if (!MessageTypes.APP_PONG.equals(message.getType())) {

            appendLog(
                    "Message: " + message.getType()
            );
        }
    }

    private void renderRoom(RoomSnapshot room) {

        StringBuilder builder = new StringBuilder();

        builder.append("Room ")
                .append(room.getCode())
                .append(" / ")
                .append(room.getStatus())
                .append("\nHost: ")
                .append(shortId(room.getHostPlayerId()));

        for (PlayerSnapshot player : room.getPlayers()) {

            builder.append("\n")
                    .append(player.isHost() ? "* " : "- ")
                    .append(player.getNickname())
                    .append(" [")
                    .append(shortId(player.getId()))
                    .append("]")
                    .append(" ready=")
                    .append(player.isReady())
                    .append(" pos=")
                    .append(player.getPosition())
                    .append(" score=")
                    .append(player.getScore());
        }

        roomStateText.setText(builder.toString());
    }

    private void renderGame(GameSnapshot game) {

        currentRound = game.getCurrentRound();

        gameStateText.setText(
                "Game room=" + game.getRoomCode()
                        + "\nround=" +
                        game.getCurrentRound() +
                        "/" +
                        game.getFinalRound()
                        + "\nphase=" +
                        game.getTurnPhase()
                        + "\ncurrent=" +
                        shortId(game.getCurrentPlayerId())
                        + "\ndice=" +
                        game.getLastDiceRoll()
        );
    }

    private String getMiniGameTypeForRound(int round) {

        switch (round) {

            case 1:
                return "COLOR_CAPTCHA";

            case 2:
                return "PASSWORD_CREATION";

            case 3:
                return "VOLUME_MAZE";

            default:
                return "COLOR_CAPTCHA";
        }
    }

    private void appendLog(String line) {

        eventLog.append("\n").append(line);

        eventLogText.setText(eventLog.toString());
    }

    private String nickname() {

        String nickname = textOf(nicknameInput);

        return nickname.isEmpty()
                ? "Player"
                : nickname;
    }

    private int score() {

        String scoreText = textOf(scoreInput);

        if (scoreText.isEmpty()) {
            return 0;
        }

        try {

            return Integer.parseInt(scoreText);

        } catch (NumberFormatException e) {

            appendLog("Invalid score: " + scoreText);

            return 0;
        }
    }

    private void withIdToken(TokenAction action) {

        if (authTokenProvider == null) {

            appendLog(
                    "Auth error: Firebase is not initialized. " +
                            "Add app/google-services.json."
            );

            return;
        }

        authTokenProvider.requireIdToken(
                new FirebaseAuthTokenProvider.TokenCallback() {

                    @Override
                    public void onToken(String idToken) {
                        action.run(idToken);
                    }

                    @Override
                    public void onError(Exception exception) {

                        runOnUiThread(() ->
                                appendLog(
                                        "Auth error: " +
                                                authErrorMessage(exception)
                                ));
                    }
                });
    }

    private String authErrorMessage(Exception exception) {

        String message =
                exception.getMessage() == null
                        ? ""
                        : exception.getMessage();

        if (message.contains("CONFIGURATION_NOT_FOUND")) {

            return "Firebase Authentication is not configured. " +
                    "Enable Authentication and Anonymous sign-in " +
                    "in Firebase Console.";
        }

        if (exception instanceof FirebaseAuthException) {

            FirebaseAuthException authException =
                    (FirebaseAuthException) exception;

            if ("ERROR_OPERATION_NOT_ALLOWED"
                    .equals(authException.getErrorCode())) {

                return "Anonymous sign-in is disabled. " +
                        "Enable it in Firebase Console > " +
                        "Authentication > Sign-in method.";
            }
        }

        return message.isEmpty()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private String textOf(EditText editText) {

        return editText.getText()
                .toString()
                .trim();
    }

    private String shortId(String id) {

        if (id == null || id.isEmpty()) {
            return "-";
        }

        return id.length() <= 6
                ? id
                : id.substring(0, 6);
    }

    private interface TokenAction {
        void run(String idToken);
    }
}