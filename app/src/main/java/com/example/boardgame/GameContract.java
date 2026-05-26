package com.example.boardgame;

public final class GameContract {
    public static final String EXTRA_DICE_RESULT = "extra_dice_result";
    public static final String EXTRA_SUCCESS = "extra_success";
    public static final String EXTRA_PROGRESS = "extra_progress";
    public static final String EXTRA_ROUND = "extra_round";
    public static final String EXTRA_PLAYER_INDEX = "extra_player_index";

    public static final int ROUND_CAPTCHA = 1;
    public static final int ROUND_PASSWORD = 2;
    public static final int ROUND_VOLUME_MAZE = 3;

    private GameContract() {
    }
}
