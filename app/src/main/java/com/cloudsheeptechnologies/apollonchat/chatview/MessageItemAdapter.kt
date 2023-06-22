package com.cloudsheeptechnologies.apollonchat.chatview

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.databinding.BindingAdapter
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.cloudsheeptechnologies.apollonchat.R
import com.cloudsheeptechnologies.apollonchat.database.message.DisplayMessage
import com.cloudsheeptechnologies.apollonchat.databinding.ChatMessageItemBinding

class MessageItemAdapter() : PagingDataAdapter<DisplayMessage, MessageItemAdapter.MessageViewHolder>(MessageViewHolder.MessageItemDiffCallback()) {

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        return MessageViewHolder.from(parent)
    }

    class MessageViewHolder private constructor(val binding : ChatMessageItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item : DisplayMessage?) {
            item.let {
                binding.message = item
//            binding.margin = Margins(8, 8, 8, 0)
                binding.executePendingBindings()
            }
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
//        val background = ContextCompat.getColor(view.context, R.color.apollon_green)
//        view.setCardBackgroundColor(background)
        view.background = ContextCompat.getDrawable(view.context, R.drawable.own_message_gradient)
    } else {
        val lParams = view.layoutParams as MarginLayoutParams
        lParams.setMargins(120, 16, 30, 0)
//        val background = ContextCompat.getColor(view.context, R.color.apollon_green_bright)
//        view.setCardBackgroundColor(background)
        view.background = ContextCompat.getDrawable(view.context, R.drawable.remote_message_gradient)
    }
    view.requestLayout()
}