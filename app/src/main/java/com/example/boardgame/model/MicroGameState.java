package com.example.boardgame.model;

import java.util.HashMap;
import java.util.Map;

public class MicroGameState {
    public enum Status {
        WAITING,
        RUNNING,
        FINISHED
    }

    private String roomCode;
    private String microGameId;
    private int round;
    private Status status;
    private int durationSeconds;
    private long startedAtMillis;
    private long endsAtMillis;
    private Map<String, Integer> submittedScores;

    public MicroGameState() {
        this("", "", 1, 0);
    }

    public MicroGameState(String roomCode, String microGameId, int round, int durationSeconds) {
        this.roomCode = roomCode;
        this.microGameId = microGameId;
        this.round = round;
        this.status = Status.WAITING;
        this.durationSeconds = durationSeconds;
        this.startedAtMillis = 0L;
        this.endsAtMillis = 0L;
        this.submittedScores = new HashMap<>();
    }

    public void start(long startedAtMillis) {
        this.status = Status.RUNNING;
        this.startedAtMillis = startedAtMillis;
        this.endsAtMillis = startedAtMillis + durationSeconds * 1000L;
    }

    public void submitScore(String playerId, int score) {
        submittedScores.put(playerId, score);
    }

    public boolean hasSubmitted(String playerId) {
        return submittedScores.containsKey(playerId);
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public String getMicroGameId() {
        return microGameId;
    }

    public void setMicroGameId(String microGameId) {
        this.microGameId = microGameId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public long getStartedAtMillis() {
        return startedAtMillis;
    }

    public void setStartedAtMillis(long startedAtMillis) {
        this.startedAtMillis = startedAtMillis;
    }

    public long getEndsAtMillis() {
        return endsAtMillis;
    }

    public void setEndsAtMillis(long endsAtMillis) {
        this.endsAtMillis = endsAtMillis;
    }

    public Map<String, Integer> getSubmittedScores() {
        return submittedScores;
    }

    public void setSubmittedScores(Map<String, Integer> submittedScores) {
        this.submittedScores = submittedScores == null ? new HashMap<>() : submittedScores;
    }
}
