package com.example.boardgame.server;

import com.example.boardgame.socket.protocol.ErrorCodes;

final class ServerErrorMapper {
    private ServerErrorMapper() {
    }

    static ServerError from(Throwable throwable) {
        if (throwable instanceof AuthException) {
            return new ServerError(ErrorCodes.UNAUTHENTICATED, "Authentication required");
        }

        String message = throwable.getMessage() == null ? "" : throwable.getMessage();

        if (message.startsWith("Unsupported type:")) {
            return new ServerError(ErrorCodes.UNSUPPORTED_TYPE, "Unsupported command type");
        }
        if ("Connection is already in a room".equals(message)) {
            return new ServerError(ErrorCodes.ALREADY_IN_ROOM, "Connection is already in a room");
        }
        if ("Connection is not in a room".equals(message)) {
            return new ServerError(ErrorCodes.NOT_IN_ROOM, "Connection is not in a room");
        }
        if ("Room not found".equals(message)) {
            return new ServerError(ErrorCodes.ROOM_NOT_FOUND, "Room not found");
        }
        if ("Room is full".equals(message)) {
            return new ServerError(ErrorCodes.ROOM_FULL, "Room is full");
        }
        if ("Room is already in game".equals(message) || "Cannot leave after game has started".equals(message)) {
            return new ServerError(ErrorCodes.ROOM_IN_GAME, "Room is already in game");
        }
        if ("Invalid room password".equals(message)) {
            return new ServerError(ErrorCodes.INVALID_ROOM_PASSWORD, "Invalid room password");
        }
        if ("User is already in a room".equals(message)) {
            return new ServerError(ErrorCodes.DUPLICATE_USER, "User is already in a room");
        }
        if ("Player not found in room".equals(message)) {
            return new ServerError(ErrorCodes.PLAYER_NOT_FOUND, "Player not found in room");
        }
        if ("Only the host can start the game".equals(message)) {
            return new ServerError(ErrorCodes.NOT_HOST, "Only the host can perform this command");
        }
        if ("It is not your turn".equals(message)) {
            return new ServerError(ErrorCodes.NOT_YOUR_TURN, "It is not your turn");
        }
        if (message.startsWith("At least ") && message.endsWith(" ready player(s) are required to start")) {
            return new ServerError(ErrorCodes.NOT_ENOUGH_READY_PLAYERS, "Not enough ready players");
        }
        if ("Game has not started".equals(message)) {
            return new ServerError(ErrorCodes.GAME_NOT_STARTED, "Game has not started");
        }
        if ("Game has already started".equals(message)) {
            return new ServerError(ErrorCodes.GAME_ALREADY_STARTED, "Game has already started");
        }
        if (message.startsWith("Current phase is not ") || message.startsWith("Cannot transition from ")) {
            return new ServerError(ErrorCodes.INVALID_PHASE, "Command is not allowed in the current game phase");
        }
        if ("Submitted score is out of range".equals(message)) {
            return new ServerError(ErrorCodes.INVALID_SCORE, "Submitted score is out of range");
        }
        if ("Mini game has not started".equals(message)) {
            return new ServerError(ErrorCodes.MINI_GAME_NOT_STARTED, "Mini game has not started");
        }
        if ("Mini game has already started".equals(message)) {
            return new ServerError(ErrorCodes.MINI_GAME_ALREADY_STARTED, "Mini game has already started");
        }
        if ("Mini game is still accepting scores".equals(message)) {
            return new ServerError(ErrorCodes.INVALID_STATE, "Mini game is still accepting scores");
        }
        if ("Micro game has not started".equals(message)) {
            return new ServerError(ErrorCodes.MICRO_GAME_NOT_STARTED, "Micro game has not started");
        }
        if ("Micro game has already started".equals(message)) {
            return new ServerError(ErrorCodes.MICRO_GAME_ALREADY_STARTED, "Micro game has already started");
        }
        if (message.endsWith("score has already been submitted")) {
            return new ServerError(ErrorCodes.SCORE_ALREADY_SUBMITTED, "Score has already been submitted");
        }

        if (throwable instanceof IllegalStateException) {
            return new ServerError(ErrorCodes.INVALID_STATE, "Command is not allowed right now");
        }
        return new ServerError(ErrorCodes.BAD_REQUEST, "Invalid request");
    }
}
