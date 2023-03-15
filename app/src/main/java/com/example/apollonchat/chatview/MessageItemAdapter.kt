package com.example.apollonchat.chatview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.print.PrintAttributes.Margins
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.RelativeLayout.LayoutParams
import android.widget.TextView
import androidx.annotation.Dimension
import androidx.annotation.IntegerRes
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.apollonchat.R
import com.example.apollonchat.chatlist.ChatUserItemAdapter
import com.example.apollonchat.database.contact.Contact
import com.example.apollonchat.database.message.DisplayMessage
import com.example.apollonchat.databinding.ChatMessageItemBinding
import com.example.apollonchat.databinding.ListItemUserBinding

class MessageItemAdapter(private val context : Context) : ListAdapter<DisplayMessage, MessageItemAdapter.MessageViewHolder>(MessageViewHolder.MessageItemDiffCallback()) {

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position), context)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder.from(parent)
    }

    class MessageViewHolder private constructor(val binding : ChatMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item : DisplayMessage, context: Context) {
            binding.message = item
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

            fun from(parent: ViewGroup) : MessageViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ChatMessageItemBinding.inflate(layoutInflater, parent, false)
                return MessageViewHolder(binding)
            }
        }

    class MessageItemDiffCallback : DiffUtil.ItemCallback<DisplayMessage>() {
        override fun areItemsTheSame(oldItem: DisplayMessage, newItem: DisplayMessage): Boolean {
            return oldItem.messageId == newItem.messageId
        }

        override fun areContentsTheSame(oldItem: DisplayMessage, newItem: DisplayMessage): Boolean {
            return oldItem == newItem
        }
    }

    }
}

/* This method styles the text message item depending on whether it
* was sent by us or the other end.
 */
@BindingAdapter("app:cardViewStyle")
fun bindCardViewStyle(view : CardView, own : Boolean) {
    if (own) {
        val lParams = view.layoutParams as MarginLayoutParams
        lParams.setMargins(30, 16, 120, 0)
        // Need to resolve the color instead of passing a reference
        val background = ContextCompat.getColor(view.context, R.color.apollon_green)
        view.setCardBackgroundColor(background)
    } else {
        val lParams = view.layoutParams as MarginLayoutParams
        lParams.setMargins(120, 16, 30, 0)
        val background = ContextCompat.getColor(view.context, R.color.apollon_green_bright)
        view.setCardBackgroundColor(background)
    }
    view.requestLayout()
}