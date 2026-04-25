package com.example.boardgame.model;

public class BoardTile {
    public enum Type {
        START,
        NORMAL,
        QUESTION_MARK,
        CARD,
        GAME
    }

    private int index;
    private Type type;
    private int value;
    private String label;

    public BoardTile() {
        this(0, Type.NORMAL, 0, "Normal");
    }

    public BoardTile(int index, Type type, int value, String label) {
        this.index = index;
        this.type = type;
        this.value = value;
        this.label = label;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
