package com.example.boardgame;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.boardgame.socket.protocol.GameSnapshot;
import com.example.boardgame.socket.protocol.PlayerSnapshot;
import com.example.boardgame.socket.protocol.RoomSnapshot;

import java.util.ArrayList;

public class BoardActivity extends AppCompatActivity {
    public static final String EXTRA_PLAYERS = "extra_players";

    private TextView boardCountdownText;
    private TextView boardPlayersText;

    private final ServerSession.Listener serverListener = new ServerSession.Listener() {
        @Override
        public void onRoomUpdated(RoomSnapshot room) {
            renderBoard();
        }

        @Override
        public void onGameUpdated(GameSnapshot game) {
            renderBoard();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);
        boardCountdownText = findViewById(R.id.boardCountdownText);
        boardPlayersText = findViewById(R.id.boardPlayersText);
        renderBoard();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ServerSession.addListener(serverListener);
    }

    @Override
    protected void onStop() {
        ServerSession.removeListener(serverListener);
        super.onStop();
    }

    private void renderBoard() {
        GameSnapshot game = ServerSession.getLatestGameSnapshot();
        if (game == null) {
            boardCountdownText.setText(getString(R.string.board_start_now));
        } else {
            boardCountdownText.setText("Round " + game.getCurrentRound()
                    + "/" + game.getFinalRound()
                    + "  Phase: " + game.getTurnPhase());
        }

        StringBuilder description = new StringBuilder();
        description.append(getString(R.string.board_demo_title)).append("\n\n");
        description.append(getString(R.string.board_participants_title)).append("\n");

        RoomSnapshot room = ServerSession.getLatestRoomSnapshot();
        if (room != null && !room.getPlayers().isEmpty()) {
            int index = 1;
            for (PlayerSnapshot player : room.getPlayers()) {
                description.append(index)
                        .append("P  ")
                        .append(player.getNickname())
                        .append("  score=")
                        .append(player.getScore())
                        .append("  pos=")
                        .append(player.getPosition())
                        .append("\n");
                index += 1;
            }
        } else {
            ArrayList<String> players = getIntent().getStringArrayListExtra(EXTRA_PLAYERS);
            if (players != null) {
                for (int i = 0; i < players.size(); i++) {
                    description.append(i + 1).append("P  ").append(players.get(i)).append("\n");
                }
            }
        }
        boardPlayersText.setText(description.toString());
    }
}
