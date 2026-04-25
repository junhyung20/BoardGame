package com.example.boardgame.service;

import com.example.boardgame.model.GameState;
import com.example.boardgame.model.MicroGameState;
import com.example.boardgame.storage.FirebaseGameStorage;
import com.example.boardgame.util.Constants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MicroGameService {
    private final FirebaseGameStorage gameStorage;

    public MicroGameService(FirebaseGameStorage gameStorage) {
        this.gameStorage = gameStorage;
    }

    public MicroGameState startMicroGame(GameState gameState) {
        MicroGameState microGameState = new MicroGameState(
                gameState.getRoomCode(),
                "micro-round-" + gameState.getCurrentRound() + "-" + System.currentTimeMillis(),
                gameState.getCurrentRound(),
                Constants.MICRO_GAME_DURATION_SECONDS
        );
        microGameState.start(System.currentTimeMillis());
        gameStorage.saveMicroGameState(microGameState);
        return microGameState;
    }

    public MicroGameState submitScore(MicroGameState microGameState, String playerId, int score) {
        microGameState.submitScore(playerId, score);
        gameStorage.saveMicroGameState(microGameState);
        return microGameState;
    }

    public boolean allPlayersSubmitted(MicroGameState microGameState, int expectedPlayerCount) {
        return microGameState != null && microGameState.getSubmittedScores().size() >= expectedPlayerCount;
    }

    public Map<String, Integer> finishMicroGame(MicroGameState microGameState) {
        List<Map.Entry<String, Integer>> submittedScores = new ArrayList<>(microGameState.getSubmittedScores().entrySet());
        Collections.sort(submittedScores, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> first, Map.Entry<String, Integer> second) {
                return Integer.compare(second.getValue(), first.getValue());
            }
        });

        Map<String, Integer> scoreDeltasByPlayerId = new LinkedHashMap<>();
        for (int i = 0; i < submittedScores.size(); i++) {
            Map.Entry<String, Integer> entry = submittedScores.get(i);
            int scoreDelta = i < Constants.MICRO_GAME_SCORE_BY_RANK.length
                    ? Constants.MICRO_GAME_SCORE_BY_RANK[i]
                    : 0;
            scoreDeltasByPlayerId.put(entry.getKey(), scoreDelta);
        }

        microGameState.setStatus(MicroGameState.Status.FINISHED);
        gameStorage.saveMicroGameState(microGameState);
        return scoreDeltasByPlayerId;
    }
}
