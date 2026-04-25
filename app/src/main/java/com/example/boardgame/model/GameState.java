package com.example.boardgame.model;

import com.example.boardgame.util.Constants;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    public enum TurnPhase {
        WAITING_FOR_ROLL,
        MOVING,
        TILE_EFFECT,
        MINI_GAME,
        MICRO_GAME,
        ROUND_END,
        FINISHED
    }

    private String roomCode;
    private int currentRound;
    private int finalRound;
    private int currentPlayerIndex;
    private int lastDiceRoll;
    private TurnPhase turnPhase;
    private List<String> turnOrder;

    public GameState() {
        this("");
    }

    public GameState(String roomCode) {
        this.roomCode = roomCode;
        this.currentRound = 1;
        this.finalRound = Constants.FINAL_ROUND;
        this.currentPlayerIndex = 0;
        this.lastDiceRoll = 0;
        this.turnPhase = TurnPhase.WAITING_FOR_ROLL;
        this.turnOrder = new ArrayList<>();
    }

    public String getCurrentPlayerId() {
        if (turnOrder == null || turnOrder.isEmpty()) {
            return null;
        }
        return turnOrder.get(currentPlayerIndex);
    }

    public boolean isLastPlayerTurn() {
        return turnOrder != null && !turnOrder.isEmpty() && currentPlayerIndex == turnOrder.size() - 1;
    }

    public void advanceTurn() {
        if (turnOrder == null || turnOrder.isEmpty()) {
            return;
        }
        if (isLastPlayerTurn()) {
            currentPlayerIndex = 0;
            turnPhase = TurnPhase.ROUND_END;
        } else {
            currentPlayerIndex++;
            turnPhase = TurnPhase.WAITING_FOR_ROLL;
        }
    }

    public void advanceRound() {
        currentRound++;
        currentPlayerIndex = 0;
        turnPhase = currentRound > finalRound ? TurnPhase.FINISHED : TurnPhase.WAITING_FOR_ROLL;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getFinalRound() {
        return finalRound;
    }

    public void setFinalRound(int finalRound) {
        this.finalRound = finalRound;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public int getLastDiceRoll() {
        return lastDiceRoll;
    }

    public void setLastDiceRoll(int lastDiceRoll) {
        this.lastDiceRoll = lastDiceRoll;
    }

    public TurnPhase getTurnPhase() {
        return turnPhase;
    }

    public void setTurnPhase(TurnPhase turnPhase) {
        this.turnPhase = turnPhase;
    }

    public List<String> getTurnOrder() {
        return turnOrder;
    }

    public void setTurnOrder(List<String> turnOrder) {
        this.turnOrder = turnOrder == null ? new ArrayList<>() : turnOrder;
    }
}
