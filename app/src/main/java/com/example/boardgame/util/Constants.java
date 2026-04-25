package com.example.boardgame.util;

public final class Constants {
    public static final int MAX_PLAYERS = 4;
    public static final int FINAL_ROUND = 5;
    public static final int BOARD_SIZE = 24;
    public static final int DICE_MIN = 1;
    public static final int DICE_MAX = 6;

    public static final int RANDOM_EVENT_BONUS_SCORE = 10;
    public static final int RANDOM_EVENT_PENALTY_SCORE = -5;
    public static final int RANDOM_EVENT_MOVE_STEPS = 2;

    public static final String CARD_DOUBLE_DICE = "DOUBLE_DICE";
    public static final String CARD_SHIELD = "SHIELD";
    public static final String CARD_SWAP_POSITION = "SWAP_POSITION";

    public static final int MINI_GAME_DURATION_SECONDS = 45;
    public static final int MICRO_GAME_DURATION_SECONDS = 10;
    public static final int[] MINI_GAME_SCORE_BY_RANK = {30, 20, 10, 5};
    public static final int[] MICRO_GAME_SCORE_BY_RANK = {8, 5, 3, 1};

    private Constants() {
    }
}
