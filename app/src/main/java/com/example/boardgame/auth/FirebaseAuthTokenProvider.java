package com.example.boardgame.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthTokenProvider {

    public interface TokenCallback {
        void onToken(String idToken);
        void onError(Exception exception);
    }

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthTokenProvider() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseAuthTokenProvider(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    public void requireIdToken(TokenCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user == null) {
            firebaseAuth.signInAnonymously()
                    .addOnSuccessListener(result ->
                            requestIdToken(result.getUser(), callback))
                    .addOnFailureListener(callback::onError);
            return;
        }

        requestIdToken(user, callback);
    }

    private void requestIdToken(FirebaseUser user, TokenCallback callback) {
        if (user == null) {
            callback.onError(
                    new IllegalStateException("Firebase user is not signed in")
            );
            return;
        }

        user.getIdToken(true)
                .addOnSuccessListener(result -> {
                    String token = result.getToken();

                    if (token == null || token.isEmpty()) {
                        callback.onError(
                                new IllegalStateException("Firebase ID token is null")
                        );
                        return;
                    }

                    callback.onToken(token);
                })
                .addOnFailureListener(callback::onError);
    }
}