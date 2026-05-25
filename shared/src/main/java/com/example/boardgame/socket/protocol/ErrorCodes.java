package com.example.boardgame.socket.protocol;

public final class ErrorCodes {
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    public static final String MALFORMED_MESSAGE = "MALFORMED_MESSAGE";
    public static final String UNSUPPORTED_TYPE = "UNSUPPORTED_TYPE";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INVALID_STATE = "INVALID_STATE";
    public static final String INVALID_PHASE = "INVALID_PHASE";
    public static final String INVALID_SCORE = "INVALID_SCORE";

    public static final String ALREADY_IN_ROOM = "ALREADY_IN_ROOM";
    public static final String NOT_IN_ROOM = "NOT_IN_ROOM";
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_FULL = "ROOM_FULL";
    public static final String ROOM_IN_GAME = "ROOM_IN_GAME";
    public static final String INVALID_ROOM_PASSWORD = "INVALID_ROOM_PASSWORD";
    public static final String DUPLICATE_USER = "DUPLICATE_USER";
    public static final String PLAYER_NOT_FOUND = "PLAYER_NOT_FOUND";
    public static final String NOT_HOST = "NOT_HOST";
    public static final String NOT_YOUR_TURN = "NOT_YOUR_TURN";
    public static final String NOT_ENOUGH_READY_PLAYERS = "NOT_ENOUGH_READY_PLAYERS";

    public static final String GAME_NOT_STARTED = "GAME_NOT_STARTED";
    public static final String GAME_ALREADY_STARTED = "GAME_ALREADY_STARTED";
    public static final String MINI_GAME_NOT_STARTED = "MINI_GAME_NOT_STARTED";
    public static final String MINI_GAME_ALREADY_STARTED = "MINI_GAME_ALREADY_STARTED";
    public static final String MICRO_GAME_NOT_STARTED = "MICRO_GAME_NOT_STARTED";
    public static final String MICRO_GAME_ALREADY_STARTED = "MICRO_GAME_ALREADY_STARTED";
    public static final String SCORE_ALREADY_SUBMITTED = "SCORE_ALREADY_SUBMITTED";

    private ErrorCodes() {
    }
}
