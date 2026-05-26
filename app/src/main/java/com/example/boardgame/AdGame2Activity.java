package com.example.boardgame;

import android.content.Intent;
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

public class AdGame2Activity extends AppCompatActivity {

    private ConstraintLayout layoutAdContent;
    private LinearLayout layoutCountdown;
    private View gaugeBarOnly;
    private View gaugeFill;
    private CardView cardAdResultPopup;
    private TextView stopwatchText, countdownText, adResultStatus, adResultScore, adResultTime;
    private Button btnStop;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;
    private long elapsedTime = 0L;
    private boolean isRunning = false;
    private boolean ended = false;

    private CountDownTimer stageCountdownTimer;

    private final long TARGET_MS = 8000;
    private final long TOLERANCE_MS = 500;
    private final long MAX_TIME_MS = 10000;

    private final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private final int COLOR_RED = Color.parseColor("#D32F2F");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_game2);
        blockBackDuringGame();

        layoutAdContent = findViewById(R.id.layoutAdContent);
        layoutCountdown = findViewById(R.id.layoutCountdown);

        gaugeBarOnly = findViewById(R.id.gaugeBarOnly);
        gaugeFill = findViewById(R.id.gaugeFill);

        cardAdResultPopup = findViewById(R.id.cardAdResultPopup);

        stopwatchText = findViewById(R.id.stopwatchText);
        countdownText = findViewById(R.id.countdownText);
        adResultStatus = findViewById(R.id.adResultStatus);
        adResultScore = findViewById(R.id.adResultScore);
        adResultTime = findViewById(R.id.adResultTime);
        btnStop = findViewById(R.id.btnStop);

        gaugeFill.setPivotX(0f);

        btnStop.setOnClickListener(v -> {
            if (isRunning && !ended) {
                stopStopwatch();
            }
        });

        startStartCountdownPhase();
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            elapsedTime = SystemClock.uptimeMillis() - startTime;

            stopwatchText.setText(formatTimeMs(elapsedTime));

            float progress = Math.min(1.0f, (float) elapsedTime / MAX_TIME_MS);
            gaugeFill.setScaleX(progress);

            if (elapsedTime >= 3000 && elapsedTime <= 4000) {
                float alpha = 1.0f - ((elapsedTime - 3000) / 1000.0f);
                stopwatchText.setAlpha(alpha);
                gaugeBarOnly.setAlpha(alpha);
            } else if (elapsedTime > 4000) {
                stopwatchText.setAlpha(0f);
                gaugeBarOnly.setAlpha(0f);
            } else {
                stopwatchText.setAlpha(1.0f);
                gaugeBarOnly.setAlpha(1.0f);
            }

            if (elapsedTime >= 10000) {
                elapsedTime = 10000;
                stopStopwatch();
                return;
            }

            timerHandler.postDelayed(this, 20);
        }
    };

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

    private void stopStopwatch() {
        isRunning = false;
        ended = true;
        timerHandler.removeCallbacks(updateTimerRunnable);

        btnStop.setEnabled(false);

        stopwatchText.setAlpha(1.0f);
        gaugeBarOnly.setAlpha(1.0f);

        stopwatchText.setText(formatTimeMs(elapsedTime));
        stopwatchText.setTextColor(Color.parseColor("#FFCA28"));

        long diff = Math.abs(elapsedTime - TARGET_MS);
        boolean success = (diff <= TOLERANCE_MS);

        end(success, elapsedTime);
    }

    private String formatTimeMs(long timeMs) {
        long totalSeconds = timeMs / 1000;
        long ms = (timeMs % 1000) / 10;
        return String.format(Locale.getDefault(), "%02d.%02d", totalSeconds, ms);
    }

    private void end(boolean success, long finalTimeMs) {
        layoutAdContent.setVisibility(View.GONE);
        cardAdResultPopup.setVisibility(View.VISIBLE);

        String stoppedTimeStr = String.format(Locale.getDefault(), "멈춘 시간: %.2f초", finalTimeMs / 1000.0);
        adResultTime.setText(stoppedTimeStr);

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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finish();
        }, 5000);
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
                Toast.makeText(AdGame2Activity.this, "미니게임 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
