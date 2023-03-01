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

/**
 * A simple [Fragment] subclass.
 * Use the [ChatListFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
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

        val application = requireNotNull(this.activity).application
        val dataSource = ApollonDatabase.getInstance(application).contactDatabaseDao
        val uDataSource = ApollonDatabase.getInstance(application).userDatabaseDao

        viewModelFactory = ChatListViewModelFactory(dataSource, uDataSource, application)
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatListViewModel::class.java]
        binding.chatListViewModel = viewModel
        binding.lifecycleOwner = this

        val adapter = ChatUserItemAdapter(ChatUserItemAdapter.ChatUserItemListener { contactId ->
//            Log.i("ChatListFragment", "Got user $contactId")
            viewModel.onContactClicked(contactId)
        }, requireContext())
        binding.userlist.adapter = adapter

        viewModel.users.observe(viewLifecycleOwner, Observer {
            it?.let {
                adapter.submitList(it)
            }
            Log.i("ChatListFragment", "Updated users - now " + adapter.itemCount)
        })

        viewModel.navigateToContactChat.observe(viewLifecycleOwner, Observer {contactID ->
            contactID?.let {
                if(contactID != -1L) {
                    viewModel.onContactNavigated()
                    this.findNavController().navigate(ChatListFragmentDirections.actionChatListFragmentToChatViewFragment(contactID))
                }
            }
        })

        binding.addUser.setOnClickListener{
            viewModel.addUser()
        }

        return binding.root
    }
}