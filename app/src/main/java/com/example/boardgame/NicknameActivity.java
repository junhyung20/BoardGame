package com.example.boardgame;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class NicknameActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nickname);

        EditText nicknameInput = findViewById(R.id.nicknameInput);
        Button saveButton = findViewById(R.id.saveNicknameButton);
        nicknameInput.setText(SessionPrefs.getNickname(this));
        saveButton.setOnClickListener(view -> {
            String nickname = nicknameInput.getText().toString().trim();
            if (nickname.isEmpty()) {
                nicknameInput.setError(getString(R.string.error_nickname_required));
                return;
            }
            SessionPrefs.setNickname(this, nickname);
            finish();
        });
    }
}
