package com.example.boardgame;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class DiceActivity extends AppCompatActivity {

    private ConstraintLayout layoutDiceMain;
    private CardView cardResultPopup;
    private TextView txtTimer, txtFinalDiceResult, txtInstruction, txtDiceVisual;
    private Button btnDice12, btnDice34, btnDice56, btnDiceRandom, btnRoll;
    private Button[] diceButtons;

    private Handler timerHandler = new Handler(Looper.getMainLooper());
    private long startTime = 0L;
    private final long MAX_TIME_MS = 10000;
    private boolean isTimerRunning = false;

    private Button selectedButton = null;
    private boolean isYabawiActive = false;
    private boolean isLocked = false;
    private boolean isAnimating = false;

    private Random random = new Random();

    private final int COLOR_YELLOW = Color.parseColor("#FFCA28");
    private final int COLOR_RED = Color.parseColor("#D32F2F");
    private final int COLOR_NORMAL_TEXT = Color.parseColor("#333333");

    private final String[] diceFaces = {"⚀", "⚁", "⚂", "⚃", "⚄", "⚅"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dice);
        blockBackDuringGame();

        layoutDiceMain = findViewById(R.id.layoutDiceMain);
        cardResultPopup = findViewById(R.id.cardResultPopup);
        txtTimer = findViewById(R.id.txtTimer);
        txtInstruction = findViewById(R.id.txtInstruction);
        txtDiceVisual = findViewById(R.id.txtDiceVisual);
        txtFinalDiceResult = findViewById(R.id.txtFinalDiceResult);

        btnDice12 = findViewById(R.id.btnDice12);
        btnDice34 = findViewById(R.id.btnDice34);
        btnDice56 = findViewById(R.id.btnDice56);
        btnDiceRandom = findViewById(R.id.btnDiceRandom);
        btnRoll = findViewById(R.id.btnRoll);

        diceButtons = new Button[]{btnDice12, btnDice34, btnDice56, btnDiceRandom};

        btnDice12.setTag("12");
        btnDice34.setTag("34");
        btnDice56.setTag("56");
        btnDiceRandom.setTag("RANDOM");

        for (Button btn : diceButtons) {
            btn.setAlpha(0.6f);
            btn.setOnClickListener(v -> handleDiceSelection((Button) v));
        }

        btnRoll.setOnClickListener(v -> {
            if (isAnimating || isYabawiActive) return;
            if (selectedButton == null) {
                Toast.makeText(this, "먼저 주사위를 선택하세요", Toast.LENGTH_SHORT).show();
                return;
            }
            startDiceRollAnimation();
        });

        startTimer();
    }

    private Runnable updateTimerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isTimerRunning) return;

            long elapsedTime = SystemClock.uptimeMillis() - startTime;
            long remainingTime = MAX_TIME_MS - elapsedTime;

            if (remainingTime <= 0) {
                remainingTime = 0;
                txtTimer.setText("0");
                txtTimer.setTextColor(COLOR_RED);
                isTimerRunning = false;
                handleTimeout();
                return;
            }

            int secondsLeft = (int) Math.ceil(remainingTime / 1000.0);
            txtTimer.setText(String.valueOf(secondsLeft));

            if (remainingTime <= 3000) {
                txtTimer.setTextColor(COLOR_RED);
            } else {
                txtTimer.setTextColor(COLOR_YELLOW);
            }

            timerHandler.postDelayed(this, 50);
        }
    };

    private void startTimer() {
        isTimerRunning = true;
        startTime = SystemClock.uptimeMillis();
        timerHandler.post(updateTimerRunnable);
    }

    private void stopTimer() {
        isTimerRunning = false;
        timerHandler.removeCallbacks(updateTimerRunnable);
    }

    private void handleDiceSelection(Button clickedButton) {
        if (isAnimating || isLocked) return;

        if (!isYabawiActive && random.nextFloat() < 0.2f) {
            triggerYabawiGimmick();
            return;
        }

        selectedButton = clickedButton;

        for (Button btn : diceButtons) {
            btn.setAlpha(0.6f);
            btn.setScaleX(1.0f);
            btn.setScaleY(1.0f);
        }
        clickedButton.setAlpha(1.0f);
        clickedButton.setScaleX(1.1f);
        clickedButton.setScaleY(1.1f);

        if (isYabawiActive) {
            isLocked = true;
            revealOriginalButtons();
            startDiceRollAnimation();
        }
    }

    private void triggerYabawiGimmick() {
        stopTimer();
        isAnimating = true;
        isYabawiActive = true;
        selectedButton = null;

        txtTimer.setVisibility(View.INVISIBLE);
        btnRoll.setVisibility(View.INVISIBLE);

        txtInstruction.setText("다크 패턴 발동! 3초 후 버튼들이 가려지고 섞입니다");
        txtInstruction.setTextColor(COLOR_RED);

        for (Button btn : diceButtons) {
            btn.setAlpha(1.0f);
            btn.setScaleX(1.0f);
            btn.setScaleY(1.0f);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            for (Button btn : diceButtons) {
                btn.setText("?");
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#9E9E9E")));
            }
            txtInstruction.setText("주사위 버튼들을 섞는 중...");
            shuffleDicePositions(5);
        }, 3000);
    }

    private void shuffleDicePositions(int remainingShuffles) {
        if (remainingShuffles <= 0) {
            isAnimating = false;
            txtTimer.setVisibility(View.VISIBLE);
            txtInstruction.setText("주사위 버튼을 선택하세요");
            startTimer();
            return;
        }

        List<Float> currentXPositions = new ArrayList<>();
        for (Button btn : diceButtons) {
            currentXPositions.add(btn.getX());
        }
        Collections.shuffle(currentXPositions);

        for (int i = 0; i < diceButtons.length; i++) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(diceButtons[i], "x", currentXPositions.get(i));
            animator.setDuration(500);

            if (i == 0) {
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        shuffleDicePositions(remainingShuffles - 1);
                    }
                });
            }
            animator.start();
        }
    }

    private void revealOriginalButtons() {
        for (Button btn : diceButtons) {
            String tag = (String) btn.getTag();
            switch (tag) {
                case "12":
                    btn.setText("1~2\n(30%)");
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#42A5F5")));
                    break;
                case "34":
                    btn.setText("3~4\n(20%)");
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#66BB6A")));
                    break;
                case "56":
                    btn.setText("5~6\n(20%)");
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFA726")));
                    break;
                case "RANDOM":
                    btn.setText("랜덤\n(1/6)");
                    btn.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#AB47BC")));
                    break;
            }
        }
    }

    private void handleTimeout() {
        if (selectedButton == null) {
            selectedButton = btnDiceRandom;
            Toast.makeText(this, "시간 초과! 랜덤으로 굴립니다.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "시간 초과! 선택된 주사위로 굴립니다.", Toast.LENGTH_SHORT).show();
        }

        if (isYabawiActive) {
            revealOriginalButtons();
        }
        startDiceRollAnimation();
    }

    private void startDiceRollAnimation() {
        stopTimer();
        isAnimating = true;
        btnRoll.setEnabled(false);
        for (Button btn : diceButtons) btn.setEnabled(false);

        String diceType = (String) selectedButton.getTag();
        int finalResult = calculateDiceResult(diceType);

        txtInstruction.setText("주사위를 굴리는 중...");
        txtInstruction.setTextColor(COLOR_NORMAL_TEXT);
        txtDiceVisual.setTextColor(Color.parseColor("#1976D2"));

        new CountDownTimer(1500, 100) {
            @Override
            public void onTick(long millisUntilFinished) {
                txtDiceVisual.setText(diceFaces[random.nextInt(6)]);
            }

            @Override
            public void onFinish() {
                txtDiceVisual.setText(diceFaces[finalResult - 1]);
                showResultPopup(finalResult);
            }
        }.start();
    }

    private void showResultPopup(int resultNumber) {
        layoutDiceMain.setVisibility(View.GONE);
        cardResultPopup.setVisibility(View.VISIBLE);
        txtFinalDiceResult.setText(String.valueOf(resultNumber));

        Intent result = new Intent();
        result.putExtra(GameContract.EXTRA_DICE_RESULT, resultNumber);
        setResult(RESULT_OK, result);

        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 3000);
    }

    private int calculateDiceResult(String type) {
        int r = random.nextInt(100);

        switch (type) {
            case "12":
                if (r < 30) return 1;
                else if (r < 60) return 2;
                else if (r < 70) return 3;
                else if (r < 80) return 4;
                else if (r < 90) return 5;
                else return 6;
            case "34":
                if (r < 15) return 1;
                else if (r < 30) return 2;
                else if (r < 50) return 3;
                else if (r < 70) return 4;
                else if (r < 85) return 5;
                else return 6;
            case "56":
                if (r < 15) return 1;
                else if (r < 30) return 2;
                else if (r < 45) return 3;
                else if (r < 60) return 4;
                else if (r < 80) return 5;
                else return 6;
            case "RANDOM":
            default:
                return random.nextInt(6) + 1;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }

    private void blockBackDuringGame() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(DiceActivity.this, "주사위 진행 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
