package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MicroGameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

public class MicroGameService {
    public static final int MICRO_GAME_DURATION_MILLIS = 10_000;
    public static final int SUBMISSION_GRACE_MILLIS = 2_000;
    public static final int MAX_SUBMITTED_SCORE = 1_000_000;

    private final BoardGameService boardGameService;

    public MicroGameService(BoardGameService boardGameService) {
        this.boardGameService = boardGameService;
    }

    public void startMicroGame(Room room, String playerId) {
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.WAITING_FOR_MICRO_GAME);
        if (!playerId.equals(gameState.getCurrentPlayerId())) {
            throw new IllegalStateException("It is not your turn");
        }
        if (room.getMicroGameState() != null) {
            throw new IllegalStateException("Micro game has already started");
        }

        Player player = boardGameService.requirePlayer(room, playerId);
        room.setMicroGameState(new MicroGameState(
                playerId,
                System.currentTimeMillis(),
                MICRO_GAME_DURATION_MILLIS,
                SUBMISSION_GRACE_MILLIS
        ));
        player.setInMicroGame(true);
        gameState.setLastSystemMessage(player.getNickname() + " is playing a micro game.");
        room.touch();
    }

    public void submitMicroGameScore(Room room, String playerId, int score) {
        requireSubmittedScore(score);
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.WAITING_FOR_MICRO_GAME);
        if (!playerId.equals(gameState.getCurrentPlayerId())) {
            throw new IllegalStateException("It is not your turn");
        }

        Player player = boardGameService.requirePlayer(room, playerId);
        if (!player.isInMicroGame()) {
            throw new IllegalStateException("Player is not in a micro game");
        }
        MicroGameState microGameState = requireMicroGame(room);
        microGameState.requirePlayer(playerId);

        if (!microGameState.isSubmissionClosed()) {
            microGameState.markSubmitted();
            player.addScore(score);
            gameState.setLastSystemMessage(player.getNickname() + " finished the micro game for " + score + " points.");
        } else {
            gameState.setLastSystemMessage(player.getNickname() + " missed the micro game deadline.");
        }

        player.setInMicroGame(false);
        room.setMicroGameState(null);
        gameState.advanceTurn();
        room.touch();
    }

    private void requireSubmittedScore(int score) {
        if (score < 0 || score > MAX_SUBMITTED_SCORE) {
            throw new IllegalArgumentException("Submitted score is out of range");
        }
    }

    private MicroGameState requireMicroGame(Room room) {
        MicroGameState microGameState = room.getMicroGameState();
        if (microGameState == null) {
            throw new IllegalStateException("Micro game has not started");
        }
        return microGameState;
    }
}
