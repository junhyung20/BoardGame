package com.example.boardgame.service;

import com.example.boardgame.model.GameState;
import com.example.boardgame.model.MiniGameState;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MiniGameService {
    private final FirebaseGameStorage gameStorage;

    public MiniGameService(FirebaseGameStorage gameStorage) {
        this.gameStorage = gameStorage;
    }

    public MiniGameState startMiniGame(GameState gameState) {
        MiniGameState.Type type = pickRoundMiniGameType(gameState.getCurrentRound());
        MiniGameState miniGameState = new MiniGameState(
                gameState.getRoomCode(),
                "round-" + gameState.getCurrentRound() + "-" + type.name(),
                gameState.getCurrentRound(),
                type,
                Constants.MINI_GAME_DURATION_SECONDS
        );
        miniGameState.start(System.currentTimeMillis());
        gameStorage.saveMiniGameState(miniGameState);
        return miniGameState;
    }

    public MiniGameState submitScore(MiniGameState miniGameState, String playerId, int score) {
        miniGameState.submitScore(playerId, score);
        gameStorage.saveMiniGameState(miniGameState);
        return miniGameState;
    }

    public boolean allPlayersSubmitted(MiniGameState miniGameState, int expectedPlayerCount) {
        return miniGameState != null && miniGameState.getSubmittedScores().size() >= expectedPlayerCount;
    }

    public Map<String, Integer> finishMiniGame(MiniGameState miniGameState) {
        List<Map.Entry<String, Integer>> submittedScores = new ArrayList<>(miniGameState.getSubmittedScores().entrySet());
        Collections.sort(submittedScores, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> first, Map.Entry<String, Integer> second) {
                return Integer.compare(second.getValue(), first.getValue());
            }
        });

        Map<String, Integer> scoreDeltasByPlayerId = new LinkedHashMap<>();
        for (int i = 0; i < submittedScores.size(); i++) {
            Map.Entry<String, Integer> entry = submittedScores.get(i);
            int scoreDelta = i < Constants.MINI_GAME_SCORE_BY_RANK.length
                    ? Constants.MINI_GAME_SCORE_BY_RANK[i]
                    : 0;
            scoreDeltasByPlayerId.put(entry.getKey(), scoreDelta);
        }

        miniGameState.setStatus(MiniGameState.Status.FINISHED);
        gameStorage.saveMiniGameState(miniGameState);
        return scoreDeltasByPlayerId;
    }

    private MiniGameState.Type pickRoundMiniGameType(int round) {
        MiniGameState.Type[] types = {
                MiniGameState.Type.COLOR_GUESSING,
                MiniGameState.Type.PASSWORD_GUESSING,
                MiniGameState.Type.PHONE_TILT_MAZE
        };
        return types[Math.floorMod(round - 1, types.length)];
    }
}
