package com.example.boardgame.server;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

class FirebaseAdminAuthVerifier implements AuthVerifier {
    static final String SERVICE_ACCOUNT_ENV = "FIREBASE_SERVICE_ACCOUNT";
    static final String GOOGLE_APPLICATION_CREDENTIALS_ENV = "GOOGLE_APPLICATION_CREDENTIALS";

    private final TokenVerifier tokenVerifier;

    FirebaseAdminAuthVerifier() {
        this(createTokenVerifier());
    }

    FirebaseAdminAuthVerifier(TokenVerifier tokenVerifier) {
        this.tokenVerifier = tokenVerifier;
    }

    @Override
    public String verify(String firebaseIdToken) {
        String token = firebaseIdToken == null ? "" : firebaseIdToken.trim();
        if (token.isEmpty()) {
            throw new AuthException("Missing Firebase ID token");
        }

        try {
            String uid = tokenVerifier.verify(token);
            if (uid == null || uid.trim().isEmpty()) {
                throw new AuthException("Firebase token did not contain a UID");
            }
            return uid;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthException("Invalid Firebase ID token", e);
        }
    }

    private static TokenVerifier createTokenVerifier() {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance(firebaseApp());
        return token -> firebaseAuth.verifyIdToken(token, true).getUid();
    }

    private static synchronized FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials())
                    .build();
            return FirebaseApp.initializeApp(options);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Firebase credentials are not configured. Set FIREBASE_SERVICE_ACCOUNT or GOOGLE_APPLICATION_CREDENTIALS.",
                    e
            );
        }
    }

    private static GoogleCredentials credentials() throws IOException {
        String serviceAccountPath = System.getenv(SERVICE_ACCOUNT_ENV);
        if (serviceAccountPath == null || serviceAccountPath.trim().isEmpty()) {
            return GoogleCredentials.getApplicationDefault();
        }

        try (InputStream inputStream = new FileInputStream(serviceAccountPath.trim())) {
            return GoogleCredentials.fromStream(inputStream);
        }
    }

    interface TokenVerifier {
        String verify(String firebaseIdToken) throws Exception;
    }
}
