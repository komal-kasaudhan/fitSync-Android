package com.esha.fitsync.screens.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.R
import com.esha.fitsync.models.AiChatMessage

class AiChatAdapter(
    private val messages: MutableList<AiChatMessage> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_AI = 2
    }

    override fun getItemViewType(position: Int): Int {
        val msg = messages[position]
        return if (msg.role.equals("user", ignoreCase = true)) TYPE_USER else TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_ai, parent, false)
            AiMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is UserMessageViewHolder) {
            holder.tvMessage.text = msg.content
        } else if (holder is AiMessageViewHolder) {
            holder.tvMessage.text = msg.content
        }
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: AiChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun setMessages(newMessages: List<AiChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
    }

    class AiMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMessage: TextView = itemView.findViewById(R.id.tvAiMessage)
    }
}
