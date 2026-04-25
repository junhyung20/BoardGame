package com.example.boardgame.storage;

public interface FirebaseListener<T> {
    void onDataChanged(T data);

    void onError(Exception exception);
}
