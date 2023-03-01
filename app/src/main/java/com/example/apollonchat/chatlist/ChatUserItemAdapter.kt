package com.example.apollonchat

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.databinding.ListItemUserBinding

class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)

class ChatUserItemAdapter(val clickListener : ChatUserItemListener, private val context : Context) : ListAdapter<Contact, ChatUserItemAdapter.ChatUserViewHolder>(UserDiffCallback()) {

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
//            binding.username = item.username
        }

        companion object {
            fun from(parent: ViewGroup) : ChatUserViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemUserBinding.inflate(layoutInflater, parent, false)
                return ChatUserViewHolder(binding)
            }
        }

    }

    class ChatUserItemListener(val clickListener : (userId : Long, functionType : Int) -> Unit) {
        fun onClick(contact : Contact, function : Int) = clickListener(contact.contactId.toLong(), function)
    }

    class UserDiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(oldItem: Contact, newItem: Contact): Boolean {
//            return oldItem.userId == newItem.userId
            return false
        }

        override fun areContentsTheSame(oldItem: Contact, newItem: Contact): Boolean {
//            return oldItem == newItem
            return false
        }
    }
}