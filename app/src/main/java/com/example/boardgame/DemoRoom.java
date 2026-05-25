package com.example.boardgame;

public class DemoRoom {
    public final String id;
    public final String roomCode;
    public final int currentCount;
    public final int maxCount;
    public final String hostNickname;
    public final String status;
    public final boolean hasPassword;

    public DemoRoom(
            String id,
            String roomCode,
            int currentCount,
            int maxCount,
            String hostNickname,
            String status,
            boolean hasPassword
    ) {
        this.id = id == null ? "" : id;
        this.roomCode = roomCode == null ? "" : roomCode;
        this.currentCount = currentCount;
        this.maxCount = maxCount;
        this.hostNickname = hostNickname == null ? "-" : hostNickname;
        this.status = status == null ? "" : status;
        this.hasPassword = hasPassword;
    }
}
