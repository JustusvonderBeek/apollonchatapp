package com.example.apollonchat.chatview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.databinding.DataBindingUtil
import com.example.apollonchat.chatview.ChatViewViewModel
import com.example.apollonchat.chatview.ChatViewViewModelFactory
import com.example.apollonchat.databinding.FragmentChatViewBinding
class ChatViewFragment : Fragment() {
    private lateinit var viewModel : ChatViewViewModel
    private lateinit var viewModelFactory : ChatViewViewModelFactory

    private lateinit var binding : FragmentChatViewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }
}