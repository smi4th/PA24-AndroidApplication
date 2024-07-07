package com.pariscaretaker.projet

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class ContactAdapter(
    private var contactList: List<Contact>
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contact_item_layout, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    fun updateContacts(newContacts: List<Contact>) {
        contactList = newContacts
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
        private val name: TextView = view.findViewById(R.id.contact_name)
        private val lastMessage: TextView = view.findViewById(R.id.contact_last_message)
        private val contactTime: TextView = view.findViewById(R.id.contact_time)
        private lateinit var contact: Contact

        init {
            view.setOnClickListener(this)
        }

        fun bind(contact: Contact) {
            this.contact = contact
            name.text = contact.name
            lastMessage.text = contact.lastMessage
            contactTime.text = contact.time
        }

        override fun onClick(v: View?) {
            val context = itemView.context
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("contact", contact)
            Log.d("CONTACT_CLICK", "Contact UUID: ${contact.uuid}, Name: ${contact.name}")
            context.startActivity(intent)
        }

    }
}
