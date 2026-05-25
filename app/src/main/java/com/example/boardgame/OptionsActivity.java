package com.example.boardgame;

import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OptionsActivity extends AppCompatActivity {
    private TextView volumeValueText;
    private CheckBox easyVolumeCheck;
    private Switch soundEffectsSwitch;
    private Switch vibrationSwitch;
    private RadioButton diceSpeedNormal;
    private RadioButton diceSpeedFast;
    private Switch debugModeSwitch;
    private EditText serverUrlInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);

        volumeValueText = findViewById(R.id.volumeValueText);
        easyVolumeCheck = findViewById(R.id.easyVolumeCheck);
        soundEffectsSwitch = findViewById(R.id.soundEffectsSwitch);
        vibrationSwitch = findViewById(R.id.vibrationSwitch);
        diceSpeedNormal = findViewById(R.id.diceSpeedNormal);
        diceSpeedFast = findViewById(R.id.diceSpeedFast);
        debugModeSwitch = findViewById(R.id.debugModeSwitch);
        serverUrlInput = findViewById(R.id.serverUrlInput);

        Button minusButton = findViewById(R.id.volumeMinusButton);
        Button plusButton = findViewById(R.id.volumePlusButton);
        Button saveServerUrlButton = findViewById(R.id.saveServerUrlButton);
        Button useLanServerButton = findViewById(R.id.useLanServerButton);
        Button useWanServerButton = findViewById(R.id.useWanServerButton);
        Button closeButton = findViewById(R.id.closeOptionsButton);

        minusButton.setOnClickListener(view -> updateVolumeBy(-1));
        plusButton.setOnClickListener(view -> updateVolumeBy(1));
        closeButton.setOnClickListener(view -> finish());

        easyVolumeCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                buttonView.setChecked(false);
                Toast.makeText(this, R.string.options_easy_volume_locked, Toast.LENGTH_SHORT).show();
            }
        });

        soundEffectsSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                SessionPrefs.setSoundEffectsOn(this, isChecked));
        vibrationSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                SessionPrefs.setVibrationOn(this, isChecked));
        diceSpeedNormal.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                SessionPrefs.setDiceFast(this, false);
            }
        });
        diceSpeedFast.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            if (isChecked) {
                SessionPrefs.setDiceFast(this, true);
            }
        });
        debugModeSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) ->
                SessionPrefs.setDebugMode(this, isChecked));

        saveServerUrlButton.setOnClickListener(view -> saveServerUrlFromInput());
        useLanServerButton.setOnClickListener(view -> {
            ServerSession.useDefaultLan(this);
            renderServerUrl();
            Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
        });
        useWanServerButton.setOnClickListener(view -> {
            ServerSession.useDefaultWan(this);
            renderServerUrl();
            Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderVolume();
        renderExtraOptions();
        renderServerUrl();
        easyVolumeCheck.setChecked(false);
    }

    private void updateVolumeBy(int delta) {
        SessionPrefs.setVolume(this, SessionPrefs.getVolume(this) + delta);
        renderVolume();
    }

    private void renderVolume() {
        volumeValueText.setText(getString(R.string.options_volume_value, SessionPrefs.getVolume(this)));
    }

    private void renderExtraOptions() {
        soundEffectsSwitch.setChecked(SessionPrefs.isSoundEffectsOn(this));
        vibrationSwitch.setChecked(SessionPrefs.isVibrationOn(this));
        if (SessionPrefs.isDiceFast(this)) {
            diceSpeedFast.setChecked(true);
        } else {
            diceSpeedNormal.setChecked(true);
        }
        debugModeSwitch.setChecked(SessionPrefs.isDebugMode(this));
    }

    private void renderServerUrl() {
        serverUrlInput.setText(ServerSession.getServerUrl(this));
    }

    private void saveServerUrlFromInput() {
        String wsUrl = serverUrlInput.getText().toString().trim();
        if (!(wsUrl.startsWith("ws://") || wsUrl.startsWith("wss://"))) {
            serverUrlInput.setError(getString(R.string.options_server_url_invalid));
            return;
        }
        String normalizedUrl = ServerSession.normalizeServerUrl(wsUrl);
        ServerSession.setServerUrl(this, normalizedUrl);
        serverUrlInput.setText(normalizedUrl);
        Toast.makeText(this, R.string.options_server_url_saved, Toast.LENGTH_SHORT).show();
    }
}
