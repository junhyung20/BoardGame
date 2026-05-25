package com.example.boardgame;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable moveToNickname = () -> {
        startActivity(new Intent(this, NicknameActivity.class));
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        handler.postDelayed(moveToNickname, 1200L);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(moveToNickname);
        super.onDestroy();
    }
}
