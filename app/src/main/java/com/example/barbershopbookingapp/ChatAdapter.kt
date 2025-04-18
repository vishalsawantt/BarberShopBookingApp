package com.example.barbershopbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(
    private val messageList: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = if (viewType == VIEW_TYPE_SENT) {
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_sent, parent, false)
        } else {
            LayoutInflater.from(parent.context).inflate(R.layout.item_chat_received, parent, false)
        }
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messageList[position]

        // Use the helper functions from ChatMessage
        holder.txtMessage.text = message.message
        holder.txtTime.text = message.getFormattedTime()

        // Show date separator only when the date changes
        if (position == 0 || message.getFormattedDate() != messageList[position - 1].getFormattedDate()) {
            holder.txtDate.text = message.getFormattedDate()
            holder.txtDate.visibility = View.VISIBLE
        } else {
            holder.txtDate.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int = messageList.size

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMessage: TextView = view.findViewById(R.id.txtMessage)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val txtDate: TextView = view.findViewById(R.id.txtDate)
    }

}
