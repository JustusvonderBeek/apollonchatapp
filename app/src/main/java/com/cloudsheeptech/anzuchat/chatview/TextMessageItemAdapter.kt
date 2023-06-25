package com.cloudsheeptech.anzuchat.chatview

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptech.anzuchat.databinding.ChatMessageItemBinding

class TextMessageItemAdapter(private val context : Context) : ListAdapter<String, TextMessageItemAdapter.TextMessageViewHolder>(TextMessageViewHolder.TextItemDiffCallback()) {

    override fun onBindViewHolder(holder: TextMessageViewHolder, position: Int) {
        holder.bind(getItem(position), context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TextMessageViewHolder {
        return TextMessageViewHolder.from(parent)
    }

    class TextMessageViewHolder private constructor(val binding : ChatMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item : String, context: Context) {
//            binding.message = item
//            binding.margin = Margins(8, 8, 8, 0)
            binding.executePendingBindings()
        }

        companion object {

            // This function can be used to apply the margin view XML and databinding
//            @JvmStatic
//            @BindingAdapter("android:margin_own")
//            fun setLayoutMarginRight(view : View, own : Boolean) {
//                Log.i("Adapter", "Calling binding adapter")
//                val layoutParams = view.layoutParams as MarginLayoutParams
//                layoutParams.setMargins(8,8,8,8)
//                if (own) {
//                    layoutParams.rightMargin = R.dimen.margin_right
//                } else {
//                    layoutParams.rightMargin = R.dimen.margin_right_own
//                }
//                view.layoutParams = layoutParams
//            }

            fun from(parent: ViewGroup) : TextMessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatMessageItemBinding.inflate(layoutInflater, parent, false)
                return TextMessageViewHolder(binding)
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