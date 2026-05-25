package com.example.boardgame.server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FirebaseAdminAuthVerifierTest {
    @Test
    public void verifyReturnsUidForValidToken() {
        FirebaseAdminAuthVerifier verifier = new FirebaseAdminAuthVerifier(token -> "uid-1");

        assertEquals("uid-1", verifier.verify("valid-token"));
    }

    @Test
    public void verifyRejectsMissingToken() {
        FirebaseAdminAuthVerifier verifier = new FirebaseAdminAuthVerifier(token -> "uid-1");

        try {
            verifier.verify("");
            fail("Expected missing token to be rejected");
        } catch (AuthException expected) {
            assertEquals("Missing Firebase ID token", expected.getMessage());
        }
    }

    @Test
    public void verifyRejectsInvalidToken() {
        FirebaseAdminAuthVerifier verifier = new FirebaseAdminAuthVerifier(token -> {
            throw new IllegalArgumentException("bad token");
        });

        try {
            verifier.verify("invalid-token");
            fail("Expected invalid token to be rejected");
        } catch (AuthException expected) {
            assertEquals("Invalid Firebase ID token", expected.getMessage());
        }
    }
}
