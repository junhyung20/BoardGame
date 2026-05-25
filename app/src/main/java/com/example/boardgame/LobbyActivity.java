package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;

import java.util.ArrayList;
import java.util.List;

public class LobbyActivity extends AppCompatActivity {
    public static final String EXTRA_MY_NICKNAME = "extra_my_nickname";
    public static final String EXTRA_ROOM_CODE = "extra_room_code";
    public static final String EXTRA_LEFT_ROOM_CODE = "extra_left_room_code";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView roomCodeText;
    private TextView roomMetaText;
    private TextView startHintText;
    private Button startButton;
    private Button leaveButton;
    private String roomCode = "";
    private String myNickname = "";
    private boolean navigatingToBoard = false;
    private boolean leavingLobby = false;
    private Runnable pendingStartRunnable;

    private TextView[] slotNameViews;
    private TextView[] slotStateViews;
    private View[] slotCards;

    private final ServerSession.Listener serverListener = new ServerSession.Listener() {
        @Override
        public void onConnectionStateChanged(ConnectionState state) {
            if (state == ConnectionState.DISCONNECTED && !leavingLobby && !navigatingToBoard) {
                Toast.makeText(LobbyActivity.this, "Server disconnected.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onRoomUpdated(RoomSnapshot room) {
            if (room.getCode().equalsIgnoreCase(roomCode)) {
                renderRoomSnapshot(room);
            }
        }

        @Override
        public void onGameUpdated(GameSnapshot game) {
            if (!game.getRoomCode().equalsIgnoreCase(roomCode) || navigatingToBoard) {
                return;
            }
            moveToBoard(playersFromLatestRoom());
        }

        @Override
        public void onServerError(String errorCode, String details) {
            if (!leavingLobby && !navigatingToBoard) {
                Toast.makeText(LobbyActivity.this, details, Toast.LENGTH_SHORT).show();
            }
            if (pendingStartRunnable != null) {
                handler.removeCallbacks(pendingStartRunnable);
                pendingStartRunnable = null;
            }
            RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
            if (room != null && room.getCode().equalsIgnoreCase(roomCode)) {
                renderRoomSnapshot(room);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby);

        roomCodeText = findViewById(R.id.roomCodeText);
        roomMetaText = findViewById(R.id.roomMetaText);
        startHintText = findViewById(R.id.startHintText);
        startButton = findViewById(R.id.startGameButton);
        leaveButton = findViewById(R.id.leaveRoomButton);
        slotNameViews = new TextView[]{
                findViewById(R.id.slot1Name),
                findViewById(R.id.slot2Name),
                findViewById(R.id.slot3Name),
                findViewById(R.id.slot4Name)
        };
        slotStateViews = new TextView[]{
                findViewById(R.id.slot1State),
                findViewById(R.id.slot2State),
                findViewById(R.id.slot3State),
                findViewById(R.id.slot4State)
        };
        slotCards = new View[]{
                (View) slotNameViews[0].getParent(),
                (View) slotNameViews[1].getParent(),
                (View) slotNameViews[2].getParent(),
                (View) slotNameViews[3].getParent()
        };

        myNickname = getIntent().getStringExtra(EXTRA_MY_NICKNAME);
        if (myNickname == null || myNickname.trim().isEmpty()) {
            myNickname = SessionPrefs.getNickname(this);
        }
        if (myNickname == null || myNickname.trim().isEmpty()) {
            myNickname = "Player";
        }
        roomCode = getIntent().getStringExtra(EXTRA_ROOM_CODE);
        if (roomCode == null) {
            roomCode = "";
        }

        roomCodeText.setText(getString(R.string.lobby_room_code, roomCode));
        startButton.setOnClickListener(view -> requestStartGame());
        leaveButton.setOnClickListener(view -> leaveLobbyAndFinish());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                leaveLobbyAndFinish();
            }
        });

        RoomSnapshot latestRoom = ServerSession.getLatestRoomSnapshot();
        if (latestRoom != null && latestRoom.getCode().equalsIgnoreCase(roomCode)) {
            renderRoomSnapshot(latestRoom);
        } else {
            renderEmptyRoom();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServerSession.addListener(serverListener);
        ServerSession.connect(this);
    }

    @Override
    protected void onStop() {
        ServerSession.removeListener(serverListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        if (pendingStartRunnable != null) {
            handler.removeCallbacks(pendingStartRunnable);
            pendingStartRunnable = null;
        }
        super.onDestroy();
    }

    private void requestStartGame() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room == null || !room.getCode().equalsIgnoreCase(roomCode)) {
            return;
        }
        PlayerSnapshot me = findMe(room);
        if (me == null || !me.isHost() || !"READY".equals(room.getStatus())) {
            return;
        }
        updateStartButtonState(true, room);
        if (pendingStartRunnable != null) {
            handler.removeCallbacks(pendingStartRunnable);
        }
        pendingStartRunnable = ServerSession::startGame;
        handler.postDelayed(pendingStartRunnable, 700L);
    }

    private void renderRoomSnapshot(RoomSnapshot room) {
        List<PlayerSlot> slots = new ArrayList<>();
        for (PlayerSnapshot player : room.getPlayers()) {
            slots.add(new PlayerSlot(player.getNickname(), player.isHost(), player.isReady(), player.getId()));
        }
        while (slots.size() < 4) {
            slots.add(PlayerSlot.empty());
        }
        renderSlots(slots);
        updateRoomMeta(room);
        updateStartButtonState(false, room);
    }

    private void renderEmptyRoom() {
        List<PlayerSlot> slots = new ArrayList<>();
        while (slots.size() < 4) {
            slots.add(PlayerSlot.empty());
        }
        renderSlots(slots);
        roomMetaText.setText(getString(R.string.lobby_room_meta, 0, 4, 0));
        startButton.setEnabled(false);
        startButton.setBackgroundResource(R.drawable.bg_action_secondary);
    }

    private void renderSlots(List<PlayerSlot> slots) {
        for (int i = 0; i < slotNameViews.length; i++) {
            PlayerSlot slot = slots.get(i);
            TextView nameView = slotNameViews[i];
            TextView stateView = slotStateViews[i];
            View cardView = slotCards[i];

            if (slot.nickname.isEmpty()) {
                nameView.setText(getString(R.string.lobby_slot_waiting_name));
            } else if (slot.host) {
                nameView.setText(getString(R.string.lobby_slot_host_format, slot.nickname));
            } else {
                nameView.setText(slot.nickname);
            }

            stateView.setText(getString(slot.ready ? R.string.lobby_status_ready : R.string.lobby_status_waiting));
            stateView.setBackgroundResource(slot.ready ? R.drawable.bg_status_ready : R.drawable.bg_status_waiting);
            if (!slot.playerId.isEmpty() && slot.playerId.equals(ServerSession.getCurrentPlayerId())) {
                stateView.setOnClickListener(view -> toggleMyReady());
            } else {
                stateView.setOnClickListener(null);
            }

            cardView.setAlpha(0.80f);
            cardView.setTranslationY(10f);
            cardView.animate().alpha(1.0f).translationY(0f).setDuration(220L).start();
        }
    }

    private void updateRoomMeta(RoomSnapshot room) {
        int readyCount = 0;
        for (PlayerSnapshot player : room.getPlayers()) {
            if (player.isReady()) {
                readyCount += 1;
            }
        }
        roomMetaText.setText(getString(R.string.lobby_room_meta, room.getPlayers().size(), 4, readyCount));
    }

    private void updateStartButtonState(boolean starting, RoomSnapshot room) {
        PlayerSnapshot me = findMe(room);
        boolean canStart = me != null && me.isHost() && "READY".equals(room.getStatus());
        if (starting) {
            startButton.setEnabled(false);
            startButton.setText(getString(R.string.lobby_start_loading));
            startHintText.setText(getString(R.string.lobby_start_hint_loading));
            startButton.setBackgroundResource(R.drawable.bg_action_secondary);
            return;
        }
        startButton.setEnabled(canStart);
        startButton.setText(getString(R.string.lobby_start_button));
        startButton.setBackgroundResource(canStart ? R.drawable.bg_action_primary : R.drawable.bg_action_secondary);
        if (canStart) {
            startHintText.setText(getString(R.string.lobby_start_hint_ready));
        } else if (me != null && me.isHost()) {
            startHintText.setText(getString(R.string.lobby_start_hint_need_all_ready));
        } else {
            startHintText.setText(getString(R.string.lobby_start_hint_host_only));
        }
    }

    private void toggleMyReady() {
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room == null || !room.getCode().equalsIgnoreCase(roomCode)) {
            return;
        }
        PlayerSnapshot me = findMe(room);
        if (me != null) {
            ServerSession.setReady(!me.isReady());
        }
    }

    private PlayerSnapshot findMe(RoomSnapshot room) {
        String currentPlayerId = ServerSession.getCurrentPlayerId();
        for (PlayerSnapshot player : room.getPlayers()) {
            if (player.getId().equals(currentPlayerId)) {
                return player;
            }
        }
        return null;
    }

    private ArrayList<String> playersFromLatestRoom() {
        ArrayList<String> players = new ArrayList<>();
        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room == null) {
            return players;
        }
        for (PlayerSnapshot player : room.getPlayers()) {
            players.add(player.getNickname());
        }
        return players;
    }

    private void moveToBoard(ArrayList<String> players) {
        navigatingToBoard = true;
        Intent intent = new Intent(this, BoardActivity.class);
        intent.putStringArrayListExtra(BoardActivity.EXTRA_PLAYERS, players);
        startActivity(intent);
        finish();
    }

    private void leaveLobbyAndFinish() {
        if (leavingLobby || navigatingToBoard) {
            return;
        }
        leavingLobby = true;
        ServerSession.leaveRoom();
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_LEFT_ROOM_CODE, roomCode));
        finish();
    }

    private static class PlayerSlot {
        final String nickname;
        final boolean host;
        final boolean ready;
        final String playerId;

        PlayerSlot(String nickname, boolean host, boolean ready, String playerId) {
            this.nickname = nickname == null ? "" : nickname;
            this.host = host;
            this.ready = ready;
            this.playerId = playerId == null ? "" : playerId;
        }

        static PlayerSlot empty() {
            return new PlayerSlot("", false, false, "");
        }
    }
}
