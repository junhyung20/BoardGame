package com.example.boardgame.service;

import com.example.boardgame.model.BoardTile;
import com.example.boardgame.model.Player;
import com.example.boardgame.util.Constants;
import com.example.boardgame.util.RandomUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TileService {
    private final List<BoardTile> boardTiles;

    public TileService() {
        this.boardTiles = createDefaultBoard();
    }

    public BoardTile getTile(int position) {
        int index = Math.floorMod(position, boardTiles.size());
        return boardTiles.get(index);
    }

    public void applyTileEffect(Player player, BoardTile tile) {
        if (player == null || tile == null) {
            return;
        }

        switch (tile.getType()) {
            case QUESTION_MARK:
                applyRandomEvent(player);
                break;
            case CARD:
                player.addItemCard(drawItemCard());
                break;
            case START:
            case GAME:
            case NORMAL:
            default:
                break;
        }
    }

    public List<BoardTile> getBoardTiles() {
        return new ArrayList<>(boardTiles);
    }

    private List<BoardTile> createDefaultBoard() {
        List<BoardTile> tiles = new ArrayList<>();
        tiles.add(new BoardTile(0, BoardTile.Type.START, 0, "Start"));

        for (int i = 1; i < Constants.BOARD_SIZE; i++) {
            if (i % 9 == 0) {
                tiles.add(new BoardTile(i, BoardTile.Type.GAME, 0, "Mini Game"));
            } else if (i % 4 == 0) {
                tiles.add(new BoardTile(i, BoardTile.Type.CARD, 0, "Card"));
            } else if (i % 2 == 0) {
                tiles.add(new BoardTile(i, BoardTile.Type.QUESTION_MARK, 0, "Random Event"));
            } else {
                tiles.add(new BoardTile(i, BoardTile.Type.NORMAL, 0, "Normal"));
            }
        }

        return tiles;
    }

    private void applyRandomEvent(Player player) {
        int event = RandomUtil.randomInt(1, 4);
        switch (event) {
            case 1:
                player.addScore(Constants.RANDOM_EVENT_BONUS_SCORE);
                break;
            case 2:
                player.addScore(Constants.RANDOM_EVENT_PENALTY_SCORE);
                break;
            case 3:
                player.moveBy(Constants.RANDOM_EVENT_MOVE_STEPS, boardTiles.size());
                break;
            case 4:
            default:
                player.moveBy(-Constants.RANDOM_EVENT_MOVE_STEPS, boardTiles.size());
                break;
        }
    }

    private String drawItemCard() {
        List<String> itemCards = Arrays.asList(
                Constants.CARD_DOUBLE_DICE,
                Constants.CARD_SHIELD,
                Constants.CARD_SWAP_POSITION
        );
        return itemCards.get(RandomUtil.randomInt(0, itemCards.size() - 1));
    }
}
