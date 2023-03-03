package com.example.apollonchat.chatview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.apollonchat.R
import com.example.apollonchat.chatlist.ChatListViewModel
import com.example.apollonchat.chatlist.ChatListViewModelFactory
import com.example.apollonchat.chatlist.ChatUserItemAdapter
import com.example.apollonchat.chatview.ChatViewViewModel
import com.example.apollonchat.chatview.ChatViewViewModelFactory
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.databinding.FragmentChatViewBinding
class ChatViewFragment : Fragment() {
    private lateinit var viewModel : ChatViewViewModel
    private lateinit var viewModelFactory : ChatViewViewModelFactory

    private lateinit var binding : FragmentChatViewBinding

    // Receiving the given safearg containing the actual pressed contactID
    private val args : ChatViewFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_view, container, false)

        val application = requireNotNull(this.activity).application
        val dataSource = ApollonDatabase.getInstance(application).contactDao()

        viewModelFactory = ChatViewViewModelFactory(args.contactID, dataSource, application)
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewViewModel::class.java]
        binding.chatViewViewModel = viewModel
        binding.lifecycleOwner = this

        val adapter = MessageItemAdapter(requireContext())
        binding.messageView.adapter = adapter

//        viewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
//            messages?.let {
//                adapter.submitList(messages)
//            }
//        })

        viewModel.contact.observe(viewLifecycleOwner, Observer { cnt ->
            cnt?.let {
                adapter.submitList(cnt.messages)
            }
        })

        return binding.root
    }
}