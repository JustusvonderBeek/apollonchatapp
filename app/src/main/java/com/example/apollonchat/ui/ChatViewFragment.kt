package com.example.apollonchat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.example.apollonchat.R
import com.example.apollonchat.databinding.FragmentChatViewBinding

/**
 * A simple [Fragment] subclass.
 * Use the [ChatViewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ChatViewFragment : Fragment() {
    // TODO: Rename and change types of parameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding : FragmentChatViewBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_chat_view, container, false)
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ChatViewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChatViewFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}