package com.example.apollonchat.chatview

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
        val uDataSource = ApollonDatabase.getInstance(application).userDao()
        val mDataSource = ApollonDatabase.getInstance(application).messageDao()

        viewModelFactory = ChatViewViewModelFactory(args.contactID, dataSource, uDataSource, mDataSource, application)
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[ChatViewViewModel::class.java]
        binding.chatViewViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        val adapter = MessageItemAdapter(requireContext())
        binding.messageView.adapter = adapter

        viewModel.messages.observe(viewLifecycleOwner, Observer { messages ->
            messages?.let {
                adapter.submitList(messages)
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

//        viewModel.contact.observe(viewLifecycleOwner, Observer { cnt ->
//            cnt?.let {
//                adapter.submitList(cnt.messages)
////                adapter.notifyDataSetChanged()
//            }
//        })

        return binding.root
    }
}