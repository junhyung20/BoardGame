package com.example.boardgame;

import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class VolumeMazeActivity extends AppCompatActivity implements SensorEventListener {
    private static final long TIME_LIMIT_MS = 120_000L;

    private VolumeMazeView mazeView;
    private LinearLayout layoutExplanation;
    private LinearLayout layoutCountdown;
    private View layoutGame;
    private TextView explanationTimerText;
    private TextView countdownText;
    private SeekBar volumeBar;
    private TextView statusText;
    private TextView volumeIndicatorText;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private CountDownTimer timer;
    private CountDownTimer explanationTimer;
    private CountDownTimer countdownTimer;
    private boolean finished;
    private boolean gameStarted;
    private int bestProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volume_maze);
        blockBackDuringGame();

        layoutExplanation = findViewById(R.id.layoutVolumeExplanation);
        layoutCountdown = findViewById(R.id.layoutVolumeCountdown);
        layoutGame = findViewById(R.id.layoutVolumeGame);
        explanationTimerText = findViewById(R.id.volumeExplanationTimer);
        countdownText = findViewById(R.id.volumeCountdownText);
        mazeView = findViewById(R.id.mazeView);
        volumeBar = findViewById(R.id.volumeBar);
        statusText = findViewById(R.id.miniGameStatusText);
        volumeIndicatorText = findViewById(R.id.volumeIndicatorText);
        Button resetButton = findViewById(R.id.resetButton);

        volumeBar.setMax(100);
        volumeBar.setEnabled(false);
        statusText.setText("볼륨 미로 진행 중");

        mazeView.setProgressListener(new VolumeMazeView.ProgressListener() {
            @Override
            public void onProgressChanged(int progress) {
                bestProgress = Math.max(bestProgress, progress);
                volumeBar.setProgress(progress);
                updateVolumeIndicator(progress);
            }

            @Override
            public void onGoalReached() {
                finishMiniGame(true);
            }
        });

        resetButton.setOnClickListener(v -> {
            bestProgress = 0;
            statusText.setText("볼륨 미로 진행 중");
            mazeView.resetGame();
        });
        volumeBar.post(() -> updateVolumeIndicator(volumeBar.getProgress()));

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        startExplanationPhase();
    }

    private void startExplanationPhase() {
        layoutExplanation.setVisibility(View.VISIBLE);
        layoutCountdown.setVisibility(View.GONE);
        layoutGame.setVisibility(View.GONE);

        explanationTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                explanationTimerText.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                startCountdownPhase();
            }
        }.start();
    }

    private void startCountdownPhase() {
        layoutExplanation.setVisibility(View.GONE);
        layoutCountdown.setVisibility(View.VISIBLE);
        layoutGame.setVisibility(View.GONE);

        countdownTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) Math.ceil(millisUntilFinished / 1000.0);
                countdownText.setText(String.valueOf(secondsLeft));
            }

            @Override
            public void onFinish() {
                startGame();
            }
        }.start();
    }

    private void startGame() {
        gameStarted = true;
        layoutCountdown.setVisibility(View.GONE);
        layoutGame.setVisibility(View.VISIBLE);
        mazeView.resetGame();
        mazeView.start();
        registerSensor();
        volumeBar.post(() -> updateVolumeIndicator(volumeBar.getProgress()));

        timer = new CountDownTimer(TIME_LIMIT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                statusText.setText("볼륨 미로 진행 중 | " + (millisUntilFinished / 1000) + "초");
            }

            @Override
            public void onFinish() {
                finishMiniGame(false);
            }
        }.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameStarted && !finished) {
            mazeView.start();
            registerSensor();
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        mazeView.stop();
        super.onPause();
    }

    private void registerSensor() {
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mazeView.setTilt(event.values[0], event.values[1]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void finishMiniGame(boolean success) {
        if (finished) {
            return;
        }
        finished = true;
        if (timer != null) {
            timer.cancel();
        }
        Toast.makeText(this, success ? "볼륨 미로 클리어!" : "볼륨 미로 시간 초과!", Toast.LENGTH_SHORT).show();

        Intent result = new Intent();
        result.putExtra(GameContract.EXTRA_SUCCESS, success);
        result.putExtra(GameContract.EXTRA_PROGRESS, success ? 100 : bestProgress);
        setResult(RESULT_OK, result);
        finish();
    }

    private void updateVolumeIndicator(int progress) {
        volumeIndicatorText.setText(String.valueOf(progress));
        volumeBar.post(() -> {
            int trackWidthPx = volumeBar.getWidth() - volumeBar.getPaddingStart() - volumeBar.getPaddingEnd();
            float fraction = Math.max(0f, Math.min(1f, progress / 100f));
            float thumbCenterX = volumeBar.getX()
                    + volumeBar.getPaddingStart()
                    + (trackWidthPx * fraction)
                    - volumeBar.getThumbOffset();
            float indicatorX = thumbCenterX - (volumeIndicatorText.getWidth() / 2f);
            float indicatorY = volumeBar.getY() - volumeIndicatorText.getHeight() - dpToPx(10f);

            volumeIndicatorText.setX(indicatorX);
            volumeIndicatorText.setY(indicatorY);
        });
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
        }
        if (explanationTimer != null) {
            explanationTimer.cancel();
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
        super.onDestroy();
    }

    private void blockBackDuringGame() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Toast.makeText(VolumeMazeActivity.this, "미니게임 중에는 뒤로갈 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
