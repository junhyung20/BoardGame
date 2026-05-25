package com.example.boardgame;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.RoomViewHolder> {
    public interface OnRoomClickListener {
        void onRoomClicked(DemoRoom room);
    }

    private final List<DemoRoom> rooms = new ArrayList<>();
    private final OnRoomClickListener listener;

    public RoomListAdapter(OnRoomClickListener listener) {
        this.listener = listener;
    }

    public void submit(List<DemoRoom> updatedRooms) {
        rooms.clear();
        if (updatedRooms != null) {
            rooms.addAll(updatedRooms);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_room, parent, false);
        return new RoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        DemoRoom room = rooms.get(position);
        holder.codeText.setText(room.roomCode);
        String host = holder.itemView.getContext().getString(R.string.room_host_format, room.hostNickname);
        holder.hostText.setText(room.hasPassword ? host + " [locked]" : host);
        holder.countText.setText(holder.itemView.getContext().getString(
                R.string.room_count_format,
                room.currentCount,
                room.maxCount
        ));
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onRoomClicked(room);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        final TextView codeText;
        final TextView hostText;
        final TextView countText;

        RoomViewHolder(@NonNull View itemView) {
            super(itemView);
            codeText = itemView.findViewById(R.id.roomItemCode);
            hostText = itemView.findViewById(R.id.roomItemHost);
            countText = itemView.findViewById(R.id.roomItemCount);
        }
    }
}
