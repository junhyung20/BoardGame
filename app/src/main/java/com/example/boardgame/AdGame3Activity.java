package com.example.boardgame;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.Locale;

public class AdGame3Activity extends AppCompatActivity {

    private ConstraintLayout layoutAdContent;
    private LinearLayout layoutCountdown;
    private CardView cardAdResultPopup;
    private TextView stopwatchText, countdownText, clickCounterText, txtAdRemovalTag;
    private TextView adResultStatus, adResultScore, adResultClicks;
    private Button btnTap;
    private View clickGaugeFill;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;
    private long elapsedTime = 0L;
    private boolean isRunning = false;

    private CountDownTimer stageCountdownTimer;

    private int currentClicks = 0;
    private boolean isReversed = false;
    private int currentPhase = 0;

    private final int TARGET_CLICKS = 30;
    private final long MAX_TIME_MS = 10000;

    private final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private final int COLOR_RED = Color.parseColor("#D32F2F");
    private final int COLOR_YELLOW = Color.parseColor("#FFCA28");
    private final int COLOR_GRAY = Color.parseColor("#757575");
    private final int COLOR_TAG_GREEN = Color.parseColor("#388E3C");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_game3);
        blockBackDuringGame();

        layoutAdContent = findViewById(R.id.layoutAdContent);
        layoutCountdown = findViewById(R.id.layoutCountdown);
        cardAdResultPopup = findViewById(R.id.cardAdResultPopup);

        stopwatchText = findViewById(R.id.stopwatchText);
        countdownText = findViewById(R.id.countdownText);
        clickCounterText = findViewById(R.id.clickCounterText);
        txtAdRemovalTag = findViewById(R.id.txtAdRemovalTag);
        clickGaugeFill = findViewById(R.id.clickGaugeFill);

        adResultStatus = findViewById(R.id.adResultStatus);
        adResultScore = findViewById(R.id.adResultScore);
        adResultClicks = findViewById(R.id.adResultClicks);

        btnTap = findViewById(R.id.btnTap);

        clickGaugeFill.setPivotX(0f);

        btnTap.setOnClickListener(v -> {
            if (!isRunning) return;

            if (isReversed) {
                currentClicks--;
            } else {
                currentClicks++;
            }
            updateClickText();
        });

        startStartCountdownPhase();
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            elapsedTime = SystemClock.uptimeMillis() - startTime;
            long remainingTime = MAX_TIME_MS - elapsedTime;

            if (remainingTime <= 0) {
                remainingTime = 0;
                stopwatchText.setText(formatTimeMs(remainingTime));
                stopwatchText.setTextColor(COLOR_RED);
                endGame();
                return;
            }

            stopwatchText.setText(formatTimeMs(remainingTime));

            if (remainingTime <= 3000) {
                stopwatchText.setTextColor(COLOR_RED);
            } else {
                stopwatchText.setTextColor(COLOR_YELLOW);
            }

            if (elapsedTime >= 8000 && currentPhase < 3) {
                currentPhase = 3;
                isReversed = false;
                btnTap.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
                btnTap.setText("TAP!\n(+1)");
                btnTap.animate().scaleX(0.5f).scaleY(0.5f).setDuration(200).start();
            }
            else if (elapsedTime >= 7000 && elapsedTime < 8000 && currentPhase < 2) {
                currentPhase = 2;
                isReversed = false;
                btnTap.setBackgroundTintList(ColorStateList.valueOf(COLOR_GREEN));
                btnTap.setText("TAP!\n(+1)");
            }
            else if (elapsedTime >= 5000 && elapsedTime < 7000 && currentPhase < 1) {
                currentPhase = 1;
                isReversed = true;
                btnTap.setBackgroundTintList(ColorStateList.valueOf(COLOR_RED));
                btnTap.setText("STOP!\n(-1)");
            }

            timerHandler.postDelayed(this, 20);
        }
    };

    private void updateClickText() {
        clickCounterText.setText("카운트 " + currentClicks + " / " + TARGET_CLICKS);
        if (currentClicks < 0) {
            clickCounterText.setTextColor(COLOR_RED);
        } else {
            clickCounterText.setTextColor(Color.parseColor("#1976D2"));
        }

        float progress = Math.max(0f, Math.min(1.0f, (float) currentClicks / TARGET_CLICKS));
        clickGaugeFill.setScaleX(progress);

        if (currentClicks >= TARGET_CLICKS) {
            txtAdRemovalTag.setTextColor(COLOR_TAG_GREEN);
        } else {
            txtAdRemovalTag.setTextColor(COLOR_GRAY);
        }
    }

    private void startStartCountdownPhase() {
        layoutCountdown.setVisibility(View.VISIBLE);

        if (stageCountdownTimer != null) {
            stageCountdownTimer.cancel();
        }

        stageCountdownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownText.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                layoutCountdown.setVisibility(View.GONE);
                startGame();
            }
        }.start();
    }

    private void startGame() {
        layoutAdContent.setVisibility(View.VISIBLE);
        isRunning = true;
        startTime = SystemClock.uptimeMillis();
        timerHandler.post(updateTimerRunnable);
    }

    private String formatTimeMs(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long ms = (timeMs % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d.%02d", totalSeconds, ms);
    }

    private void endGame() {
        isRunning = false;
        timerHandler.removeCallbacks(updateTimerRunnable);
        btnTap.setEnabled(false);

        boolean success = (currentClicks >= TARGET_CLICKS);

        layoutAdContent.setVisibility(View.GONE);
        cardAdResultPopup.setVisibility(View.VISIBLE);

        adResultClicks.setText("최종 터치: " + currentClicks + "회");

        if (success) {
            adResultStatus.setText("광고 제거 성공!");
            adResultStatus.setTextColor(COLOR_GREEN);
            adResultScore.setText("획득 점수: +5점");
        } else {
            adResultStatus.setText("광고 제거 실패!");
            adResultStatus.setTextColor(COLOR_RED);
            adResultScore.setText("획득 점수: -5점");
        }

        Intent result = new Intent();
        result.putExtra(GameContract.EXTRA_SUCCESS, success);
        setResult(RESULT_OK, result);

        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(updateTimerRunnable);
        if (stageCountdownTimer != null) stageCountdownTimer.cancel();
    }

    private void blockBackDuringGame() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(AdGame3Activity.this, "미니게임 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
