package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MicroGameState;
import com.example.boardgame.server.model.Room;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class MicroGameServiceTest {
    private final BoardGameService boardGameService = new BoardGameService();
    private final MicroGameService microGameService = new MicroGameService(boardGameService);

    @Test
    public void microGameRequiresMicroGamePhase() {
        Room room = startedRoom();

        try {
            microGameService.startMicroGame(room, room.getHostPlayerId());
            fail("Expected micro game start to require micro-game phase");
        } catch (IllegalStateException expected) {
            assertEquals("Current phase is not " + GameState.WAITING_FOR_MICRO_GAME, expected.getMessage());
        }
    }

    @Test
    public void microGameScoreAdvancesTurn() {
        Room room = roomWaitingForMicroGame();
        String playerId = room.getHostPlayerId();
        microGameService.startMicroGame(room, playerId);

        microGameService.submitMicroGameScore(room, playerId, 50);

        assertEquals(50, room.getPlayer(playerId).getScore());
        assertFalse(room.getPlayer(playerId).isInMicroGame());
        assertEquals(GameState.WAITING_FOR_MINI_GAME, room.getGameState().getTurnPhase());
    }

    @Test
    public void lateMicroGameScoreIsIgnoredButResolvesTurn() {
        Room room = roomWaitingForMicroGame();
        String playerId = room.getHostPlayerId();
        room.getPlayer(playerId).setInMicroGame(true);
        room.setMicroGameState(new MicroGameState(
                playerId,
                System.currentTimeMillis() - 20_000,
                MicroGameService.MICRO_GAME_DURATION_MILLIS,
                MicroGameService.SUBMISSION_GRACE_MILLIS
        ));

        microGameService.submitMicroGameScore(room, playerId, 50);

        assertEquals(0, room.getPlayer(playerId).getScore());
        assertFalse(room.getPlayer(playerId).isInMicroGame());
        assertEquals(GameState.WAITING_FOR_MINI_GAME, room.getGameState().getTurnPhase());
    }

    @Test
    public void microGameRejectsOutOfRangeScore() {
        Room room = roomWaitingForMicroGame();
        microGameService.startMicroGame(room, room.getHostPlayerId());

        try {
            microGameService.submitMicroGameScore(room, room.getHostPlayerId(), -1);
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

    private Room roomWaitingForMicroGame() {
        Room room = startedRoom();
        room.getGameState().transitionTo(GameState.WAITING_FOR_TILE_EFFECT);
        room.getGameState().transitionTo(GameState.WAITING_FOR_MICRO_GAME);
        return room;
    }
}
