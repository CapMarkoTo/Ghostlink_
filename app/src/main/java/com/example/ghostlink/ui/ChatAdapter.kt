package com.example.ghostlink.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ghostlink.data.Chat

// Этот класс берет список чатов и "рисует" их в списке
class ChatAdapter(private val chats: List<Chat>, private val onClick: (Chat) -> Unit) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // Холдер - это коробка, в которой лежат элементы дизайна (например, текст)
    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(android.R.id.text1)
    }

    // Здесь мы выбираем, как будет выглядеть один пункт списка (пока берем стандартный)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ChatViewHolder(view)
    }

    // Тут мы вставляем имя чата в текстовое поле
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.nameText.text = chat.chatName
        holder.itemView.setOnClickListener { onClick(chat) }
    }

    override fun getItemCount() = chats.size
}