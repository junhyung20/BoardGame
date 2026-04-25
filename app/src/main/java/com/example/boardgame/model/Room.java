package com.example.boardgame.model;

import com.example.boardgame.util.Constants;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Room {
    public enum Status {
        WAITING,
        READY,
        IN_GAME,
        FINISHED
    }

    private String code;
    private String hostPlayerId;
    private Status status;
    private Map<String, Player> players;

    public Room() {
        this("");
    }

    public Room(String code) {
        this.code = code;
        this.status = Status.WAITING;
        this.players = new LinkedHashMap<>();
    }

    public boolean addPlayer(Player player) {
        if (player == null || isFull()) {
            return false;
        }
        players.put(player.getId(), player);
        if (hostPlayerId == null || hostPlayerId.isEmpty()) {
            hostPlayerId = player.getId();
            player.setHost(true);
        }
        return true;
    }

    public void removePlayer(String playerId) {
        players.remove(playerId);
        if (playerId != null && playerId.equals(hostPlayerId)) {
            hostPlayerId = players.isEmpty() ? null : players.keySet().iterator().next();
            if (hostPlayerId != null) {
                players.get(hostPlayerId).setHost(true);
            }
        }
    }

    public boolean isFull() {
        return players.size() >= Constants.MAX_PLAYERS;
    }

    public boolean canStart() {
        if (players.size() != Constants.MAX_PLAYERS) {
            return false;
        }
        for (Player player : players.values()) {
            if (!player.isReady()) {
                return false;
            }
        }
        return true;
    }

    public List<Player> getPlayerList() {
        return new ArrayList<>(players.values());
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getHostPlayerId() {
        return hostPlayerId;
    }

    public void setHostPlayerId(String hostPlayerId) {
        this.hostPlayerId = hostPlayerId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, Player> getPlayers() {
        return players;
    }

    public void setPlayers(Map<String, Player> players) {
        this.players = players == null ? new LinkedHashMap<>() : players;
    }
}
