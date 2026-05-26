package com.example.boardgame.server.model;

import com.example.boardgame.socket.protocol.PlayerSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player {
    private final String id;
    private final String firebaseUid;
    private final String nickname;
    private int score;
    private int position;
    private boolean ready;
    private boolean host;

    private boolean inMicroGame = false;

    private final List<String> itemCards = new ArrayList<>();

    public Player(String id, String firebaseUid, String nickname) {
        this.id = id;
        this.firebaseUid = firebaseUid == null ? "" : firebaseUid;
        this.nickname = nickname == null || nickname.trim().isEmpty() ? "Player" : nickname.trim();
    }

    public void moveBy(int steps, int boardSize) {
        // Math.floorMod를 사용해 뒤로 가는 효과(음수 steps)가 발생해도 안전하게 순환합니다.
        position = Math.floorMod(position + steps, boardSize);
    }

    public void addScore(int points) {
        score += points; // 점수는 기획상 마이너스가 될 수 있으므로 그대로 더합니다.
    }

    public boolean addItemCard(String itemCard) {
        if (itemCards.size() >= 1) {
            return false;
        }
        itemCards.add(itemCard);
        return true;
    }

    public boolean hasItemCard(String itemCard) {
        return itemCards.contains(itemCard);
    }

    public boolean useItemCard(String itemCard) {
        return itemCards.remove(itemCard);
    }

    public PlayerSnapshot toSnapshot() {
        return new PlayerSnapshot(
                id,
                nickname,
                score,
                position,
                ready,
                host,
                inMicroGame,
                new ArrayList<>(itemCards)
        );
    }

    // --- Getters & Setters ---

    public String getId() { return id; }
    public String getFirebaseUid() { return firebaseUid; }
    public String getNickname() { return nickname; }
    public int getScore() { return score; }

    // 점수 강제 세팅이 필요한 경우를 대비 (예: 미니게임 후 일괄 정산)
    public void setScore(int score) { this.score = score; }

    public int getPosition() { return position; }

    // 특정 칸으로 강제 이동(워프)하는 경우를 대비
    public void setPosition(int position) { this.position = position; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean isHost() { return host; }
    public void setHost(boolean host) { this.host = host; }

    public boolean isInMicroGame() { return inMicroGame; }
    public void setInMicroGame(boolean inMicroGame) { this.inMicroGame = inMicroGame; }

    public List<String> getItemCards() {
        return Collections.unmodifiableList(itemCards);
    }
}
