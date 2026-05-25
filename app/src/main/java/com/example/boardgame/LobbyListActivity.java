package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.boardgame.socket.protocol.ConnectionState;
import com.example.boardgame.socket.protocol.LobbySnapshot;

import java.util.ArrayList;
import java.util.List;

public class LobbyListActivity extends AppCompatActivity {
    private static final int MAX_PLAYERS = 4;

    private enum PendingAction {
        NONE,
        CREATE_ROOM,
        JOIN_ROOM
    }

    private RoomListAdapter roomAdapter;
    private EditText roomCodeInput;
    private EditText roomPasswordInput;
    private String myNickname = "";
    private String pendingRoomCode = "";
    private PendingAction pendingAction = PendingAction.NONE;
    private boolean openingRoom = false;

    private final ServerSession.Listener serverListener = new ServerSession.Listener() {
        @Override
        public void onConnectionStateChanged(ConnectionState state) {
            if (state == ConnectionState.DISCONNECTED) {
                Toast.makeText(LobbyListActivity.this, "Server disconnected.", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLobbyUpdated(LobbySnapshot lobby) {
            renderLobby(lobby);
        }

        @Override
        public void onRequestOk(String commandType, String roomCode, String playerId) {
            if (pendingAction == PendingAction.NONE || roomCode == null || roomCode.trim().isEmpty()) {
                return;
            }
            if (pendingAction == PendingAction.JOIN_ROOM
                    && !pendingRoomCode.trim().isEmpty()
                    && !pendingRoomCode.equalsIgnoreCase(roomCode)) {
                return;
            }

            pendingAction = PendingAction.NONE;
            pendingRoomCode = "";
            openRoom(roomCode);
        }

        @Override
        public void onServerError(String errorCode, String details) {
            pendingAction = PendingAction.NONE;
            pendingRoomCode = "";
            Toast.makeText(LobbyListActivity.this, details, Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_list);

        RecyclerView recyclerView = findViewById(R.id.roomRecyclerView);
        Button createRoomButton = findViewById(R.id.createRoomButton);
        Button refreshButton = findViewById(R.id.refreshButton);
        Button backTitleButton = findViewById(R.id.backToTitleButton);
        roomCodeInput = findViewById(R.id.roomCodeInput);
        roomPasswordInput = findViewById(R.id.roomPasswordInput);
        Button joinByCodeButton = findViewById(R.id.joinByCodeButton);

        myNickname = getNicknameOrDefault();
        roomAdapter = new RoomListAdapter(room -> {
            roomCodeInput.setText(room.roomCode);
            joinRoom(room.roomCode);
        });
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(roomAdapter);

        createRoomButton.setOnClickListener(view -> {
            pendingAction = PendingAction.CREATE_ROOM;
            pendingRoomCode = "";
            ServerSession.createRoom(this, myNickname, password());
            Toast.makeText(this, "Creating room...", Toast.LENGTH_SHORT).show();
        });
        refreshButton.setOnClickListener(view -> {
            ServerSession.connect(this);
            renderLobby(ServerSession.getLatestLobbySnapshot());
        });
        joinByCodeButton.setOnClickListener(view -> {
            String code = roomCodeInput.getText().toString().trim();
            if (code.isEmpty()) {
                roomCodeInput.setError(getString(R.string.lobby_list_code_required));
                return;
            }
            joinRoom(code);
        });
        backTitleButton.setOnClickListener(view -> finish());

        renderLobby(ServerSession.getLatestLobbySnapshot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        openingRoom = false;
        ServerSession.addListener(serverListener);
        ServerSession.connect(this);
    }

    @Override
    protected void onStop() {
        ServerSession.removeListener(serverListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        myNickname = getNicknameOrDefault();
    }

    private void joinRoom(String roomCode) {
        pendingAction = PendingAction.JOIN_ROOM;
        pendingRoomCode = roomCode;
        ServerSession.joinRoom(this, roomCode, myNickname, password());
        Toast.makeText(this, "Joining room...", Toast.LENGTH_SHORT).show();
    }

    private void openRoom(String roomCode) {
        if (openingRoom) {
            return;
        }
        openingRoom = true;
        Intent intent = new Intent(this, LobbyActivity.class);
        intent.putExtra(LobbyActivity.EXTRA_MY_NICKNAME, myNickname);
        intent.putExtra(LobbyActivity.EXTRA_ROOM_CODE, roomCode);
        startActivity(intent);
    }

    private void renderLobby(LobbySnapshot lobby) {
        List<DemoRoom> rooms = new ArrayList<>();
        if (lobby != null) {
            for (LobbySnapshot.RoomListInfo room : lobby.getRooms()) {
                rooms.add(new DemoRoom(
                        room.getCode(),
                        room.getCode(),
                        room.getPlayerCount(),
                        MAX_PLAYERS,
                        room.getHostNickname(),
                        room.getStatus(),
                        room.hasPassword()
                ));
            }
        }
        roomAdapter.submit(rooms);
    }

    private String password() {
        return roomPasswordInput.getText().toString();
    }

    private String getNicknameOrDefault() {
        String nickname = SessionPrefs.getNickname(this);
        return nickname == null || nickname.trim().isEmpty() ? "Player" : nickname.trim();
    }
}
