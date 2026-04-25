package com.example.boardgame.storage;

import com.example.boardgame.model.GameState;
import com.example.boardgame.model.MicroGameState;
import com.example.boardgame.model.MiniGameState;
import com.example.boardgame.util.FirebasePaths;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseGameStorage {
    private static final Map<String, GameState> gameStateCache = new HashMap<>();
    private static final Map<String, MiniGameState> miniGameStateCache = new HashMap<>();
    private static final Map<String, MicroGameState> microGameStateCache = new HashMap<>();
    private final DatabaseReference gamesRef;
    private final DatabaseReference miniGamesRef;
    private final DatabaseReference microGamesRef;

    public FirebaseGameStorage() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        this.gamesRef = database.getReference(FirebasePaths.GAMES);
        this.miniGamesRef = database.getReference(FirebasePaths.MINI_GAMES);
        this.microGamesRef = database.getReference(FirebasePaths.MICRO_GAMES);
    }

    public FirebaseGameStorage(DatabaseReference gamesRef, DatabaseReference miniGamesRef, DatabaseReference microGamesRef) {
        this.gamesRef = gamesRef;
        this.miniGamesRef = miniGamesRef;
        this.microGamesRef = microGamesRef;
    }

    public void saveGameState(GameState gameState) {
        if (gameState != null && gameState.getRoomCode() != null) {
            gameStateCache.put(gameState.getRoomCode(), gameState);
            gamesRef.child(gameState.getRoomCode()).setValue(gameState);
        }
    }

    public GameState getGameState(String roomCode) {
        return gameStateCache.get(roomCode);
    }

    public void saveMiniGameState(MiniGameState miniGameState) {
        if (miniGameState != null && miniGameState.getRoomCode() != null) {
            miniGameStateCache.put(miniGameState.getRoomCode(), miniGameState);
            miniGamesRef.child(miniGameState.getRoomCode()).setValue(miniGameState);
        }
    }

    public MiniGameState getMiniGameState(String roomCode) {
        return miniGameStateCache.get(roomCode);
    }

    public void saveMicroGameState(MicroGameState microGameState) {
        if (microGameState != null && microGameState.getRoomCode() != null) {
            microGameStateCache.put(microGameState.getRoomCode(), microGameState);
            microGamesRef.child(microGameState.getRoomCode()).setValue(microGameState);
        }
    }

    public MicroGameState getMicroGameState(String roomCode) {
        return microGameStateCache.get(roomCode);
    }

    public void listenToGameState(String roomCode, FirebaseListener<GameState> listener) {
        gamesRef.child(roomCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                GameState gameState = snapshot.getValue(GameState.class);
                if (gameState != null) {
                    gameStateCache.put(roomCode, gameState);
                }
                listener.onDataChanged(gameState);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    public void listenToMiniGameState(String roomCode, FirebaseListener<MiniGameState> listener) {
        miniGamesRef.child(roomCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                MiniGameState miniGameState = snapshot.getValue(MiniGameState.class);
                if (miniGameState != null) {
                    miniGameStateCache.put(roomCode, miniGameState);
                }
                listener.onDataChanged(miniGameState);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }

    public void listenToMicroGameState(String roomCode, FirebaseListener<MicroGameState> listener) {
        microGamesRef.child(roomCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                MicroGameState microGameState = snapshot.getValue(MicroGameState.class);
                if (microGameState != null) {
                    microGameStateCache.put(roomCode, microGameState);
                }
                listener.onDataChanged(microGameState);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }
}
