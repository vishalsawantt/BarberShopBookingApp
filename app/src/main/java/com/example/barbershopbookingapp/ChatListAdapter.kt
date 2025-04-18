package com.example.barbershopbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatUser(val uid: String, val name: String)

class ChatListAdapter(
    private val userList: List<ChatUser>,
    private val onClick: (ChatUser) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtName: TextView = view.findViewById(R.id.txtChatUserName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_user, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val user = userList[position]
        holder.txtName.text = user.name
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount() = userList.size
}
