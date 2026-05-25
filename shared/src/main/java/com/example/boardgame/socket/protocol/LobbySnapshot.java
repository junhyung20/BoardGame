package com.example.boardgame.socket.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LobbySnapshot {
    private final List<RoomListInfo> rooms;

    public LobbySnapshot(List<RoomListInfo> rooms) {
        this.rooms = new ArrayList<>(rooms == null ? Collections.emptyList() : rooms);
    }

    public List<RoomListInfo> getRooms() {
        return Collections.unmodifiableList(rooms);
    }

    public static class RoomListInfo {
        private final String code;
        private final String status;
        private final int playerCount;
        private final String hostNickname;
        private final boolean hasPassword;

        public RoomListInfo(String code, String status, int playerCount, String hostNickname, boolean hasPassword) {
            this.code = code;
            this.status = status;
            this.playerCount = playerCount;
            this.hostNickname = hostNickname;
            this.hasPassword = hasPassword;
        }

        public String getCode() {
            return code;
        }

        public String getStatus() {
            return status;
        }

        public int getPlayerCount() {
            return playerCount;
        }

        public String getHostNickname() {
            return hostNickname;
        }

        public boolean hasPassword() {
            return hasPassword;
        }
    }
}
