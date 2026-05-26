package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Room;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BoardGameServiceTest {
    @Test
    public void startGameUsesConfiguredMinimumPlayers() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();

        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        assertEquals(Room.IN_GAME, room.getStatus());
    }

    @Test
    public void startGameRejectsWhenConfiguredMinimumIsNotMet() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();

        roomService.setReady(room.getCode(), playerId, true);
        try {
            boardGameService.startGame(room, 2);
            fail("Expected start to require two players");
        } catch (IllegalStateException expected) {
            assertEquals("At least 2 ready player(s) are required to start", expected.getMessage());
        }
    }

    @Test
    public void rollAndTileEffectFollowFiniteStateMachine() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();
        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        try {
            boardGameService.applyTileEffect(room, playerId);
            fail("Expected tile effect before roll to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("Current phase is not " + GameState.WAITING_FOR_TILE_EFFECT,
                    expected.getMessage());
        }

        int diceRoll = boardGameService.rollDice(room, playerId);

        assertTrue(diceRoll >= 1 && diceRoll <= 6);
        assertEquals(GameState.WAITING_FOR_TILE_EFFECT,
                room.getGameState().getTurnPhase());
    }

    @Test
    public void rollDiceCanUseValidatedClientResult() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();
        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        int diceRoll = boardGameService.rollDice(room, playerId, 4);

        assertEquals(4, diceRoll);
        assertEquals(4, room.getGameState().getLastDiceRoll());
        assertEquals(4, room.getPlayer(playerId).getPosition());
    }

    @Test
    public void rollDiceRejectsInvalidClientResult() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();
        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        try {
            boardGameService.rollDice(room, playerId, 7);
            fail("Expected invalid dice result to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Dice roll must be between 1 and 6", expected.getMessage());
        }
    }

    @Test
    public void gameCannotStartTwice() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();
        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        try {
            boardGameService.startGame(room, RoomService.MIN_PLAYERS);
            fail("Expected duplicate start to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("Game has already started", expected.getMessage());
        }
    }
}
