package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameState {

    public static final String WAITING_FOR_ROLL = "WAITING_FOR_ROLL";
    public static final String WAITING_FOR_TILE_EFFECT = "WAITING_FOR_TILE_EFFECT";
    public static final String WAITING_FOR_MICRO_GAME = "WAITING_FOR_MICRO_GAME";
    public static final String WAITING_FOR_MINI_GAME = "WAITING_FOR_MINI_GAME";
    public static final String MINI_GAME_RUNNING = "MINI_GAME_RUNNING";
    public static final String FINISHED = "FINISHED";

    // Compatibility aliases for older callers/tests during the protocol cleanup.
    public static final String TILE_EFFECT_APPLIED = WAITING_FOR_TILE_EFFECT;
    public static final String MINI_GAME_PHASE = MINI_GAME_RUNNING;

    private final String roomCode;
    private final int finalRound; // 기획서 기준 3
    private int currentRound = 1;
    private int currentPlayerIndex;
    private int lastDiceRoll;
    private String turnPhase = WAITING_FOR_ROLL;

    // 클라이언트 화면에 띄워줄 시스템 메시지
    private String lastSystemMessage = "";

    // 게임에 참여하는 플레이어들의 ID 순서
    private final List<String> turnOrder = new ArrayList<>();

    public GameState(String roomCode, int finalRound) {
        this.roomCode = roomCode;
        this.finalRound = finalRound > 0 ? finalRound : 3;
    }

    public void setTurnOrder(List<String> playerIds) {
        turnOrder.clear();
        if (playerIds != null) {
            turnOrder.addAll(playerIds);
        }
        currentPlayerIndex = 0;
    }

    public String getCurrentPlayerId() {
        if (turnOrder.isEmpty()) {
            return "";
        }
        return turnOrder.get(currentPlayerIndex);
    }

    public void advanceTurn() {
        if (turnOrder.isEmpty()) {
            return;
        }

        if (currentPlayerIndex == turnOrder.size() - 1) {
            currentPlayerIndex = 0;
            transitionTo(WAITING_FOR_MINI_GAME);
        } else {
            currentPlayerIndex++;
            transitionTo(WAITING_FOR_ROLL);
        }
    }

    public void advanceRound() {
        currentRound++;
        if (currentRound > finalRound) {
            transitionTo(FINISHED);
        } else {
            currentPlayerIndex = 0;
            transitionTo(WAITING_FOR_ROLL);
        }
    }

    public void removePlayer(String playerId) {
        int removedIndex = turnOrder.indexOf(playerId);
        if (removedIndex < 0) {
            return;
        }

        boolean removedCurrentPlayer = removedIndex == currentPlayerIndex;
        turnOrder.remove(removedIndex);

        if (turnOrder.isEmpty()) {
            currentPlayerIndex = 0;
            transitionTo(FINISHED);
            return;
        }

        if (removedIndex < currentPlayerIndex) {
            currentPlayerIndex--;
        } else if (currentPlayerIndex >= turnOrder.size()) {
            currentPlayerIndex = 0;
        }

        if (removedCurrentPlayer
                && !WAITING_FOR_MINI_GAME.equals(turnPhase)
                && !MINI_GAME_RUNNING.equals(turnPhase)
                && !FINISHED.equals(turnPhase)) {
            lastDiceRoll = 0;
            transitionTo(WAITING_FOR_ROLL);
            lastSystemMessage = "A player left. Turn skipped.";
        }
    }

    public void transitionTo(String nextPhase) {
        if (!isKnownPhase(nextPhase)) {
            throw new IllegalArgumentException("Unknown game phase: " + nextPhase);
        }
        if (!canTransition(turnPhase, nextPhase)) {
            throw new IllegalStateException("Cannot transition from " + turnPhase + " to " + nextPhase);
        }
        turnPhase = nextPhase;
    }

    private boolean canTransition(String currentPhase, String nextPhase) {
        if (currentPhase.equals(nextPhase) || FINISHED.equals(nextPhase)) {
            return true;
        }
        return switch (currentPhase) {
            case WAITING_FOR_ROLL -> WAITING_FOR_TILE_EFFECT.equals(nextPhase);
            case WAITING_FOR_TILE_EFFECT -> WAITING_FOR_ROLL.equals(nextPhase)
                    || WAITING_FOR_MICRO_GAME.equals(nextPhase)
                    || WAITING_FOR_MINI_GAME.equals(nextPhase);
            case WAITING_FOR_MICRO_GAME -> WAITING_FOR_ROLL.equals(nextPhase)
                    || WAITING_FOR_MINI_GAME.equals(nextPhase);
            case WAITING_FOR_MINI_GAME -> MINI_GAME_RUNNING.equals(nextPhase);
            case MINI_GAME_RUNNING -> WAITING_FOR_ROLL.equals(nextPhase);
            default -> false;
        };
    }

    private boolean isKnownPhase(String phase) {
        return WAITING_FOR_ROLL.equals(phase)
                || WAITING_FOR_TILE_EFFECT.equals(phase)
                || WAITING_FOR_MICRO_GAME.equals(phase)
                || WAITING_FOR_MINI_GAME.equals(phase)
                || MINI_GAME_RUNNING.equals(phase)
                || FINISHED.equals(phase);
    }

    public GameSnapshot toSnapshot() {
        return new GameSnapshot(
                roomCode,
                currentRound,
                finalRound,
                getCurrentPlayerId(),
                lastDiceRoll,
                turnPhase,
                turnOrder,
                lastSystemMessage // 새로 추가된 메시지 전송
        );
    }

    // --- Getters & Setters ---

    public String getRoomCode() { return roomCode; }
    public int getCurrentRound() { return currentRound; }
    public int getFinalRound() { return finalRound; }

    public int getLastDiceRoll() { return lastDiceRoll; }
    public void setLastDiceRoll(int lastDiceRoll) { this.lastDiceRoll = lastDiceRoll; }

    public String getTurnPhase() { return turnPhase; }
    public void setTurnPhase(String turnPhase) { transitionTo(turnPhase); }

    public List<String> getTurnOrder() { return Collections.unmodifiableList(turnOrder); }

    public String getLastSystemMessage() { return lastSystemMessage; }
    public void setLastSystemMessage(String lastSystemMessage) { this.lastSystemMessage = lastSystemMessage; }
}
