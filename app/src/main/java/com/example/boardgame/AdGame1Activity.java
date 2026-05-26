package com.example.boardgame;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class AdGame1Activity extends AppCompatActivity {
    private TextView adTimerText;
    private CountDownTimer gameTimer;
    private CountDownTimer stageCountdownTimer;
    private boolean ended = false;
    private int score = 0;
    private ObjectAnimator realButtonAnimation;

    private ConstraintLayout layoutAdContent;
    private CardView cardAdResultPopup;
    private TextView adResultStatus, adResultScore;

    private LinearLayout layoutCountdown;
    private TextView countdownText;

    private Toast currentToast;
    private Handler toastHandler = new Handler(Looper.getMainLooper());

    private final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private final int COLOR_RED = Color.parseColor("#D32F2F");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_game1);
        blockBackDuringGame();

        adTimerText = findViewById(R.id.adTimerText);
        ImageView fake1 = findViewById(R.id.fakeButton1);
        ImageButton real = findViewById(R.id.realButton);
        TextView fake2 = findViewById(R.id.fakeButton2);
        ImageView fake3 = findViewById(R.id.fakeButton3);
        TextView fake4 = findViewById(R.id.fakeButton4);
        ImageView fake5 = findViewById(R.id.fakeButton5);

        Button btnInstall = findViewById(R.id.btnInstall);

        layoutAdContent = findViewById(R.id.layoutAdContent);
        cardAdResultPopup = findViewById(R.id.cardAdResultPopup);
        adResultStatus = findViewById(R.id.adResultStatus);
        adResultScore = findViewById(R.id.adResultScore);

        layoutCountdown = findViewById(R.id.layoutCountdown);
        countdownText = findViewById(R.id.countdownText);

        View.OnClickListener fakeL = v -> {
            if (!ended) {
                showWhiteToast("광고를 제거하지 못했습니다.");
            }
        };

        fake1.setOnClickListener(fakeL);
        fake2.setOnClickListener(fakeL);
        fake3.setOnClickListener(fakeL);
        fake4.setOnClickListener(fakeL);
        fake5.setOnClickListener(fakeL);
        btnInstall.setOnClickListener(fakeL);

        real.setOnClickListener(v -> {
            if (!ended) {
                score = 50;
                end(true);
            }
        });

        realButtonAnimation = ObjectAnimator.ofFloat(real, "alpha", 0.0f, 1.0f);
        realButtonAnimation.setDuration(5000);
        realButtonAnimation.setStartDelay(5000);

        startGame();
    }

    private void showWhiteToast(String message) {
        if (currentToast != null) {
            currentToast.cancel();
        }

        toastHandler.removeCallbacksAndMessages(null);

        toastHandler.postDelayed(() -> {
            currentToast = new Toast(getApplicationContext());
            TextView tv = new TextView(this);
            tv.setText(message);
            tv.setTextColor(Color.BLACK);
            tv.setTextSize(16);
            tv.setPadding(60, 30, 60, 30);
            tv.setBackgroundResource(android.R.drawable.toast_frame);
            tv.getBackground().setTint(Color.parseColor("#F0F0F0"));
            currentToast.setView(tv);
            currentToast.setDuration(Toast.LENGTH_SHORT);
            currentToast.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 60);
            currentToast.show();
        }, 150);
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
        realButtonAnimation.start();
        startTimer(10000);
    }

    private void startTimer(long durationMillis) {
        gameTimer = new CountDownTimer(durationMillis, 1000) {
            @Override
            public void onTick(long m) {
                long s = m / 1000;
                adTimerText.setText(s + "초 남았습니다");
                if (s <= 3) adTimerText.setTextColor(COLOR_RED);
                else adTimerText.setTextColor(Color.parseColor("#333333"));
            }
            @Override
            public void onFinish() {
                if (!ended) {
                    score = 0;
                    end(false);
                }
            }
        }.start();
    }

    private void end(boolean s) {
        ended = true;
        if (gameTimer != null) gameTimer.cancel();
        if (stageCountdownTimer != null) stageCountdownTimer.cancel();
        if (realButtonAnimation != null) realButtonAnimation.cancel();

        toastHandler.removeCallbacksAndMessages(null);
        if (currentToast != null) {
            currentToast.cancel();
        }

        layoutAdContent.setVisibility(View.GONE);
        cardAdResultPopup.setVisibility(View.VISIBLE);

        if (s) {
            adResultStatus.setText("광고 제거 성공!");
            adResultStatus.setTextColor(COLOR_GREEN);
            adResultScore.setText("획득 점수: +5점");
        } else {
            adResultStatus.setText("광고 제거 실패!");
            adResultStatus.setTextColor(COLOR_RED);
            adResultScore.setText("획득 점수: -5점");
        }

        Intent result = new Intent();
        result.putExtra(GameContract.EXTRA_SUCCESS, s);
        setResult(RESULT_OK, result);

        new Handler().postDelayed(() -> {
            finish();
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameTimer != null) gameTimer.cancel();
        if (stageCountdownTimer != null) stageCountdownTimer.cancel();
        if (realButtonAnimation != null) realButtonAnimation.cancel();
        toastHandler.removeCallbacksAndMessages(null);
        if (currentToast != null) {
            currentToast.cancel();
        }
    }

    private void blockBackDuringGame() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(AdGame1Activity.this, "미니게임 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
