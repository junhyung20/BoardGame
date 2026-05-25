package com.example.boardgame.server;

import com.example.boardgame.socket.protocol.ErrorCodes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServerErrorMapperTest {
    @Test
    public void mapsKnownRoomErrorsToStableCodes() {
        assertError(ErrorCodes.ROOM_NOT_FOUND, new IllegalArgumentException("Room not found"));
        assertError(ErrorCodes.ROOM_FULL, new IllegalStateException("Room is full"));
        assertError(ErrorCodes.INVALID_ROOM_PASSWORD, new IllegalArgumentException("Invalid room password"));
        assertError(ErrorCodes.NOT_HOST, new IllegalStateException("Only the host can start the game"));
    }

    @Test
    public void mapsKnownGameErrorsToStableCodes() {
        assertError(ErrorCodes.NOT_YOUR_TURN, new IllegalStateException("It is not your turn"));
        assertError(ErrorCodes.INVALID_PHASE, new IllegalStateException("Current phase is not WAITING_FOR_ROLL"));
        assertError(ErrorCodes.INVALID_SCORE, new IllegalArgumentException("Submitted score is out of range"));
        assertError(ErrorCodes.SCORE_ALREADY_SUBMITTED,
                new IllegalStateException("Mini game score has already been submitted"));
    }

    @Test
    public void hidesUnknownExceptionDetails() {
        ServerError error = ServerErrorMapper.from(new IllegalArgumentException("raw internal detail"));

        assertEquals(ErrorCodes.BAD_REQUEST, error.code());
        assertEquals("Invalid request", error.details());
    }

    @Test
    public void mapsAuthenticationError() {
        ServerError error = ServerErrorMapper.from(new AuthException("token expired"));

        assertEquals(ErrorCodes.UNAUTHENTICATED, error.code());
        assertEquals("Authentication required", error.details());
    }

    private void assertError(String expectedCode, RuntimeException exception) {
        assertEquals(expectedCode, ServerErrorMapper.from(exception).code());
    }
}
