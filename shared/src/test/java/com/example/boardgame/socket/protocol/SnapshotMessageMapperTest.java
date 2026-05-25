package com.example.boardgame.socket.protocol;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SnapshotMessageMapperTest {
    @Test
    public void roomSnapshotUsesNestedJsonPlayers() {
        RoomSnapshot snapshot = new RoomSnapshot(
                "ABCD",
                "player-1",
                "WAITING",
                Arrays.asList(
                        new PlayerSnapshot("player-1", "Host", 10, 3, true, true, false, Collections.emptyList()),
                        new PlayerSnapshot("player-2", "Guest", 4, 1, false, false, false, Collections.emptyList())
                )
        );

        SocketMessage message = SnapshotMessageMapper.roomUpdated(snapshot);
        String wireText = message.toWireText();
        RoomSnapshot parsed = SnapshotMessageMapper.toRoomSnapshot(SocketMessage.parse(wireText));

        assertTrue(wireText.contains("\"room\""));
        assertTrue(wireText.contains("\"players\""));
        assertEquals("ABCD", parsed.getCode());
        assertEquals("player-1", parsed.getHostPlayerId());
        assertEquals("WAITING", parsed.getStatus());
        assertEquals(2, parsed.getPlayers().size());
        assertEquals("Guest", parsed.getPlayers().get(1).getNickname());
        assertEquals(4, parsed.getPlayers().get(1).getScore());
    }

    @Test
    public void gameSnapshotUsesNestedJsonTurnOrder() {
        GameSnapshot snapshot = new GameSnapshot(
                "ABCD",
                2,
                5,
                "player-2",
                6,
                "WAITING_FOR_TILE",
                Arrays.asList("player-1", "player-2"),
                ""
        );

        SocketMessage message = SnapshotMessageMapper.gameUpdated(snapshot);
        String wireText = message.toWireText();
        GameSnapshot parsed = SnapshotMessageMapper.toGameSnapshot(SocketMessage.parse(wireText));

        assertTrue(wireText.contains("\"game\""));
        assertTrue(wireText.contains("\"turnOrder\""));
        assertEquals("ABCD", parsed.getRoomCode());
        assertEquals(2, parsed.getCurrentRound());
        assertEquals(5, parsed.getFinalRound());
        assertEquals("player-2", parsed.getCurrentPlayerId());
        assertEquals(6, parsed.getLastDiceRoll());
        assertEquals("WAITING_FOR_TILE", parsed.getTurnPhase());
        assertEquals(Arrays.asList("player-1", "player-2"), parsed.getTurnOrder());
    }

    @Test
    public void lobbySnapshotUsesRoomListWithPasswordFlagOnly() {
        LobbySnapshot snapshot = new LobbySnapshot(
                Arrays.asList(
                        new LobbySnapshot.RoomListInfo("123456", "WAITING", 1, "Host", true)
                )
        );

        SocketMessage message = SnapshotMessageMapper.lobbyUpdated(snapshot);
        String wireText = message.toWireText();
        LobbySnapshot parsed = SnapshotMessageMapper.toLobbySnapshot(SocketMessage.parse(wireText));

        assertTrue(wireText.contains("\"lobby\""));
        assertTrue(wireText.contains("\"hasPassword\":true"));
        assertFalse(wireText.contains("passwordHash"));
        assertFalse(wireText.contains("passwordSalt"));
        assertEquals(1, parsed.getRooms().size());
        assertEquals("123456", parsed.getRooms().get(0).getCode());
        assertTrue(parsed.getRooms().get(0).hasPassword());
    }
}
