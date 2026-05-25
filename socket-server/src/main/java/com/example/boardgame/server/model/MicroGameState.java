package com.example.boardgame.server.model;

public class MicroGameState {
    private final String playerId;
    private final long startedAtMillis;
    private final int durationMillis;
    private final int submissionGraceMillis;
    private boolean submitted;

    public MicroGameState(String playerId, long startedAtMillis, int durationMillis, int submissionGraceMillis) {
        this.playerId = playerId == null ? "" : playerId;
        this.startedAtMillis = startedAtMillis;
        this.durationMillis = durationMillis;
        this.submissionGraceMillis = submissionGraceMillis;
    }

    public void requirePlayer(String playerId) {
        if (!this.playerId.equals(playerId)) {
            throw new IllegalStateException("Player is not in this micro game");
        }
    }

    public boolean isForPlayer(String playerId) {
        return this.playerId.equals(playerId);
    }

    public void markSubmitted() {
        if (submitted) {
            throw new IllegalStateException("Micro game score has already been submitted");
        }
        submitted = true;
    }

    public boolean isSubmissionClosed() {
        return System.currentTimeMillis() > getSubmissionDeadlineMillis();
    }

    public long getSubmissionDeadlineMillis() {
        return startedAtMillis + durationMillis + submissionGraceMillis;
    }
}
