package com.cloudsheeptech.anzuchat.chatlist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.anzuchat.database.contact.Contact
import com.cloudsheeptech.anzuchat.databinding.ListItemUserBinding

class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)

class ChatUserItemAdapter(val clickListener : ChatUserItemListener, private val context : Context) : ListAdapter<Contact, ChatUserItemAdapter.ChatUserViewHolder>(
    UserDiffCallback()
) {

//    private val viewBinderHelper = ViewBinderHelper()

    override fun onBindViewHolder(holder: ChatUserViewHolder, position: Int) {
        holder.bind(clickListener, getItem(position), context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatUserViewHolder {
        return ChatUserViewHolder.from(parent)
    }

    // Best practice: Include the class as a view holder which we will actually work with
    class ChatUserViewHolder private constructor(val binding : ListItemUserBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(clickListener : ChatUserItemListener, item : Contact, context: Context) {
            binding.contact = item
            binding.clickListener = clickListener
//            binding.username = item.contactName
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) : ChatUserViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemUserBinding.inflate(layoutInflater, parent, false)
                return ChatUserViewHolder(binding)
            }
        }

    }

    class ChatUserItemListener(val clickListener : (userId : Long) -> Unit) {
        fun onClick(contact : Contact) = clickListener(contact.contactId)
    }

    class UserDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem.contactId == newItem.contactId
//            return false
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
            return oldItem == newItem
//            return false
        }
    }
}