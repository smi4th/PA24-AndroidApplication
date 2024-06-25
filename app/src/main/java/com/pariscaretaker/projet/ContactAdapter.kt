package com.pariscaretaker.projet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private val contactList: List<Contact>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.header_item_layout, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item_layout, parent, false)
            ContactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ContactViewHolder) {
            val contact = contactList[position - 1]
            holder.bind(contact)
        }
    }

    override fun getItemCount(): Int {
        return contactList.size + 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.contact_name)
        private val lastMessage: TextView = view.findViewById(R.id.contact_last_message)
        private val contacttime: TextView = view.findViewById(R.id.contact_time)

        fun bind(contact: Contact) {
            name.text = contact.name
            lastMessage.text = contact.lastMessage
            contacttime.text = contact.time
        }
    }

}