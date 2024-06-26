package com.cloudsheeptech.anzuchat.chatview

import android.app.Activity
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.view.MenuProvider
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.cloudsheeptech.anzuchat.R
import com.cloudsheeptech.anzuchat.databinding.FragmentChatViewBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

class ChatViewFragment : Fragment(), MenuProvider {
    private lateinit var viewModel : ChatViewViewModel
    private lateinit var viewModelFactory : ChatViewViewModelFactory

    private lateinit var binding : FragmentChatViewBinding

    // Receiving the given safearg containing the actual pressed contactID
    private val args : ChatViewFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.chat_view_menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.manageUserAction -> {
                viewModel.showContactInformation()
                return true
            }
            R.id.removeUserAction -> {
                viewModel.removeUser()
                return true
            }
        }
        return false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_chat_view, container, false)

        // Creating the dropdown menu in the toolbar
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        val application = requireNotNull(this.activity).application
        viewModelFactory = ChatViewViewModelFactory(args.contactID, application)
        // We don't want the view model to survive navigate up. Therefore using "this"
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewViewModel::class.java]
        binding.chatViewViewModel = viewModel
        binding.lifecycleOwner = requireActivity()

        val adapter = MessageItemAdapter()
        binding.messageView.adapter = adapter

        lifecycleScope.launch {
            viewModel.messages.collectLatest { adapter.submitData(it) }
        }

        viewModel.hideKeyboard.observe(viewLifecycleOwner, Observer {hide ->
            if(hide) {
                val inputManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                if (requireActivity().currentFocus != null) {
                    inputManager.hideSoftInputFromWindow(requireActivity().currentFocus!!.windowToken, 0)
                    viewModel.hideKeyboardDone()
                }
            }
        })

        viewModel.userImage.observe(viewLifecycleOwner, Observer { path ->
            Log.i("ChatViewFragment", "Path: $path")
            if (path == "") {
                return@Observer
            }
            if (!File(path).exists()) {
                return@Observer
            }
            Log.i("ChatViewFragment", "Changing image path")
            try {
                val image = BitmapFactory.decodeFile(path)
//                if (image.height > 60) {
//                    // Resize to fit the image better
//                    val scaledImage = Bitmap.createScaledBitmap(image, 60, 60, true)
//                    image = scaledImage
//                }
                Glide.with(this)
                    .load(image).centerCrop()
                    .into(binding.chatviewToolbarImage);
            } catch (ex : Exception) {
                Log.i("ChatViewFragment", "Failed to load image from $path: $ex")
                binding.chatviewToolbarImage.setImageResource(R.drawable.ic_user)
            }
        })


        viewModel.lastOnline.observe(viewLifecycleOwner, Observer { lastOnline ->
            lastOnline?.let {
                binding.chatviewToolbarSubtitle.text = lastOnline
            }
        })

        // TODO: Fix with paginated view
        viewModel.scrollToBottom.observe(viewLifecycleOwner, Observer { scroll ->
           scroll?.let {
               if (it > 0) {
                   binding.messageView.smoothScrollToPosition(scroll)
                   viewModel.ScrolledBottom()
               }
           }
        })

        viewModel.navigateUp.observe(viewLifecycleOwner, Observer { navigate ->
            if (navigate) {
                viewModel.navigatUpDone()
                findNavController().navigateUp()
            }
        })

        viewModel.showRejectHint.observe(viewLifecycleOwner, Observer {hint ->
            if (hint) {
                binding.rejectHintStack.visibility = View.VISIBLE
            } else {
                binding.rejectHintStack.visibility = View.GONE
            }
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Adding the toolbar to the view
        binding.chatviewToolbar.inflateMenu(R.menu.chat_view_menu)
        binding.chatviewToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.manageUserAction -> {
                    viewModel.showContactInformation()
                    true
                }
                R.id.removeUserAction -> {
                    viewModel.removeUser()
                    true
                }
            }
            false
        }

        // Adding the back button to the toolbar
        binding.chatviewToolbar.setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }
    }
}