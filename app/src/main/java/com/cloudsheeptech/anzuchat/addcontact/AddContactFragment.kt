package com.cloudsheeptech.anzuchat.addcontact

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.chatlist.ChatUserItemAdapter
import com.cloudsheeptech.anzuchat.database.ApollonDatabase
import com.cloudsheeptech.anzuchat.databinding.FragmentAddContactBinding

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
        // Setting the activity as lifecycle owner so that it is not destroyed when navigating back to the main screen
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[AddContactViewModel::class.java]

        binding.addConctactViewModel = viewModel
        // Destroy the fragment after navigating back to the main screen
        binding.lifecycleOwner = this

        val adapter = ChatUserItemAdapter(ChatUserItemAdapter.ChatUserItemListener { contactId ->
//            Log.i("ChatListFragment", "Got user $contactId")
            viewModel.addContact(contactId)
        }, requireContext())
        binding.contactList.adapter = adapter

        binding.searchContactButton.setOnClickListener {
            // Issuing a request to the server and displaying the users
            viewModel.searchContacts()
        }

        viewModel.contacts.observe(viewLifecycleOwner, Observer { newList ->
            if (newList == null) {
                adapter.submitList(listOf())
                adapter.notifyDataSetChanged()
            } else {
                adapter.submitList(newList)
                // Should be "okay" for now since the list of potential contacts is wiped and not that long either
                adapter.notifyDataSetChanged()
            }
        })

        viewModel.navigateToContactListEvent.observe(viewLifecycleOwner, Observer {navigate ->
            if(navigate) {
                viewModel.onContactListNavigated()
                findNavController().navigate(R.id.navigation_chat_list)
            }
        })

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer {hide ->
            if(hide) {
                val inputManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                if (requireActivity().currentFocus != null) {
                    inputManager.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
                    viewModel.hideKeyboardDone()
                }
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adding the back button to the toolbar
        binding.addContactToolbar.setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }
    }
}