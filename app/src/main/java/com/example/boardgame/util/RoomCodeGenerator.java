package com.example.boardgame.util;

public final class RoomCodeGenerator {
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private RoomCodeGenerator() {
    }

    public static String generate() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RandomUtil.randomInt(0, ALPHABET.length() - 1);
            code.append(ALPHABET.charAt(index));
        }
        return code.toString();
    }
}
