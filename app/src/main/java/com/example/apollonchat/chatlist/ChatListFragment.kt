package com.example.apollonchat.chatlist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.apollonchat.R
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.databinding.FragmentChatListBinding

class ChatListFragment : Fragment(), MenuProvider {
    private lateinit var viewModel : ChatListViewModel
    private lateinit var viewModelFactory : ChatListViewModelFactory

    private lateinit var binding : FragmentChatListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    // Methods to execute when app bar item is clicked
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.debug_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.clearDatabaseAction -> {
                Log.i("ChatListFragment", "Clearing the database")
                viewModel.clearContacts()
                return true
            }
            R.id.clearUserAction -> {
                Log.i("ChatListFragment", "Clearing User")
                viewModel.clearUser()
                return true
            }
            R.id.reconnectNetworkAction -> {
                Log.i("ChatListFragment", "Reconnecting the Network")
                return true
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_list, container, false)

        // Enabling the debug menu in the app bar (3 dots)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        // Getting all database DAOs and bring them into the networking and start it
        val application = requireNotNull(this.activity).application
        val contactDao = ApollonDatabase.getInstance(application).contactDao()
        val userDao = ApollonDatabase.getInstance(application).userDao()
        val messageDao = ApollonDatabase.getInstance(application).messageDao()

        viewModelFactory = ChatListViewModelFactory(contactDao, userDao, messageDao, application)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ChatListViewModel::class.java]
        binding.chatListViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        val adapter = ChatUserItemAdapter(ChatUserItemAdapter.ChatUserItemListener { contactId ->
            Log.i("ChatListFragment", "Got user $contactId")
            viewModel.onContactClicked(contactId)
        }, requireContext())
        binding.userlist.adapter = adapter

        // In case no user exists, navigate to the screen which creates a user
        viewModel.user.observe(viewLifecycleOwner, Observer { newUser ->
            if (newUser == null) {
                findNavController().navigate(ChatListFragmentDirections.actionNavigationChatListToNavigationUserCreation())
            }
        })

        // Problems with automatically added users pointing to wrong ID and wrong chat
        viewModel.contacts.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
                // TODO: Make smarter
                adapter.notifyDataSetChanged()
            }
//            Log.i("ChatListFragment", "Updated users - now " + adapter.itemCount)
        })

        viewModel.navigateToContactChat.observe(viewLifecycleOwner, Observer {contactID ->
            contactID?.let {
                if(contactID != -1L) {
                    this.findNavController().navigate(ChatListFragmentDirections.actionChatListFragmentToChatViewFragment(contactID))
                    viewModel.onContactNavigated()
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