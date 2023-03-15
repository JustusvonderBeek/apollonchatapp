package com.example.apollonchat.usercreate

import android.os.Bundle
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
import com.example.apollonchat.databinding.FragmentUserCreationBinding

class UserCreationFragment : Fragment() {

    private lateinit var viewModel : UserCreationViewModel
    private lateinit var viewModelFactory : UserCreationViewModelFactory

    private lateinit var binding : FragmentUserCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_creation, container, false)

        val application = requireNotNull(this.activity).application
        val navigation = findNavController()
        val dataSource = ApollonDatabase.getInstance(application).userDao()

        viewModelFactory = UserCreationViewModelFactory(dataSource, application)
        viewModel = ViewModelProvider(this, viewModelFactory)[UserCreationViewModel::class.java]

        binding.userCreationViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        viewModel.navigateUserListEvent.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                viewModel.onUserListNavigated()
//                findNavController().navigate(R.id.navigation_chat_list)
                findNavController().navigateUp()
            }
        })

        viewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                viewModel.onUserListNavigated()
                findNavController().navigateUp()
            }
        })

        binding.button.setOnClickListener{
            viewModel.createUser()
        }

        // Inflate the layout for this fragment
        return binding.root
    }

}