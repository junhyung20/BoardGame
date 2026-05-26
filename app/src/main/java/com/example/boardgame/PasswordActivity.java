package com.example.boardgame;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class PasswordActivity extends AppCompatActivity {

    private LinearLayout layoutExplanation, layoutCountdown, layoutGame, layoutStageResult;
    private TextView explanationTimer, countdownText, stageTitleText, timerText, clearText;
    private TextView stageResultStatus, stageRankText, stageScoreText, totalScoreText, nextStageNoticeText;
    private EditText passwordInput;
    private TextView[] rulesView = new TextView[5];

    private CountDownTimer currentTimer;
    private CountDownTimer stageCountdownTimer;
    private CountDownTimer resultTimer;
    private CountDownTimer explanationTimerObj;

    private int currentStage = 1;
    private int totalScore = 0;
    private boolean isCleared = false;
    private boolean isGameOver = false;

    private int activeRuleCount = 0;
    private String[] currentRuleTexts;
    private int[] currentRuleColors;
    private boolean[] isRevealed = new boolean[5];

    private final int COLOR_GREEN = Color.parseColor("#4CAF50");
    private final int COLOR_ORANGE = Color.parseColor("#FF9800");
    private final int COLOR_RED = Color.parseColor("#F44336");
    private final int COLOR_FAIL = Color.parseColor("#B0B0B0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_password);
        blockBackDuringGame();

        layoutExplanation = findViewById(R.id.layoutExplanation);
        layoutCountdown = findViewById(R.id.layoutCountdown);
        layoutGame = findViewById(R.id.layoutGame);
        layoutStageResult = findViewById(R.id.layoutStageResult);

        explanationTimer = findViewById(R.id.explanationTimer);
        countdownText = findViewById(R.id.countdownText);
        stageTitleText = findViewById(R.id.stageTitleText);
        timerText = findViewById(R.id.timerText);
        clearText = findViewById(R.id.clearText);
        passwordInput = findViewById(R.id.passwordInput);

        stageResultStatus = findViewById(R.id.stageResultStatus);
        stageRankText = findViewById(R.id.stageRankText);
        stageScoreText = findViewById(R.id.stageScoreText);
        totalScoreText = findViewById(R.id.totalScoreText);
        nextStageNoticeText = findViewById(R.id.nextStageNoticeText);

        rulesView[0] = findViewById(R.id.rule1);
        rulesView[1] = findViewById(R.id.rule2);
        rulesView[2] = findViewById(R.id.rule3);
        rulesView[3] = findViewById(R.id.rule4);
        rulesView[4] = findViewById(R.id.rule5);

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isGameOver && !isCleared) checkCurrentStageRules(s.toString());
            }
        });

        startExplanationPhase();
    }

    private void startExplanationPhase() {
        layoutExplanation.setVisibility(View.VISIBLE);

        if (explanationTimerObj != null) explanationTimerObj.cancel();

        explanationTimerObj = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                explanationTimer.setText(String.valueOf(secondsLeft));
            }
            @Override
            public void onFinish() {
                layoutExplanation.setVisibility(View.GONE);
                startCountdownPhase();
            }
        }.start();
    }

    private void startCountdownPhase() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        layoutStageResult.setVisibility(View.GONE);
        layoutCountdown.setVisibility(View.VISIBLE);

        if (stageCountdownTimer != null) stageCountdownTimer.cancel();

        stageCountdownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownText.setText(String.valueOf(secondsLeft));
            }
            @Override
            public void onFinish() {
                layoutCountdown.setVisibility(View.GONE);
                setupStage(currentStage);
            }
        }.start();
    }

    private void setupStage(int stage) {
        layoutGame.setVisibility(View.VISIBLE);
        passwordInput.setText("");
        passwordInput.setEnabled(true);
        clearText.setText("");
        isCleared = false;
        isGameOver = false;

        for (int i = 0; i < 5; i++) {
            isRevealed[i] = false;
        }

        stageTitleText.setText("STAGE " + stage);
        long timeLimit = 0;

        for (TextView rv : rulesView) rv.setVisibility(View.GONE);

        if (stage == 1) {
            timeLimit = 30000;
            activeRuleCount = 3;
            currentRuleTexts = new String[]{"조건 1: 8자 이상", "조건 2: 숫자 1개 이상 포함", "조건 3: 글자 3번 이상 연속"};
            currentRuleColors = new int[]{COLOR_GREEN, COLOR_GREEN, COLOR_ORANGE};
        } else if (stage == 2) {
            timeLimit = 50000;
            activeRuleCount = 4;
            currentRuleTexts = new String[]{"조건 1: 영문 대문자 포함", "조건 2: 특수문자 2개 이상 포함", "조건 3: 모음 2개 이상 포함", "조건 4: 홀수번째 자리는 반드시 숫자 (4번 이상)"};
            currentRuleColors = new int[]{COLOR_GREEN, COLOR_ORANGE, COLOR_ORANGE, COLOR_RED};
        } else if (stage == 3) {
            timeLimit = 90000;
            activeRuleCount = 5;
            currentRuleTexts = new String[]{"조건 1: 10자 이상", "조건 2: 특수문자 2개 이상 포함", "조건 3: 글자 2번 이상 연속", "조건 4: 숫자 총합 30 이상", "조건 5: 짝수번째 자리는 영문자 (4번 이상)"};
            currentRuleColors = new int[]{COLOR_GREEN, COLOR_ORANGE, COLOR_ORANGE, COLOR_RED, COLOR_RED};
        }

        for (int i = 0; i < activeRuleCount; i++) {
            rulesView[i].setVisibility(View.VISIBLE);
            rulesView[i].setText("조건 " + (i + 1) + ": ???");
            rulesView[i].setTextColor(COLOR_FAIL);
        }

        if (currentTimer != null) currentTimer.cancel();
        startGameTimer(timeLimit);
    }

    private void startGameTimer(long millis) {
        currentTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long s = millisUntilFinished / 1000;
                timerText.setText(String.format("남은 시간: %02d:%02d", s / 60, s % 60));
                timerText.setTextColor(s <= 10 ? COLOR_RED : Color.parseColor("#333333"));
            }
            @Override
            public void onFinish() {
                timerText.setText("남은 시간: 00:00");
                showStageResult();
            }
        }.start();
    }

    private void checkCurrentStageRules(String input) {
        boolean[] pass = new boolean[5];

        if (currentStage == 1) {
            pass[0] = input.length() >= 8;
            pass[1] = input.matches(".*\\d.*");
            pass[2] = input.matches(".*(.)\\1\\1.*");
        } else if (currentStage == 2) {
            pass[0] = input.matches(".*[A-Z].*");
            pass[1] = countSpecialChars(input) >= 2;
            pass[2] = input.matches(".*[aeiouAEIOU].*[aeiouAEIOU].*");
            pass[3] = checkOddPositionsAreNumbers(input, 4);
        } else if (currentStage == 3) {
            pass[0] = input.length() >= 10;
            pass[1] = countSpecialChars(input) >= 2;
            pass[2] = input.matches(".*(.)\\1.*");
            pass[3] = checkDigitSum(input, 30);
            pass[4] = checkEvenPositionsAreLetters(input, 4);
        }

        boolean allPassed = true;
        for (int i = 0; i < activeRuleCount; i++) {
            if (pass[i]) {
                isRevealed[i] = true;
            }

            if (isRevealed[i]) {
                rulesView[i].setText(currentRuleTexts[i]);
                rulesView[i].setTextColor(pass[i] ? currentRuleColors[i] : COLOR_FAIL);
            } else {
                rulesView[i].setText("조건 " + (i + 1) + ": ???");
                rulesView[i].setTextColor(COLOR_FAIL);
            }

            if (!pass[i]) {
                allPassed = false;
            }
        }

        if (allPassed) {
            isCleared = true;
            passwordInput.setEnabled(false);
            clearText.setText(currentStage + "스테이지 통과!\n남은 시간을 대기합니다...");
            clearText.setTextColor(Color.parseColor("#1976D2"));
        }
    }

    private boolean checkOddPositionsAreNumbers(String s, int minCount) {
        if (s.isEmpty()) return false;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (i % 2 == 0) {
                if (!Character.isDigit(s.charAt(i))) return false;
                count++;
            }
        }
        return count >= minCount;
    }

    private boolean checkEvenPositionsAreLetters(String s, int minCount) {
        if (s.isEmpty() || s.length() < 2) return false;
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (i % 2 == 1) {
                if (!Character.isLetter(s.charAt(i))) {
                    return false;
                }
                count++;
            }
        }
        return count >= minCount;
    }

    private int countSpecialChars(String s) {
        int count = 0;
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) count++;
        }
        return count;
    }

    private boolean checkDigitSum(String t, int target) {
        int sum = 0;
        for (char c : t.toCharArray()) {
            if (Character.isDigit(c)) sum += Character.getNumericValue(c);
        }
        return sum >= target;
    }

    private void showStageResult() {
        isGameOver = true;
        layoutGame.setVisibility(View.GONE);
        layoutStageResult.setVisibility(View.VISIBLE);

        int stageScore = isCleared ? 100 : 0;
        int stageRank = isCleared ? 1 : 4;
        totalScore += stageScore;

        if (isCleared) {
            stageResultStatus.setText(currentStage + "스테이지 통과!");
            stageResultStatus.setTextColor(COLOR_GREEN);
        } else {
            stageResultStatus.setText("시간 초과!");
            stageResultStatus.setTextColor(COLOR_RED);
        }

        stageRankText.setText("해당 스테이지 등수: " + stageRank + "등");
        stageScoreText.setText("획득 점수: +" + stageScore + "점");
        totalScoreText.setText("누적 점수: " + totalScore + "점");

        if (currentStage < 3) {
            nextStageNoticeText.setText("잠시 후 다음 스테이지가 시작됩니다...");
        } else {
            nextStageNoticeText.setText("잠시 후 최종 결과가 나옵니다...");
        }

        if (resultTimer != null) resultTimer.cancel();

        resultTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long m) {}
            @Override
            public void onFinish() {
                if (currentStage < 3) {
                    currentStage++;
                    startCountdownPhase();
                } else {
                    goToFinalResult();
                }
            }
        }.start();
    }

    private void goToFinalResult() {
        Intent intent = new Intent();
        intent.putExtra(GameContract.EXTRA_SUCCESS, totalScore >= 300);
        intent.putExtra(GameContract.EXTRA_PROGRESS, Math.min(100, totalScore / 3));
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (currentTimer != null) currentTimer.cancel();
        if (stageCountdownTimer != null) stageCountdownTimer.cancel();
        if (resultTimer != null) resultTimer.cancel();
        if (explanationTimerObj != null) explanationTimerObj.cancel();
    }

    private void blockBackDuringGame() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(PasswordActivity.this, "미니게임 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
