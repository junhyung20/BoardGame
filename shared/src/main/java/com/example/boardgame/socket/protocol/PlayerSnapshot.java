package com.example.boardgame.socket.protocol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlayerSnapshot {
    private final String id;
    private final String nickname;
    private final int score;
    private final int position;
    private final boolean ready;
    private final boolean host;

    private final boolean inMicroGame;
    private final List<String> itemCards;

    public PlayerSnapshot(
            String id,
            String nickname,
            int score,
            int position,
            boolean ready,
            boolean host,
            boolean inMicroGame,
            List<String> itemCards
    ) {
        this.id = id == null ? "" : id;
        this.nickname = nickname == null ? "" : nickname;
        this.score = score;
        this.position = position;
        this.ready = ready;
        this.host = host;
        this.inMicroGame = inMicroGame;
        this.itemCards = itemCards == null ? new ArrayList<>() : new ArrayList<>(itemCards);
    }

    public String getId() { return id; }
    public String getNickname() { return nickname; }
    public int getScore() { return score; }
    public int getPosition() { return position; }
    public boolean isReady() { return ready; }
    public boolean isHost() { return host; }

    public boolean isInMicroGame() { return inMicroGame; }
    public List<String> getItemCards() {
        return Collections.unmodifiableList(itemCards);
    }
}
