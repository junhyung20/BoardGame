package com.example.boardgame.model;

import java.util.HashMap;
import java.util.Map;

public class MiniGameState {
    public enum Status {
        WAITING,
        RUNNING,
        FINISHED
    }

    public enum Type {
        COLOR_GUESSING,
        PASSWORD_GUESSING,
        PHONE_TILT_MAZE
    }

    private String roomCode;
    private String miniGameId;
    private int round;
    private Type type;
    private Status status;
    private int durationSeconds;
    private long startedAtMillis;
    private long endsAtMillis;
    private Map<String, Integer> submittedScores;

    public MiniGameState() {
        this("", "", 1, Type.COLOR_GUESSING, 0);
    }

    public MiniGameState(String roomCode, String miniGameId, int round, Type type, int durationSeconds) {
        this.roomCode = roomCode;
        this.miniGameId = miniGameId;
        this.round = round;
        this.type = type;
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

    public String getMiniGameId() {
        return miniGameId;
    }

    public void setMiniGameId(String miniGameId) {
        this.miniGameId = miniGameId;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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
