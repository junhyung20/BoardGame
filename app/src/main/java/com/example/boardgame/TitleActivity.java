package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TitleActivity extends AppCompatActivity {
    private TextView nicknameBadge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_title);

        nicknameBadge = findViewById(R.id.nicknameBadge);
        Button joinGameButton = findViewById(R.id.joinGameButton);
        Button optionsButton = findViewById(R.id.optionsButton);
        Button rulesButton = findViewById(R.id.rulesButton);

        joinGameButton.setOnClickListener(view -> onJoinGameClicked());
        optionsButton.setOnClickListener(view -> startActivity(new Intent(this, OptionsActivity.class)));
        rulesButton.setOnClickListener(view -> showRulesPagerDialog());
        nicknameBadge.setOnClickListener(view -> startActivity(new Intent(this, NicknameActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderNickname();
    }

    private void onJoinGameClicked() {
        String nickname = SessionPrefs.getNickname(this);
        if (nickname == null || nickname.trim().isEmpty()) {
            Intent intent = new Intent(this, NicknameActivity.class);
            intent.putExtra(NicknameActivity.EXTRA_OPEN_LOBBY_AFTER_LOGIN, true);
            startActivity(intent);
            return;
        }

        startActivity(new Intent(this, LobbyListActivity.class));
    }

    private void renderNickname() {
        String nickname = SessionPrefs.getNickname(this);
        if (nickname == null || nickname.trim().isEmpty()) {
            nicknameBadge.setVisibility(View.GONE);
            return;
        }
        nicknameBadge.setVisibility(View.VISIBLE);
        nicknameBadge.setText(getString(R.string.title_nickname_format, nickname));
    }

    private void showRulesPagerDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_rules_pager, null);
        TextView pageText = dialogView.findViewById(R.id.rulesPageText);
        TextView pageIndexText = dialogView.findViewById(R.id.rulesPageIndexText);
        ImageButton prevButton = dialogView.findViewById(R.id.rulesPrevButton);
        ImageButton nextButton = dialogView.findViewById(R.id.rulesNextButton);
        String[] pages = getResources().getStringArray(R.array.title_rules_pages_v2);
        int[] index = {0};

        Runnable renderPage = () -> {
            pageText.setText(pages[index[0]]);
            pageIndexText.setText((index[0] + 1) + " / " + pages.length);
            prevButton.setEnabled(index[0] > 0);
            nextButton.setEnabled(index[0] < pages.length - 1);
            prevButton.setAlpha(prevButton.isEnabled() ? 1.0f : 0.35f);
            nextButton.setAlpha(nextButton.isEnabled() ? 1.0f : 0.35f);
        };

        prevButton.setOnClickListener(view -> {
            if (index[0] > 0) {
                index[0] -= 1;
                renderPage.run();
            }
        });
        nextButton.setOnClickListener(view -> {
            if (index[0] < pages.length - 1) {
                index[0] += 1;
                renderPage.run();
            }
        });

        renderPage.run();
        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(true)
                .show();
    }
}
