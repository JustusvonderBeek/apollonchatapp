package com.cloudsheeptech.anzuchat.chatlist

import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.databinding.FragmentChatListBinding
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar

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
        // DO NOT USE THE SETUP SUPPORT TOOLBAR FOR FRAGMENT OWNED TOOLBARS!

        // Getting all database DAOs and bring them into the networking and start it
        val application = requireNotNull(this.activity).application

        viewModelFactory = ChatListViewModelFactory(application)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ChatListViewModel::class.java]
        binding.chatListViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

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

        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri == null) {
                Log.i("ChatListFragment", "No image selected")
                return@registerForActivityResult
            }
            Log.i("ChatListFragment", "Selected Image: $uri")
            val resolver = application.contentResolver
            var fileContent : ByteArray
            resolver.openInputStream(uri).use {
                fileContent = it!!.readBytes()
            }
            viewModel.onImagePicked(fileContent)
        }

        viewModel.pickImage.observe(viewLifecycleOwner) {
            if (it) {
                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.chatlistToolbar.inflateMenu(R.menu.debug_menu)
        // For fragment owned toolbars add the click listener directly here7
        binding.chatlistToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.clearDatabaseAction -> {
                    Log.i("ChatListFragment", "Clearing the database")
                    viewModel.clearContacts()
                    viewModel.clearMessages()
                    true
                }
                R.id.clearUserAction -> {
                    Log.i("ChatListFragment", "Clearing User")
                    viewModel.clearUser()
                    true
                }
                R.id.reconnectNetworkAction -> {
                    Log.i("ChatListFragment", "Reconnecting the Network")
                    viewModel.reconnectNetwork()
                    true
                }
                R.id.addUserAction -> {
                    this.findNavController().navigate(ChatListFragmentDirections.actionNavigationChatListToAddContactFragment())
                    true
                }
                // Remove the button from the main UI
                else -> false
            }
        }
        // In this view we don't need an up or back button for the navigation!
    }
}