package com.example.boardgame.server;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DevAuthVerifierTest {
    @Test
    public void verifyUsesTokenAsUidWhenProvided() {
        DevAuthVerifier verifier = new DevAuthVerifier();

        assertEquals("dev-user-1", verifier.verify(" dev-user-1 "));
    }

    @Test
    public void verifyCreatesDevUidWhenTokenIsMissing() {
        DevAuthVerifier verifier = new DevAuthVerifier();

        assertEquals("dev-1", verifier.verify(""));
        assertEquals("dev-2", verifier.verify(null));
    }
}
