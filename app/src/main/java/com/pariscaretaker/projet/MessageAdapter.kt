package com.pariscaretaker.projet

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val messageList: List<Message>,
    private val userToken: String?
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item_layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message, userToken)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val messageText: TextView = view.findViewById(R.id.chat_message)
        private val messageTime: TextView = view.findViewById(R.id.chat_time)

        fun bind(message: Message, userToken: String?) {
            messageText.text = message.content
            messageTime.text = message.creationDate

            val isSentByUser = message.author == userToken
            Log.d("BIND_MESSAGE", "Message Author: ${message.author}, User Token: $userToken, isSentByUser: $isSentByUser")
            messageText.setBackgroundResource(if (isSentByUser) R.drawable.chat_bubble_sent else R.drawable.chat_bubble_received)
        }
    }
}
