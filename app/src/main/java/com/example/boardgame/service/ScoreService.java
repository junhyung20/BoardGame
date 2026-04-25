package com.example.boardgame.service;

import com.example.boardgame.model.Player;
import com.example.boardgame.model.Room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ScoreService {
    public void applyScoreDeltas(Room room, Map<String, Integer> scoreDeltasByPlayerId) {
        if (room == null || scoreDeltasByPlayerId == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : scoreDeltasByPlayerId.entrySet()) {
            Player player = room.getPlayers().get(entry.getKey());
            if (player != null) {
                player.addScore(entry.getValue());
            }
        }
    }

    public Player getWinner(Room room) {
        List<Player> leaderboard = getLeaderboard(room);
        return leaderboard.isEmpty() ? null : leaderboard.get(0);
    }

    public List<Player> getLeaderboard(Room room) {
        List<Player> players = room == null ? new ArrayList<>() : room.getPlayerList();
        Collections.sort(players, new Comparator<Player>() {
            @Override
            public int compare(Player first, Player second) {
                return Integer.compare(second.getScore(), first.getScore());
            }
        });
        return players;
    }
}
