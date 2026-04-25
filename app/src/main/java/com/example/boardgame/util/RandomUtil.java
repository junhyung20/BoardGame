package com.example.boardgame.util;

import java.util.Random;

public final class RandomUtil {
    private static final Random RANDOM = new Random();

    private RandomUtil() {
    }

    public static int rollDice() {
        return randomInt(Constants.DICE_MIN, Constants.DICE_MAX);
    }

    public static int randomInt(int minInclusive, int maxInclusive) {
        if (minInclusive > maxInclusive) {
            throw new IllegalArgumentException("minInclusive must be <= maxInclusive");
        }
        return RANDOM.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }
}
