package com.example.boardgame;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class BoardActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYERS = "extra_players";

    private static final int BOARD_SIZE = 16;
    private static final int MAX_PLAYERS = 4;
    private static final int MICRO_GAME_SUCCESS_SCORE = 5;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final int[] tileIds = {
            R.id.tile0, R.id.tile1, R.id.tile2, R.id.tile3, R.id.tile4,
            R.id.tile5, R.id.tile6, R.id.tile7, R.id.tile8, R.id.tile9,
            R.id.tile10, R.id.tile11, R.id.tile12, R.id.tile13, R.id.tile14, R.id.tile15
    };
    private final ImageView[] playerViews = new ImageView[MAX_PLAYERS];
    private final TextView[] scorePanels = new TextView[MAX_PLAYERS];

    private TextView txtDiceVisual;
    private TextView txtTurnInfo;
    private Button btnDice;
    private ActivityResultLauncher<Intent> diceLauncher;
    private ActivityResultLauncher<Intent> microGameLauncher;
    private ActivityResultLauncher<Intent> miniGameLauncher;

    private String appliedTileKey = "";
    private String launchedMicroGameKey = "";
    private int requestedMiniGameRound = 0;
    private int launchedMiniGameRound = 0;
    private int submittedMiniGameRound = 0;
    private boolean finalDialogShown = false;

    private final ServerSession.Listener serverListener = new ServerSession.Listener() {
        @Override
        public void onRoomUpdated(RoomSnapshot room) {
            renderBoard();
        }

        @Override
        public void onGameUpdated(GameSnapshot game) {
            renderBoard();
            driveGamePhase();
        }

        @Override
        public void onServerError(String errorCode, String details) {
            GameSnapshot game = ServerSession.getLatestGameSnapshot();
            if (game != null
                    && "MINI_GAME_RUNNING".equals(game.getTurnPhase())
                    && isHost()
                    && details != null
                    && details.contains("Mini game is still accepting scores")) {
                scheduleFinishMiniGame();
                return;
            }
            Toast.makeText(BoardActivity.this, details, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerGameLaunchers();
        applyImmersiveSystemBars();
        setContentView(R.layout.activity_board);

        txtDiceVisual = findViewById(R.id.txtDiceVisual);
        txtTurnInfo = findViewById(R.id.txtTurnInfo);
        btnDice = findViewById(R.id.btnDice);
        playerViews[0] = findViewById(R.id.player1);
        playerViews[1] = findViewById(R.id.player2);
        playerViews[2] = findViewById(R.id.player3);
        playerViews[3] = findViewById(R.id.player4);

        createScorePanels();
        btnDice.setOnClickListener(view -> {
            btnDice.setEnabled(false);
            diceLauncher.launch(new Intent(this, DiceActivity.class));
        });

        renderBoard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServerSession.addListener(serverListener);
        ServerSession.connect(this);
        renderBoard();
        driveGamePhase();
    }

    @Override
    protected void onStop() {
        ServerSession.removeListener(serverListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyImmersiveSystemBars();
        }
    }

    private void registerGameLaunchers() {
        diceLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            GameSnapshot game = ServerSession.getLatestGameSnapshot();
            if (game != null && isMyTurn(game) && "WAITING_FOR_ROLL".equals(game.getTurnPhase())) {
                int diceRoll = 0;
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    diceRoll = result.getData().getIntExtra(GameContract.EXTRA_DICE_RESULT, 0);
                }
                ServerSession.rollDice(diceRoll);
            } else {
                renderBoard();
            }
        });

        microGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            boolean success = result.getResultCode() == RESULT_OK
                    && result.getData() != null
                    && result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
            ServerSession.submitMicroGameScore(success ? MICRO_GAME_SUCCESS_SCORE : 0);
        });

        miniGameLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            GameSnapshot game = ServerSession.getLatestGameSnapshot();
            if (game == null || submittedMiniGameRound == game.getCurrentRound()) {
                return;
            }
            int progress = 0;
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                boolean success = result.getData().getBooleanExtra(GameContract.EXTRA_SUCCESS, false);
                progress = result.getData().getIntExtra(GameContract.EXTRA_PROGRESS, success ? 100 : 0);
            }
            submittedMiniGameRound = game.getCurrentRound();
            ServerSession.submitMiniGameScore(progress);
            if (isHost()) {
                scheduleFinishMiniGame();
            }
        });
    }

    private void renderBoard() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (room == null) {
            txtTurnInfo.setText("게임 정보를 기다리는 중");
            btnDice.setEnabled(false);
            return;
        }

        List<PlayerSnapshot> players = room.getPlayers();
        for (int i = 0; i < MAX_PLAYERS; i++) {
            boolean visible = i < players.size();
            playerViews[i].setVisibility(visible ? View.VISIBLE : View.GONE);
            scorePanels[i].setVisibility(visible ? View.VISIBLE : View.GONE);
            if (visible) {
                PlayerSnapshot player = players.get(i);
                movePlayerToTile(playerViews[i], player.getPosition(), i);
                renderScorePanel(scorePanels[i], player, i, isCurrentPlayer(game, player.getId()));
            }
        }

        if (game == null) {
            txtDiceVisual.setText("?");
            txtTurnInfo.setText("게임 시작 대기 중");
            btnDice.setEnabled(false);
            return;
        }

        txtDiceVisual.setText(game.getLastDiceRoll() > 0 ? String.valueOf(game.getLastDiceRoll()) : "?");
        txtTurnInfo.setText(turnText(room, game));
        btnDice.setEnabled(isMyTurn(game) && "WAITING_FOR_ROLL".equals(game.getTurnPhase()));

        if ("FINISHED".equals(game.getTurnPhase())) {
            btnDice.setEnabled(false);
            showFinalRanking(room);
        }
    }

    private void driveGamePhase() {
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (game == null) {
            return;
        }

        String phase = game.getTurnPhase();
        if ("WAITING_FOR_TILE_EFFECT".equals(phase) && isMyTurn(game)) {
            String key = game.getCurrentRound() + ":" + game.getCurrentPlayerId() + ":" + game.getLastDiceRoll();
            if (!key.equals(appliedTileKey)) {
                appliedTileKey = key;
                handler.postDelayed(ServerSession::applyTileEffect, 650L);
            }
            return;
        }

        if ("WAITING_FOR_MICRO_GAME".equals(phase) && isMyTurn(game)) {
            String key = game.getCurrentRound() + ":" + game.getCurrentPlayerId() + ":" + game.getLastDiceRoll();
            if (!key.equals(launchedMicroGameKey)) {
                launchedMicroGameKey = key;
                microGameLauncher.launch(randomMicroGameIntent());
            }
            return;
        }

        if ("WAITING_FOR_MINI_GAME".equals(phase) && isHost()
                && requestedMiniGameRound != game.getCurrentRound()) {
            requestedMiniGameRound = game.getCurrentRound();
            ServerSession.startMiniGame(miniGameType(game.getCurrentRound()));
            return;
        }

        if ("MINI_GAME_RUNNING".equals(phase)
                && launchedMiniGameRound != game.getCurrentRound()) {
            launchedMiniGameRound = game.getCurrentRound();
            miniGameLauncher.launch(miniGameIntent(game.getCurrentRound()));
        }
    }

    private void createScorePanels() {
        ViewGroup content = findViewById(android.R.id.content);
        ConstraintLayout rootLayout = (ConstraintLayout) content.getChildAt(0);

        for (int i = 0; i < scorePanels.length; i++) {
            TextView panel = new TextView(this);
            panel.setId(View.generateViewId());
            panel.setGravity(Gravity.CENTER);
            panel.setPadding(dp(8), dp(8), dp(8), dp(8));
            panel.setTextSize(16);
            panel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            panel.setTextColor(i == 3 ? Color.rgb(17, 17, 17) : Color.WHITE);
            panel.setBackground(createScorePanelBackground(i, false));

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(dp(142), dp(72));
            if (i == 0 || i == 2) {
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
            }
            if (i == 0 || i == 1) {
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            } else {
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            }

            rootLayout.addView(panel, params);
            scorePanels[i] = panel;
        }
    }

    private void renderScorePanel(TextView panel, PlayerSnapshot player, int index, boolean active) {
        String turnLabel = active ? "  턴" : "";
        panel.setBackground(createScorePanelBackground(index, active));
        panel.setElevation(active ? dp(10) : dp(2));
        panel.setAlpha(active ? 1.0f : 0.86f);
        panel.setText(player.getNickname() + turnLabel + "\n" + player.getScore() + "점");
    }

    private GradientDrawable createScorePanelBackground(int playerIndex, boolean active) {
        int[] fills = {
                Color.rgb(216, 67, 67),
                Color.rgb(47, 111, 222),
                Color.rgb(46, 157, 98),
                Color.rgb(242, 201, 76)
        };
        int[] borders = {
                Color.rgb(122, 23, 23),
                Color.rgb(24, 59, 122),
                Color.rgb(20, 83, 52),
                Color.rgb(138, 101, 8)
        };

        int safeIndex = Math.max(0, Math.min(playerIndex, fills.length - 1));
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fills[safeIndex]);
        drawable.setCornerRadius(dp(8));
        drawable.setStroke(active ? dp(5) : dp(2), active ? Color.WHITE : borders[safeIndex]);
        return drawable;
    }

    private void movePlayerToTile(View player, int targetTileIndex, int playerIndex) {
        int safeTileIndex = Math.floorMod(targetTileIndex, BOARD_SIZE);
        View targetTile = findViewById(tileIds[safeTileIndex]);
        targetTile.post(() -> {
            float offsetX = (playerIndex % 2 == 0 ? -10f : 10f) + playerIndex * 3f;
            float offsetY = (playerIndex < 2 ? -10f : 10f);
            float targetX = targetTile.getX() + (targetTile.getWidth() / 2f) - (player.getWidth() / 2f) + offsetX;
            float targetY = targetTile.getY() + (targetTile.getHeight() / 2f) - (player.getHeight() / 2f) + offsetY;
            player.animate().x(targetX).y(targetY).setDuration(350L).start();
        });
    }

    private Intent randomMicroGameIntent() {
        int gameIndex = random.nextInt(3);
        if (gameIndex == 0) {
            return new Intent(this, AdGame1Activity.class);
        }
        if (gameIndex == 1) {
            return new Intent(this, AdGame2Activity.class);
        }
        return new Intent(this, AdGame3Activity.class);
    }

    private Intent miniGameIntent(int round) {
        Intent intent;
        if (round == GameContract.ROUND_CAPTCHA) {
            intent = new Intent(this, CaptchaActivity.class);
        } else if (round == GameContract.ROUND_PASSWORD) {
            intent = new Intent(this, PasswordActivity.class);
        } else {
            intent = new Intent(this, VolumeMazeActivity.class);
        }
        intent.putExtra(GameContract.EXTRA_ROUND, round);
        intent.putExtra(GameContract.EXTRA_PLAYER_INDEX, playerIndexOf(ServerSession.getCurrentPlayerId()));
        return intent;
    }

    private String miniGameType(int round) {
        if (round == GameContract.ROUND_CAPTCHA) {
            return "CAPTCHA";
        }
        if (round == GameContract.ROUND_PASSWORD) {
            return "PASSWORD";
        }
        return "VOLUME_MAZE";
    }

    private void scheduleFinishMiniGame() {
        handler.postDelayed(ServerSession::finishMiniGame, 3000L);
    }

    private String turnText(RoomSnapshot room, GameSnapshot game) {
        PlayerSnapshot current = findPlayer(room, game.getCurrentPlayerId());
        String name = current == null ? "알 수 없음" : current.getNickname();
        String message = game.getLastSystemMessage();
        if (message == null || message.trim().isEmpty()) {
            message = game.getTurnPhase();
        }
        return "라운드 " + game.getCurrentRound() + "/" + game.getFinalRound()
                + " | 현재 턴 " + name + "\n" + message;
    }

    private boolean isMyTurn(GameSnapshot game) {
        return game != null && game.getCurrentPlayerId().equals(ServerSession.getCurrentPlayerId());
    }

    private boolean isCurrentPlayer(GameSnapshot game, String playerId) {
        return game != null && game.getCurrentPlayerId().equals(playerId);
    }

    private boolean isHost() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        return room != null && room.getHostPlayerId().equals(ServerSession.getCurrentPlayerId());
    }

    private PlayerSnapshot findPlayer(RoomSnapshot room, String playerId) {
        if (room == null) {
            return null;
        }
        for (PlayerSnapshot player : room.getPlayers()) {
            if (player.getId().equals(playerId)) {
                return player;
            }
        }
        return null;
    }

    private int playerIndexOf(String playerId) {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room == null) {
            return 0;
        }
        for (int i = 0; i < room.getPlayers().size(); i++) {
            if (room.getPlayers().get(i).getId().equals(playerId)) {
                return i;
            }
        }
        return 0;
    }

    private void showFinalRanking(RoomSnapshot room) {
        if (finalDialogShown || room == null) {
            return;
        }
        finalDialogShown = true;
        List<PlayerSnapshot> ranking = new ArrayList<>(room.getPlayers());
        ranking.sort(Comparator.comparingInt(PlayerSnapshot::getScore).reversed());

        StringBuilder message = new StringBuilder();
        for (int i = 0; i < ranking.size(); i++) {
            PlayerSnapshot player = ranking.get(i);
            message.append(i + 1)
                    .append("위 ")
                    .append(player.getNickname())
                    .append(" - ")
                    .append(player.getScore())
                    .append("점\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("최종 순위")
                .setMessage(message.toString())
                .setPositiveButton("확인", null)
                .setCancelable(false)
                .show();
    }

    private void applyImmersiveSystemBars() {
        Window window = getWindow();
        window.setNavigationBarColor(Color.TRANSPARENT);
        window.setStatusBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
        }

        View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
