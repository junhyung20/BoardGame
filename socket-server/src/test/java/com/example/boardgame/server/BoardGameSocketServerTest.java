package com.example.boardgame.server;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BoardGameSocketServerTest {
    @Test
    public void parsesValidPort() {
        assertEquals(8080, BoardGameSocketServer.parsePort("8080"));
    }

    @Test
    public void rejectsZeroPort() {
        try {
            BoardGameSocketServer.parsePort("0");
            fail("Expected invalid port to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Port must be between 1 and 65535", expected.getMessage());
        }
    }

    @Test
    public void rejectsTooLargePort() {
        try {
            BoardGameSocketServer.parsePort("65536");
            fail("Expected invalid port to be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals("Port must be between 1 and 65535", expected.getMessage());
        }
    }

    @Test
    public void defaultNetworkBindsForLan() {
        assertEquals("0.0.0.0", BoardGameSocketServer.resolveBindHost(new HashMap<>()));
    }

    @Test
    public void wanNetworkBindsForLocalNgrokTunnel() {
        Map<String, String> env = new HashMap<>();
        env.put("BOARDGAME_NETWORK", "WAN");

        assertEquals("127.0.0.1", BoardGameSocketServer.resolveBindHost(env));
    }

    @Test
    public void firebaseAuthIsConfiguredByServiceAccountPath() {
        Map<String, String> env = new HashMap<>();
        env.put("FIREBASE_SERVICE_ACCOUNT", "/tmp/my-renamed-service-account.json");

        assertTrue(BoardGameSocketServer.isFirebaseAuthConfigured(env));
    }

    @Test
    public void firebaseAuthIsConfiguredByGoogleApplicationCredentialsPath() {
        Map<String, String> env = new HashMap<>();
        env.put("GOOGLE_APPLICATION_CREDENTIALS", "/tmp/my-renamed-service-account.json");

        assertTrue(BoardGameSocketServer.isFirebaseAuthConfigured(env));
    }

    @Test
    public void missingCredentialPathUsesDevAuth() {
        assertFalse(BoardGameSocketServer.isFirebaseAuthConfigured(new HashMap<>()));
    }
}
