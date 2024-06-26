package com.cloudsheeptech.anzuchat.usercreate

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
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.databinding.FragmentUserCreationBinding

class UserCreationFragment : Fragment(), MenuProvider {

    private lateinit var viewModel : UserCreationViewModel
    private lateinit var viewModelFactory : UserCreationViewModelFactory

    private lateinit var binding : FragmentUserCreationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.debug_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.clearDatabaseAction -> {
                Log.i("UserCreationFragment", "Clearing the database")
                return true
            }
            R.id.clearUserAction -> {
                Log.i("UserCreationFragment", "Clearing User")
                return true
            }
            R.id.reconnectNetworkAction -> {
                Log.i("UserCreationFragment", "Reconnecting the Network")
                viewModel.reconnectNetwork()
                return true
            }
        }
        return false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
    {
        // Add the menu into the app bar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_user_creation, container, false)

        val application = requireNotNull(this.activity).application
        viewModelFactory = UserCreationViewModelFactory(application)
        viewModel = ViewModelProvider(this, viewModelFactory)[UserCreationViewModel::class.java]

        binding.userCreationViewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

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

        viewModel.waiting.observe(viewLifecycleOwner, Observer {waiting ->
            if (waiting) {
                binding.indeterminateBar.visibility = View.VISIBLE
            } else {
                binding.indeterminateBar.visibility = View.INVISIBLE
            }
        })

        binding.button.setOnClickListener {
            viewModel.createUser()
        }

        // Inflate the layout for this fragment
        return binding.root
    }

}