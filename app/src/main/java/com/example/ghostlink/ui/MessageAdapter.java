package com.example.ghostlink.ui;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Исправление 3
import androidx.recyclerview.widget.RecyclerView;
import com.example.ghostlink.R;
import com.example.ghostlink.data.Message;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Message> messageList;

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.textMessage.setText(message.text);

        // --- Исправление 2: Устанавливаем время ---
        holder.textTimestamp.setText(formatTimestamp(message.timestamp));

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) holder.cardView.getLayoutParams();
        if (message.isMine) {
            params.gravity = Gravity.END;
            // --- Исправление 3: Используем ContextCompat ---
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_light));
        } else {
            params.gravity = Gravity.START;
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), android.R.color.darker_gray));
        }
        holder.cardView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- Исправление 1: Метод перенесен сюда ---
    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date date = new Date(timestamp);
        return sdf.format(date);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView textMessage;
        TextView textTimestamp; // Добавили поле для времени
        MaterialCardView cardView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.textMessage);
            textTimestamp = itemView.findViewById(R.id.textTimestamp); // Инициализация
            cardView = itemView.findViewById(R.id.messageCard);
        }
    }
}