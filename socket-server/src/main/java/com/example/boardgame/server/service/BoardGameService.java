package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class BoardGameService {
    public static final int BOARD_SIZE = 16;
    public static final int FINAL_ROUND = 3;

    // 타일 종류 상수 정의 (기획서 매핑용)
    public static final String TILE_START = "START";
    public static final String TILE_PLUS_SCORE = "PLUS_SCORE";
    public static final String TILE_MINUS_SCORE = "MINUS_SCORE";
    public static final String TILE_CARD = "CARD";
    public static final String TILE_QUESTION = "QUESTION";
    public static final String TILE_AD = "AD";
    public static final String TILE_NORMAL = "NORMAL";

    // 카드 종류 상수
    public static final String CARD_DEFENSE = "DEFENSE_CARD";

    private final SecureRandom random = new SecureRandom();

    public void startGame(Room room, int minPlayers) {
        if (!Room.WAITING.equals(room.getStatus()) && !Room.READY.equals(room.getStatus())) {
            throw new IllegalStateException("Game has already started");
        }
        if (room.getGameState() != null) {
            throw new IllegalStateException("Game has already started");
        }
        if (!room.canStart(minPlayers)) {
            throw new IllegalStateException("At least " + minPlayers + " ready player(s) are required to start");
        }

        GameState gameState = new GameState(room.getCode(), FINAL_ROUND);
        List<String> turnOrder = new ArrayList<>();
        for (Player player : room.getPlayerList()) {
            turnOrder.add(player.getId());
        }
        gameState.setTurnOrder(turnOrder);
        gameState.setLastSystemMessage("Game started.");
        room.setGameState(gameState);
        room.setStatus(Room.IN_GAME);
    }

    public int rollDice(Room room, String playerId) {
        return rollDice(room, playerId, 0);
    }

    public int rollDice(Room room, String playerId, int requestedDiceRoll) {
        GameState gameState = requireGameState(room);
        requireCurrentPlayer(gameState, playerId);
        requirePhase(gameState, GameState.WAITING_FOR_ROLL);

        Player player = requirePlayer(room, playerId);

        int diceRoll = requestedDiceRoll > 0 ? requestedDiceRoll : 1 + random.nextInt(6);
        if (diceRoll < 1 || diceRoll > 6) {
            throw new IllegalArgumentException("Dice roll must be between 1 and 6");
        }
        player.moveBy(diceRoll, BOARD_SIZE);
        player.addScore(diceRoll);

        gameState.setLastDiceRoll(diceRoll);
        gameState.transitionTo(GameState.WAITING_FOR_TILE_EFFECT);
        gameState.setLastSystemMessage(player.getNickname() + " rolled " + diceRoll + ".");
        room.touch();

        return diceRoll;
    }

    public String applyTileEffect(Room room, String playerId) {
        GameState gameState = requireGameState(room);
        requireCurrentPlayer(gameState, playerId);
        requirePhase(gameState, GameState.WAITING_FOR_TILE_EFFECT);

        Player player = requirePlayer(room, playerId);
        String tileType = getTileType(player.getPosition());

        switch (tileType) {
            case TILE_START:
                player.addScore(5);
                gameState.setLastSystemMessage(player.getNickname() + " gained 5 points on START.");
                gameState.advanceTurn();
                break;
            case TILE_PLUS_SCORE:
                player.addScore(3);
                gameState.setLastSystemMessage(player.getNickname() + " gained 3 points.");
                gameState.advanceTurn();
                break;
            case TILE_MINUS_SCORE:
                if (player.hasItemCard(CARD_DEFENSE)) {
                    player.useItemCard(CARD_DEFENSE);
                    gameState.setLastSystemMessage(player.getNickname() + " used a defense card.");
                } else {
                    player.addScore(-3);
                    gameState.setLastSystemMessage(player.getNickname() + " lost 3 points.");
                }
                gameState.advanceTurn();
                break;
            case TILE_CARD:
                if (player.addItemCard(CARD_DEFENSE)) {
                    gameState.setLastSystemMessage(player.getNickname() + " gained a defense card.");
                } else {
                    gameState.setLastSystemMessage(player.getNickname() + " already has a card.");
                }
                gameState.advanceTurn();
                break;
            case TILE_QUESTION:
                int randomScore = random.nextBoolean() ? 5 : -5;
                player.addScore(randomScore);
                gameState.setLastSystemMessage(player.getNickname() + " question result: " + randomScore + " points.");
                gameState.advanceTurn();
                break;
            case TILE_AD:
                gameState.setLastSystemMessage(player.getNickname() + " started a micro game.");
                gameState.transitionTo(GameState.WAITING_FOR_MICRO_GAME);
                break;
            default:
                gameState.setLastSystemMessage(player.getNickname() + " landed on a normal tile.");
                gameState.advanceTurn();
                break;
        }

        room.touch();
        return tileType;
    }

    public String getTileType(int position) {
        return switch (position) {
            case 0 -> TILE_START;
            case 1, 3, 5, 11, 15 -> TILE_PLUS_SCORE;
            case 7, 9, 13 -> TILE_MINUS_SCORE;
            case 2, 10 -> TILE_CARD;
            case 4, 8, 12 -> TILE_QUESTION;
            case 6, 14 -> TILE_AD;
            default -> TILE_NORMAL;
        };
    }

    public GameState requireGameState(Room room) {
        GameState gameState = room.getGameState();
        if (gameState == null) {
            throw new IllegalStateException("Game has not started");
        }
        return gameState;
    }

    public Player requirePlayer(Room room, String playerId) {
        Player player = room.getPlayers().get(playerId);
        if (player == null) {
            throw new IllegalArgumentException("Player not found in room");
        }
        return player;
    }

    public void requirePhase(GameState gameState, String expectedPhase) {
        if (!expectedPhase.equals(gameState.getTurnPhase())) {
            throw new IllegalStateException("Current phase is not " + expectedPhase);
        }
    }

    private void requireCurrentPlayer(GameState gameState, String playerId) {
        if (!playerId.equals(gameState.getCurrentPlayerId())) {
            throw new IllegalStateException("It is not your turn");
        }
    }
}
