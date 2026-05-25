package com.example.boardgame.server;

public interface AuthVerifier {
    String verify(String firebaseIdToken);
}
