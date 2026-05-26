package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.LobbySnapshot;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;
import com.example.boardgame.server.security.PasswordHasher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Room {
    public static final String WAITING = "WAITING";
    public static final String READY = "READY";
    public static final String IN_GAME = "IN_GAME";
    public static final String FINISHED = "FINISHED";

    private final String code;
    private final long createdAtMillis;
    private long updatedAtMillis;
    private String hostPlayerId = "";
    private String status = WAITING;
    private byte[] passwordSalt;
    private byte[] passwordHash;

    // LinkedHashMap 유지: 유저가 방에 들어온 순서를 보장하여 턴 순서 배정에 유리함
    private final Map<String, Player> players = new LinkedHashMap<>();

    // 전담하여 관리할 게임의 핵심 상태들
    private GameState gameState;
    private MiniGameState miniGameState;
    private MicroGameState microGameState;

    public Room(String code) {
        this.code = code;
        this.createdAtMillis = System.currentTimeMillis();
        this.updatedAtMillis = createdAtMillis;
    }

    public void addPlayer(Player player) {
        players.put(player.getId(), player);
        if (hostPlayerId.isEmpty()) {
            hostPlayerId = player.getId();
            player.setHost(true);
        }
        touch();
    }

    public void removePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed != null) {
            removed.setHost(false);
        }
        if (playerId != null && playerId.equals(hostPlayerId)) {
            hostPlayerId = players.isEmpty() ? "" : players.keySet().iterator().next();
            Player host = players.get(hostPlayerId);
            if (host != null) {
                host.setHost(true);
            }
        }
        touch();
    }

    public boolean canStart(int minPlayers) {
        if (players.size() < minPlayers) {
            return false;
        }
        for (Player player : players.values()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public void refreshReadyStatus(int minPlayers) {
        if (IN_GAME.equals(status) || FINISHED.equals(status)) {
            return;
        }
        status = canStart(minPlayers) ? READY : WAITING;
        touch();
    }

    public Player getPlayer(String playerId) {
        return players.get(playerId);
    }

    public RoomSnapshot toSnapshot() {
        List<PlayerSnapshot> playerSnapshots = new ArrayList<>();
        for (Player player : players.values()) {
            playerSnapshots.add(player.toSnapshot());
        }
        return new RoomSnapshot(code, hostPlayerId, status, playerSnapshots);
    }

    public LobbySnapshot.RoomListInfo toRoomListInfo() {
        Player host = getPlayer(hostPlayerId);
        return new LobbySnapshot.RoomListInfo(
                code,
                status,
                players.size(),
                host == null ? "" : host.getNickname(),
                hasPassword()
        );
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            passwordSalt = null;
            passwordHash = null;
            touch();
            return;
        }

        passwordSalt = PasswordHasher.newSalt();
        passwordHash = PasswordHasher.hash(password, passwordSalt);
        touch();
    }

    public boolean hasPassword() {
        return passwordSalt != null && passwordHash != null;
    }

    public boolean passwordMatches(String attemptedPassword) {
        if (!hasPassword()) {
            return attemptedPassword == null || attemptedPassword.trim().isEmpty();
        }
        return PasswordHasher.matches(attemptedPassword, passwordSalt, passwordHash);
    }

    public void touch() {
        updatedAtMillis = System.currentTimeMillis();
    }

    // --- Getters & Setters ---

    public String getCode() { return code; }
    public String getHostPlayerId() { return hostPlayerId; }
    public String getStatus() { return status; }

    public void setStatus(String status) {
        this.status = status;
        touch();
    }

    public Map<String, Player> getPlayers() { return players; }
    public Collection<Player> getPlayerList() { return players.values(); }

    public GameState getGameState() { return gameState; }
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
        touch();
    }

    public MiniGameState getMiniGameState() { return miniGameState; }
    public void setMiniGameState(MiniGameState miniGameState) {
        this.miniGameState = miniGameState;
        touch();
    }

    public MicroGameState getMicroGameState() { return microGameState; }
    public void setMicroGameState(MicroGameState microGameState) {
        this.microGameState = microGameState;
        touch();
    }

    public long getCreatedAtMillis() { return createdAtMillis; }
    public long getUpdatedAtMillis() { return updatedAtMillis; }
}
