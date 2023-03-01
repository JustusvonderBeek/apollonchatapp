package com.example.apollonchat.chatview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apollonchat.chatlist.ChatUserItemAdapter
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.databinding.ChatMessageItemBinding
import com.example.apollonchat.databinding.ListItemUserBinding

class TextItemViewHolder(val textView: TextView): RecyclerView.ViewHolder(textView)

class MessageItemAdapter(private val context : Context) : ListAdapter<String, MessageItemAdapter.MessageViewHolder>(MessageViewHolder.TextItemDiffCallback()) {

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position), context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder.from(parent)
    }

    class MessageViewHolder private constructor(val binding : ChatMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item : String, context: Context) {
            binding.text = item
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup) : MessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatMessageItemBinding.inflate(layoutInflater, parent, false)
                return MessageViewHolder(binding)
            }
        }

    class TextItemDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem.contentEquals(newItem)
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    }
}