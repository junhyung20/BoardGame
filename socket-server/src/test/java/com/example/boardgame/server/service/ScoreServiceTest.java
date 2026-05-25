package com.example.boardgame.server.service;

import com.example.boardgame.server.model.Player;
import com.example.boardgame.server.model.Room;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ScoreServiceTest {
    @Test
    public void rankScoresAppliesRewardsByDescendingScore() {
        ScoreService scoreService = new ScoreService();
        Map<String, Integer> rawScores = new LinkedHashMap<>();
        rawScores.put("player-1", 20);
        rawScores.put("player-2", 50);
        rawScores.put("player-3", 10);

        Map<String, Integer> rewards = scoreService.rankScores(rawScores, new int[]{30, 20, 10});

        assertEquals(Integer.valueOf(30), rewards.get("player-2"));
        assertEquals(Integer.valueOf(20), rewards.get("player-1"));
        assertEquals(Integer.valueOf(10), rewards.get("player-3"));
    }

    @Test
    public void finalRankingsSortPlayersByTotalScore() {
        Room room = new Room("123456");
        Player first = new Player("player-1", "uid-1", "First");
        Player second = new Player("player-2", "uid-2", "Second");
        first.addScore(10);
        second.addScore(30);
        room.addPlayer(first);
        room.addPlayer(second);

        List<Player> rankings = new ScoreService().calculateFinalRankings(room);

        assertEquals("player-2", rankings.get(0).getId());
        assertEquals("player-1", rankings.get(1).getId());
    }
}
