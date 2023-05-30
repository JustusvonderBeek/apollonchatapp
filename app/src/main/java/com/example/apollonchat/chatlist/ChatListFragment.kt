package com.example.apollonchat.chatlist

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBindings
import com.example.apollonchat.R
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.databinding.FragmentChatListBinding
import com.example.apollonchat.networking.Networking

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
                viewModel.reconnectNetwork()
                return true
            }
            R.id.addUserAction -> {
                this.findNavController().navigate(ChatListFragmentDirections.actionNavigationChatListToAddContactFragment())
            }
            // Remove the button from the main UI
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

        viewModelFactory = ChatListViewModelFactory(application)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ChatListViewModel::class.java]
        binding.chatListViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Chats"
        (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = ""
//        (requireActivity() as AppCompatActivity).addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)


        // Move to the view that shows the chat with the clicked user
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

        // Problems with updating the list when new user is added
        // Maybe need to add another visual representation of
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

        return binding.root
    }
}