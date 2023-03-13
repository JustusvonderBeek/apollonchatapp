package com.example.apollonchat.addcontact

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.apollonchat.R
import com.example.apollonchat.chatlist.ChatListFragmentDirections
import com.example.apollonchat.chatlist.ChatUserItemAdapter
import com.example.apollonchat.database.ApollonDatabase
import com.example.apollonchat.databinding.FragmentAddContactBinding

class AddContactFragment : Fragment() {

    private lateinit var viewModel: AddContactViewModel
    private lateinit var viewModelFactory: AddContactViewModelFactory

    private lateinit var binding : FragmentAddContactBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_add_contact, container, false)

        val application = requireNotNull(this.activity).application
        val database = ApollonDatabase.getInstance(application).contactDao()
        val uDatabase = ApollonDatabase.getInstance(application).userDao()

        viewModelFactory = AddContactViewModelFactory(uDatabase, database)
        viewModel = ViewModelProvider(this, viewModelFactory)[AddContactViewModel::class.java]

        binding.addConctactViewModel = viewModel
        binding.lifecycleOwner = this

        val adapter = ChatUserItemAdapter(ChatUserItemAdapter.ChatUserItemListener { contactId ->
//            Log.i("ChatListFragment", "Got user $contactId")
            viewModel.addContact(contactId)
        }, requireContext())
        binding.contactList.adapter = adapter

        binding.searchContactButton.setOnClickListener {
            // Issueing a request to the server and displaying the users
            viewModel.searchContacts()
        }

        viewModel.contacts.observe(viewLifecycleOwner, Observer { newList ->
            newList?.let {
                adapter.submitList(newList)
            }
        })

        viewModel.navigateToContactListEvent.observe(viewLifecycleOwner, Observer {navigate ->
            if(navigate) {
                viewModel.onContactListNavigated()
                findNavController().navigate(R.id.navigation_chat_list)
            }
        })

        return binding.root
    }
}