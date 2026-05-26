package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
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
    private TextView emptyLobbyText;
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
        Button backTitleButton = findViewById(R.id.backToTitleButton);
        Button joinByCodeButton = findViewById(R.id.joinByCodeButton);
        emptyLobbyText = findViewById(R.id.emptyLobbyText);

        myNickname = getNicknameOrDefault();
        roomAdapter = new RoomListAdapter(this::showRoomJoinDialog);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(roomAdapter);

        createRoomButton.setOnClickListener(view -> showCreateRoomDialog());
        joinByCodeButton.setOnClickListener(view -> showJoinByCodeDialog());
        backTitleButton.setOnClickListener(view -> finish());

        renderLobby(ServerSession.getLatestLobbySnapshot());
    }

    @Override
    protected void onStart() {
        super.onStart();
        openingRoom = false;
        ServerSession.addListener(serverListener);
        ServerSession.connect(this);
        if (!ServerSession.getCurrentRoomCode().trim().isEmpty()) {
            ServerSession.leaveRoom();
        }
        renderLobby(ServerSession.getLatestLobbySnapshot());
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

    private void joinRoom(String roomCode, String password) {
        pendingAction = PendingAction.JOIN_ROOM;
        pendingRoomCode = roomCode;
        if (!ServerSession.joinRoom(this, roomCode, myNickname, password)) {
            pendingAction = PendingAction.NONE;
            pendingRoomCode = "";
            return;
        }
        Toast.makeText(this, "Joining room...", Toast.LENGTH_SHORT).show();
    }

    private void createRoom(String password) {
        pendingAction = PendingAction.CREATE_ROOM;
        pendingRoomCode = "";
        if (!ServerSession.createRoom(this, myNickname, password)) {
            pendingAction = PendingAction.NONE;
            return;
        }
        Toast.makeText(this, "Creating room...", Toast.LENGTH_SHORT).show();
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
        emptyLobbyText.setVisibility(rooms.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private String getNicknameOrDefault() {
        String nickname = SessionPrefs.getNickname(this);
        return nickname == null || nickname.trim().isEmpty() ? "Player" : nickname.trim();
    }

    private void showCreateRoomDialog() {
        EditText passwordInput = newDialogEditText(getString(R.string.lobby_list_password_hint));
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        new AlertDialog.Builder(this)
                .setTitle(R.string.lobby_list_create_dialog_title)
                .setView(dialogContainer(passwordInput))
                .setNegativeButton(R.string.lobby_list_dialog_cancel, null)
                .setPositiveButton(R.string.lobby_list_dialog_create,
                        (dialog, which) -> createRoom(passwordInput.getText().toString()))
                .show();
    }

    private void showJoinByCodeDialog() {
        EditText codeInput = newDialogEditText(getString(R.string.lobby_list_code_hint));
        codeInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        EditText passwordInput = newDialogEditText(getString(R.string.lobby_list_password_hint));
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = dialogContainer(codeInput, passwordInput);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.lobby_list_join_dialog_title)
                .setView(container)
                .setNegativeButton(R.string.lobby_list_dialog_cancel, null)
                .setPositiveButton(R.string.lobby_list_dialog_join, null)
                .create();

        dialog.setOnShowListener(dialogInterface ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                    String code = codeInput.getText().toString().trim();
                    if (code.isEmpty()) {
                        codeInput.setError(getString(R.string.lobby_list_code_required));
                        return;
                    }
                    dialog.dismiss();
                    joinRoom(code, passwordInput.getText().toString());
                }));
        dialog.show();
    }

    private void showRoomJoinDialog(DemoRoom room) {
        TextView codeText = new TextView(this);
        codeText.setText(getString(R.string.lobby_list_room_code_label, room.roomCode));
        codeText.setTextColor(getColor(R.color.waiting_text_primary));
        codeText.setTextSize(16);

        EditText passwordInput = null;
        LinearLayout container;
        if (room.hasPassword) {
            passwordInput = newDialogEditText(getString(R.string.lobby_list_password_hint));
            passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            container = dialogContainer(codeText, passwordInput);
        } else {
            container = dialogContainer(codeText);
        }

        EditText finalPasswordInput = passwordInput;
        new AlertDialog.Builder(this)
                .setTitle(R.string.lobby_list_join_dialog_title)
                .setView(container)
                .setNegativeButton(R.string.lobby_list_dialog_cancel, null)
                .setPositiveButton(R.string.lobby_list_dialog_join, (dialog, which) ->
                        joinRoom(room.roomCode, finalPasswordInput == null
                                ? ""
                                : finalPasswordInput.getText().toString()))
                .show();
    }

    private LinearLayout dialogContainer(android.view.View... children) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int horizontalPadding = dp(20);
        int verticalPadding = dp(8);
        container.setPadding(horizontalPadding, verticalPadding, horizontalPadding, 0);

        for (android.view.View child : children) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = dp(8);
            container.addView(child, params);
        }
        return container;
    }

    private EditText newDialogEditText(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(true);
        editText.setTextColor(getColor(R.color.waiting_text_primary));
        editText.setHintTextColor(getColor(R.color.waiting_text_secondary));
        return editText;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
