package com.example.boardgame.server.service;

import com.example.boardgame.server.model.GameState;
import com.example.boardgame.server.model.MicroGameState;
import com.example.boardgame.server.model.MiniGameState;
import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RoomServiceTest {
    @Test
    public void sameUidCannotCreateTwoRooms() {
        RoomService roomService = new RoomService();
        roomService.createRoom("uid-1", "Player 1");

        try {
            roomService.createRoom("uid-1", "Player 1 again");
            fail("Expected duplicate UID to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("User is already in a room", expected.getMessage());
        }
    }

    @Test
    public void sameUidCannotJoinAnotherRoom() {
        RoomService roomService = new RoomService();
        roomService.createRoom("uid-1", "Player 1");
        Room secondRoom = roomService.createRoom("uid-2", "Player 2");

        try {
            roomService.joinRoom(secondRoom.getCode(), "uid-1", "Player 1 again");
            fail("Expected duplicate UID to be rejected");
        } catch (IllegalStateException expected) {
            assertEquals("User is already in a room", expected.getMessage());
        }
    }

    @Test
    public void uidCanJoinAfterDisconnect() {
        RoomService roomService = new RoomService();
        Room firstRoom = roomService.createRoom("uid-1", "Player 1");
        String playerId = firstRoom.getPlayerList().iterator().next().getId();
        roomService.disconnect(firstRoom.getCode(), playerId);

        Room secondRoom = roomService.createRoom("uid-2", "Player 2");
        roomService.joinRoom(secondRoom.getCode(), "uid-1", "Player 1 again");

        assertEquals(2, secondRoom.getPlayers().size());
    }

    @Test
    public void disconnectRemovesRoomWhenLastPlayerLeaves() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Player 1");
        String playerId = room.getPlayerList().iterator().next().getId();

        roomService.disconnect(room.getCode(), playerId);

        try {
            roomService.requireRoom(room.getCode());
            fail("Expected empty room to be removed");
        } catch (IllegalArgumentException expected) {
            assertEquals("Room not found", expected.getMessage());
        }
    }

    @Test
    public void disconnectHostPromotesNextPlayer() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host");
        String originalHostId = room.getHostPlayerId();
        Player secondPlayer = roomService.joinRoom(room.getCode(), "uid-2", "Guest");

        roomService.disconnect(room.getCode(), originalHostId);

        assertEquals(secondPlayer.getId(), room.getHostPlayerId());
        assertEquals(1, room.getPlayers().size());
        assertFalse(room.getPlayers().containsKey(originalHostId));
    }

    @Test
    public void disconnectCurrentPlayerInGameSkipsToNextPlayer() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Host");
        String firstPlayerId = room.getHostPlayerId();
        Player secondPlayer = roomService.joinRoom(room.getCode(), "uid-2", "Guest");
        roomService.setReady(room.getCode(), firstPlayerId, true);
        roomService.setReady(room.getCode(), secondPlayer.getId(), true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);

        roomService.disconnect(room.getCode(), firstPlayerId);

        assertEquals(Room.IN_GAME, room.getStatus());
        assertEquals(secondPlayer.getId(), room.getGameState().getCurrentPlayerId());
        assertEquals(GameState.WAITING_FOR_ROLL, room.getGameState().getTurnPhase());
        assertEquals(1, room.getGameState().getTurnOrder().size());
        assertEquals(secondPlayer.getId(), room.getHostPlayerId());
    }

    @Test
    public void disconnectPlayerInMiniGameRemovesSubmittedScore() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host");
        Player secondPlayer = roomService.joinRoom(room.getCode(), "uid-2", "Guest");
        room.setMiniGameState(new MiniGameState(
                System.currentTimeMillis(),
                30_000,
                3_000
        ));
        room.getMiniGameState().submitScore(secondPlayer.getId(), 100);

        roomService.disconnect(room.getCode(), secondPlayer.getId());

        assertFalse(room.getMiniGameState().getScoresByPlayerId().containsKey(secondPlayer.getId()));
    }

    @Test
    public void disconnectPlayerInMicroGameClearsMicroGameState() {
        RoomService roomService = new RoomService();
        BoardGameService boardGameService = new BoardGameService();
        Room room = roomService.createRoom("uid-1", "Host");
        String firstPlayerId = room.getHostPlayerId();
        Player secondPlayer = roomService.joinRoom(room.getCode(), "uid-2", "Guest");
        roomService.setReady(room.getCode(), firstPlayerId, true);
        roomService.setReady(room.getCode(), secondPlayer.getId(), true);
        boardGameService.startGame(room, RoomService.MIN_PLAYERS);
        room.setMicroGameState(new MicroGameState(firstPlayerId, System.currentTimeMillis(), 10_000, 2_000));

        roomService.disconnect(room.getCode(), firstPlayerId);

        assertNull(room.getMicroGameState());
        assertEquals(secondPlayer.getId(), room.getGameState().getCurrentPlayerId());
        assertEquals(GameState.WAITING_FOR_ROLL, room.getGameState().getTurnPhase());
    }

    @Test
    public void blankPasswordRoomCanBeJoinedWithoutPassword() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host", "");

        roomService.joinRoom(room.getCode(), "uid-2", "Guest", "");

        assertEquals(2, room.getPlayers().size());
        assertFalse(room.hasPassword());
    }

    @Test
    public void passwordRoomRejectsMissingOrWrongPassword() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host", "secret");

        try {
            roomService.joinRoom(room.getCode(), "uid-2", "Guest", "");
            fail("Expected missing password to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid room password", expected.getMessage());
        }

        try {
            roomService.joinRoom(room.getCode(), "uid-2", "Guest", "wrong");
            fail("Expected wrong password to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Invalid room password", expected.getMessage());
        }
    }

    @Test
    public void passwordRoomAcceptsCorrectPassword() {
        RoomService roomService = new RoomService();
        Room room = roomService.createRoom("uid-1", "Host", "secret");

        roomService.joinRoom(room.getCode(), "uid-2", "Guest", "secret");

        assertEquals(2, room.getPlayers().size());
        assertTrue(room.hasPassword());
    }

    @Test
    public void matchmakeSkipsPasswordRooms() {
        RoomService roomService = new RoomService();
        Room lockedRoom = roomService.createRoom("uid-1", "Host", "secret");

        RoomService.MatchResult matchResult = roomService.matchmake("uid-2", "Guest");

        assertFalse(lockedRoom.getCode().equals(matchResult.getRoom().getCode()));
        assertEquals(1, lockedRoom.getPlayers().size());
        assertEquals(1, matchResult.getRoom().getPlayers().size());
    }
}
