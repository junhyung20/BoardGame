package com.example.boardgame.model;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String id;
    private String nickname;
    private int score;
    private int position;
    private boolean ready;
    private boolean host;
    private List<String> itemCards;

    public Player() {
        this("", "");
    }

    public Player(String id, String nickname) {
        this.id = id;
        this.nickname = nickname;
        this.score = 0;
        this.position = 0;
        this.ready = false;
        this.host = false;
        this.itemCards = new ArrayList<>();
    }

    public void addScore(int points) {
        score += points;
    }

    public void moveBy(int steps, int boardSize) {
        if (boardSize <= 0) {
            return;
        }
        position = Math.floorMod(position + steps, boardSize);
    }

    public void addItemCard(String itemCard) {
        itemCards.add(itemCard);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public List<String> getItemCards() {
        return itemCards;
    }

    public void setItemCards(List<String> itemCards) {
        this.itemCards = itemCards == null ? new ArrayList<>() : itemCards;
    }
}
