package com.example.apollonchat.chatlist

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.apollonchat.R
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.databinding.FragmentChatListBinding

class ChatListFragment : Fragment() {
    private lateinit var viewModel : ChatListViewModel
    private lateinit var viewModelFactory : ChatListViewModelFactory

    private lateinit var binding : FragmentChatListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_list, container, false)

        // Getting all database DAOs and bring them into the networking and start it
        val application = requireNotNull(this.activity).application
        val contactDao = ApollonDatabase.getInstance(application).contactDao()
        val userDao = ApollonDatabase.getInstance(application).userDao()
        val messageDao = ApollonDatabase.getInstance(application).messageDao()

        viewModelFactory = ChatListViewModelFactory(contactDao, userDao, messageDao, application)
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatListViewModel::class.java]
        binding.chatListViewModel = viewModel
        binding.lifecycleOwner = this

        val adapter = ChatUserItemAdapter(ChatUserItemAdapter.ChatUserItemListener { contactId ->
//            Log.i("ChatListFragment", "Got user $contactId")
            viewModel.onContactClicked(contactId)
        }, requireContext())
        binding.userlist.adapter = adapter

        // In case no user exists, navigate to the screen which creates a user
        viewModel.user.observe(viewLifecycleOwner, Observer { newUser ->
            if (newUser == null) {
                findNavController().navigate(ChatListFragmentDirections.actionNavigationChatListToNavigationUserCreation())
            }
        })

        viewModel.contacts.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
//            Log.i("ChatListFragment", "Updated users - now " + adapter.itemCount)
        })

        viewModel.navigateToContactChat.observe(viewLifecycleOwner, Observer {contactID ->
            contactID?.let {
                if(contactID != -1L) {
                    viewModel.onContactNavigated()
                    this.findNavController().navigate(ChatListFragmentDirections.actionChatListFragmentToChatViewFragment(contactID))
                }
            }
        })

        binding.addContact.setOnClickListener{
            // Navigating to the add Contact screen and allow the user to add a new contact
//            viewModel.clearUser()
            this.findNavController().navigate(ChatListFragmentDirections.actionNavigationChatListToAddContactFragment())
        }

        return binding.root
    }
}