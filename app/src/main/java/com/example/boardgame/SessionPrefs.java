package com.example.boardgame;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public final class SessionPrefs {
    private static final String PREFS_NAME = "boardgame_client_prefs";
    private static final String KEY_NICKNAME = "key_nickname";
    private static final String KEY_VOLUME = "key_volume";
    private static final String KEY_SOUND_EFFECTS = "key_sound_effects";
    private static final String KEY_VIBRATION = "key_vibration";
    private static final String KEY_DICE_FAST = "key_dice_fast";
    private static final String KEY_DEBUG_MODE = "key_debug_mode";
    private static final String KEY_DEV_CLIENT_TOKEN = "key_dev_client_token";
    private static final int DEFAULT_VOLUME = 50;

    private SessionPrefs() {
    }

    public static String getNickname(Context context) {
        return prefs(context).getString(KEY_NICKNAME, "");
    }

    public static void setNickname(Context context, String nickname) {
        prefs(context).edit().putString(KEY_NICKNAME, nickname == null ? "" : nickname).apply();
    }

    public static int getVolume(Context context) {
        return clamp(prefs(context).getInt(KEY_VOLUME, DEFAULT_VOLUME), 0, 100);
    }

    public static void setVolume(Context context, int volume) {
        prefs(context).edit().putInt(KEY_VOLUME, clamp(volume, 0, 100)).apply();
    }

    public static boolean isSoundEffectsOn(Context context) {
        return prefs(context).getBoolean(KEY_SOUND_EFFECTS, true);
    }

    public static void setSoundEffectsOn(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_SOUND_EFFECTS, on).apply();
    }

    public static boolean isVibrationOn(Context context) {
        return prefs(context).getBoolean(KEY_VIBRATION, true);
    }

    public static void setVibrationOn(Context context, boolean on) {
        prefs(context).edit().putBoolean(KEY_VIBRATION, on).apply();
    }

    public static boolean isDiceFast(Context context) {
        return prefs(context).getBoolean(KEY_DICE_FAST, false);
    }

    public static void setDiceFast(Context context, boolean fast) {
        prefs(context).edit().putBoolean(KEY_DICE_FAST, fast).apply();
    }

    public static boolean isDebugMode(Context context) {
        return prefs(context).getBoolean(KEY_DEBUG_MODE, false);
    }

    public static void setDebugMode(Context context, boolean debugMode) {
        prefs(context).edit().putBoolean(KEY_DEBUG_MODE, debugMode).apply();
    }

    public static String getOrCreateDevClientToken(Context context) {
        SharedPreferences sharedPreferences = prefs(context);
        String existing = sharedPreferences.getString(KEY_DEV_CLIENT_TOKEN, "");
        if (existing != null && !existing.trim().isEmpty()) {
            return existing;
        }

        String created = "DEV_" + UUID.randomUUID();
        sharedPreferences.edit().putString(KEY_DEV_CLIENT_TOKEN, created).apply();
        return created;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
