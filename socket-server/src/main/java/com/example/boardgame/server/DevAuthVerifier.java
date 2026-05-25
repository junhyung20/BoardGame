package com.example.boardgame.server;

import java.util.concurrent.atomic.AtomicInteger;

class DevAuthVerifier implements AuthVerifier {
    private static final String DEV_UID_PREFIX = "dev-";
    private final AtomicInteger nextUid = new AtomicInteger(1);

    @Override
    public String verify(String firebaseIdToken) {
        String token = firebaseIdToken == null ? "" : firebaseIdToken.trim();
        if (!token.isEmpty()) {
            return token;
        }
        return DEV_UID_PREFIX + nextUid.getAndIncrement();
    }
}
