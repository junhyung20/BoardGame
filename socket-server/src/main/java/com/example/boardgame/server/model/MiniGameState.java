package com.example.boardgame.server.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MiniGameState {
    public static final String RUNNING = "RUNNING";
    public static final String FINISHED = "FINISHED";

    private final long startedAtMillis;
    private final int durationMillis;
    private final int submissionGraceMillis;
    private String status = RUNNING;
    private final Map<String, Integer> scoresByPlayerId = new LinkedHashMap<>();

    public MiniGameState(long startedAtMillis, int durationMillis, int submissionGraceMillis) {
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
        this.submissionGraceMillis = submissionGraceMillis;
    }

    public void submitScore(String playerId, int score) {
        if (!RUNNING.equals(status)) {
            throw new IllegalStateException("Mini game is not running");
        }
        if (isSubmissionClosed()) {
            throw new IllegalStateException("Mini game submission is too late");
        }
        if (scoresByPlayerId.containsKey(playerId)) {
            throw new IllegalStateException("Mini game score has already been submitted");
        }
        scoresByPlayerId.put(playerId, score);
    }

    public boolean isSubmissionClosed() {
        return System.currentTimeMillis() > getSubmissionDeadlineMillis();
    }

    public boolean hasAllScores(int playerCount) {
        return scoresByPlayerId.size() >= playerCount;
    }

    public void removeScore(String playerId) {
        scoresByPlayerId.remove(playerId);
    }

    public long getSubmissionDeadlineMillis() {
        return startedAtMillis + durationMillis + submissionGraceMillis;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Integer> getScoresByPlayerId() {
        return Collections.unmodifiableMap(scoresByPlayerId);
    }
}
