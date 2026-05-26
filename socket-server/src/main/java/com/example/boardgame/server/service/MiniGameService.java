package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MiniGameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.util.List;
import java.util.Map;

public class MiniGameService {
    public static final int MINI_GAME_DURATION_MILLIS = 240_000;
    public static final int SUBMISSION_GRACE_MILLIS = 5_000;
    public static final int[] MINI_GAME_SCORE_BY_RANK = {30, 20, 10, 5};
    public static final int MAX_SUBMITTED_SCORE = 1_000_000;

    private final BoardGameService boardGameService;
    private final ScoreService scoreService;

    public MiniGameService(BoardGameService boardGameService, ScoreService scoreService) {
        this.boardGameService = boardGameService;
        this.scoreService = scoreService;
    }

    public void startMiniGame(Room room) {
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.WAITING_FOR_MINI_GAME);
        if (room.getMiniGameState() != null) {
            throw new IllegalStateException("Mini game has already started");
        }

        room.setMiniGameState(new MiniGameState(
                System.currentTimeMillis(),
                MINI_GAME_DURATION_MILLIS,
                SUBMISSION_GRACE_MILLIS
        ));
        gameState.transitionTo(GameState.MINI_GAME_RUNNING);
        gameState.setLastSystemMessage("Mini game started.");
        room.touch();
    }

    public void submitMiniGameScore(Room room, String playerId, int score) {
        boardGameService.requirePlayer(room, playerId);
        requireSubmittedScore(score);
        boardGameService.requirePhase(boardGameService.requireGameState(room), GameState.MINI_GAME_RUNNING);
        requireMiniGame(room).submitScore(playerId, score);
        room.touch();
    }

    public Map<String, Integer> finishMiniGame(Room room) {
        GameState gameState = boardGameService.requireGameState(room);
        boardGameService.requirePhase(gameState, GameState.MINI_GAME_RUNNING);
        MiniGameState miniGameState = requireMiniGame(room);
        if (!miniGameState.hasAllScores(room.getPlayers().size()) && !miniGameState.isSubmissionClosed()) {
            throw new IllegalStateException("Mini game is still accepting scores");
        }
        miniGameState.setStatus(MiniGameState.FINISHED);

        // 모델에 이미 예쁘게 '환산된 점수'가 들어있으므로, 바로 꺼내서 1~4등을 매깁니다.
        Map<String, Integer> rewards = scoreService.rankScores(
                miniGameState.getScoresByPlayerId(),
                MINI_GAME_SCORE_BY_RANK
        );
        scoreService.applyRewards(room, rewards);

        gameState.advanceRound();
        if (GameState.FINISHED.equals(gameState.getTurnPhase())) {
            room.setStatus(Room.FINISHED);
            gameState.setLastSystemMessage(finalResultMessage(room));
        } else {
            gameState.setLastSystemMessage("Mini game finished. Next round started.");
        }
        room.setMiniGameState(null);
        room.touch();
        return rewards;
    }

    private void requireSubmittedScore(int score) {
        if (score < 0 || score > MAX_SUBMITTED_SCORE) {
            throw new IllegalArgumentException("Submitted score is out of range");
        }
    }

    private String finalResultMessage(Room room) {
        List<Player> rankings = scoreService.calculateFinalRankings(room);
        if (rankings.isEmpty()) {
            return "Game finished.";
        }
        Player winner = rankings.get(0);
        return "Game finished. Winner: " + winner.getNickname() + " (" + winner.getScore() + ").";
    }

    private MiniGameState requireMiniGame(Room room) {
        MiniGameState miniGameState = room.getMiniGameState();
        if (miniGameState == null) {
            throw new IllegalStateException("Mini game has not started");
        }
        return miniGameState;
    }
}
