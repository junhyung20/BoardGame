package com.example.boardgame.storage;

import com.example.boardgame.model.Room;
import com.example.boardgame.util.FirebasePaths;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class FirebaseRoomStorage {
    private static final Map<String, Room> roomCache = new HashMap<>();
    private final DatabaseReference roomsRef;

    public FirebaseRoomStorage() {
        this(FirebaseDatabase.getInstance().getReference(FirebasePaths.ROOMS));
    }

    public FirebaseRoomStorage(DatabaseReference roomsRef) {
        this.roomsRef = roomsRef;
    }

    public void saveRoom(Room room) {
        if (room != null && room.getCode() != null) {
            roomCache.put(room.getCode(), room);
            roomsRef.child(room.getCode()).setValue(room);
        }
    }

    public Room getRoom(String roomCode) {
        return roomCache.get(roomCode);
    }

    public void deleteRoom(String roomCode) {
        roomCache.remove(roomCode);
        roomsRef.child(roomCode).removeValue();
    }

    public void listenToRoom(String roomCode, FirebaseListener<Room> listener) {
        roomsRef.child(roomCode).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Room room = snapshot.getValue(Room.class);
                if (room != null) {
                    roomCache.put(roomCode, room);
                }
                listener.onDataChanged(room);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                listener.onError(error.toException());
            }
        });
    }
}
