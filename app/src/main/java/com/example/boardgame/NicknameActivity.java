package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class NicknameActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_LOBBY_AFTER_LOGIN = "extra_open_lobby_after_login";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nickname);

        EditText nicknameInput = findViewById(R.id.nicknameInput);
        TextView titleView = findViewById(R.id.nicknameTitle);
        TextView guideView = findViewById(R.id.nicknameGuide);
        Button saveButton = findViewById(R.id.saveNicknameButton);
        String existingNickname = SessionPrefs.getNickname(this);
        boolean openLobbyAfterLogin = getIntent().getBooleanExtra(EXTRA_OPEN_LOBBY_AFTER_LOGIN, false);
        nicknameInput.setText(existingNickname);
        if (!openLobbyAfterLogin && existingNickname != null && !existingNickname.trim().isEmpty()) {
            titleView.setText(R.string.nickname_edit_title);
            guideView.setText(R.string.nickname_edit_guide);
            saveButton.setText(R.string.nickname_update_button);
        }
        saveButton.setOnClickListener(view -> {
            String nickname = nicknameInput.getText().toString().trim();
            if (nickname.isEmpty()) {
                nicknameInput.setError(getString(R.string.error_nickname_required));
                return;
            }
            SessionPrefs.setGuestLogin(this, nickname);
            if (openLobbyAfterLogin) {
                startActivity(new Intent(this, LobbyListActivity.class));
            }
            finish();
        });
    }
}
