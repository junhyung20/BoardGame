package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MiniGameServiceTest {
    private final BoardGameService boardGameService = new BoardGameService();
    private final ScoreService scoreService = new ScoreService();
    private final MiniGameService miniGameService = new MiniGameService(boardGameService, scoreService);

    @Test
    public void miniGameCannotStartBeforeRoundEnds() {
        Room room = startedRoom();

        try {
            miniGameService.startMiniGame(room);
            fail("Expected mini game start to require round-end phase");
        } catch (IllegalStateException expected) {
            assertEquals("Current phase is not " + GameState.WAITING_FOR_MINI_GAME, expected.getMessage());
        }
    }

    @Test
    public void miniGameUsesSeparateWaitingAndRunningPhases() {
        Room room = roomWaitingForMiniGame();

        miniGameService.startMiniGame(room);

        assertEquals(GameState.MINI_GAME_RUNNING, room.getGameState().getTurnPhase());
    }

    @Test
    public void finishMiniGameRequiresAllScoresOrDeadline() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host");
        Player guest = roomService.joinRoom(room.getCode(), "uid-2", "Guest");
        String hostId = room.getHostPlayerId();
        roomService.setReady(room.getCode(), hostId, true);
        roomService.setReady(room.getCode(), guest.getId(), true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);
        moveToMiniGame(room);
        miniGameService.startMiniGame(room);
        miniGameService.submitMiniGameScore(room, hostId, 100);

        try {
            miniGameService.finishMiniGame(room);
            fail("Expected mini game to keep accepting missing score");
        } catch (IllegalStateException expected) {
            assertEquals("Mini game is still accepting scores", expected.getMessage());
        }
    }

    @Test
    public void finishMiniGameAppliesRewardsAndAdvancesRound() {
        Room room = roomWaitingForMiniGame();
        String playerId = room.getHostPlayerId();
        miniGameService.startMiniGame(room);
        miniGameService.submitMiniGameScore(room, playerId, 100);

        miniGameService.finishMiniGame(room);

        assertEquals(2, room.getGameState().getCurrentRound());
        assertEquals(GameState.WAITING_FOR_ROLL, room.getGameState().getTurnPhase());
        assertEquals(30, room.getPlayer(playerId).getScore());
        assertNull(room.getMiniGameState());
    }

    @Test
    public void finalMiniGameCompletesMatchAndSetsWinnerMessage() {
        Room room = roomWaitingForMiniGame();
        String playerId = room.getHostPlayerId();

        finishCurrentMiniGame(room, playerId, 100);
        moveToMiniGame(room);
        finishCurrentMiniGame(room, playerId, 100);
        moveToMiniGame(room);
        finishCurrentMiniGame(room, playerId, 100);

        assertEquals(Room.FINISHED, room.getStatus());
        assertEquals(GameState.FINISHED, room.getGameState().getTurnPhase());
        assertTrue(room.getGameState().getLastSystemMessage().contains("Winner: Player 1"));
    }

    @Test
    public void miniGameRejectsOutOfRangeScore() {
        Room room = roomWaitingForMiniGame();
        miniGameService.startMiniGame(room);

        try {
            miniGameService.submitMiniGameScore(room, room.getHostPlayerId(), -1);
            fail("Expected negative score to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Submitted score is out of range", expected.getMessage());
        }
    }

    private Room startedRoom() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getHostPlayerId();
        roomService.setReady(room.getCode(), playerId, true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);
        return room;
    }

    private Room roomWaitingForMiniGame() {
        Room room = startedRoom();
        moveToMiniGame(room);
        return room;
    }

    private void moveToMiniGame(Room room) {
        GameState gameState = room.getGameState();
        int turnsInRound = gameState.getTurnOrder().size();
        for (int i = 0; i < turnsInRound; i++) {
            gameState.transitionTo(GameState.WAITING_FOR_TILE_EFFECT);
            gameState.advanceTurn();
        }
    }

    private void finishCurrentMiniGame(Room room, String playerId, int score) {
        miniGameService.startMiniGame(room);
        miniGameService.submitMiniGameScore(room, playerId, score);
        miniGameService.finishMiniGame(room);
    }
}
