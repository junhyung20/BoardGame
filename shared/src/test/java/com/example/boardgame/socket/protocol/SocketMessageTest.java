package com.example.boardgame.socket.protocol;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SocketMessageTest {
    @Test
    public void serializesAndParsesJsonWireText() {
        SocketMessage original = SocketMessage.builder(MessageTypes.CREATE_ROOM)
                .requestId("request-1")
                .put("nickname", "Player One")
                .put("score", 12)
                .put("serverTimeMillis", 123456789L)
                .put("ready", true)
                .put("symbols", "a&b=c")
                .build();

        String wireText = original.toWireText();
        SocketMessage parsed = SocketMessage.parse(wireText);

        assertEquals(MessageTypes.CREATE_ROOM, parsed.getType());
        assertEquals("request-1", parsed.getRequestId());
        assertEquals("Player One", parsed.getOrDefault("nickname", ""));
        assertEquals(12, parsed.getInt("score", 0));
        assertEquals("123456789", parsed.getOrDefault("serverTimeMillis", ""));
        assertTrue(parsed.getBoolean("ready", false));
        assertEquals("a&b=c", parsed.getOrDefault("symbols", ""));
        assertFalse(wireText.contains("type=CREATE_ROOM"));
    }

    @Test
    public void parsesJsonWithFieldsObject() {
        SocketMessage message = SocketMessage.parse("{"
                + "\"type\":\"REQUEST_ERROR\","
                + "\"requestId\":\"request-2\","
                + "\"fields\":{\"errorCode\":\"UNAUTHENTICATED\",\"details\":\"Missing token\"}"
                + "}");

        assertEquals(MessageTypes.REQUEST_ERROR, message.getType());
        assertEquals("request-2", message.getRequestId());
        assertEquals("UNAUTHENTICATED", message.getOrDefault("errorCode", ""));
        assertEquals("Missing token", message.getOrDefault("details", ""));
    }
}
