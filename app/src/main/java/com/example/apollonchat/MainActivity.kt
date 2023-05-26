package com.example.apollonchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.apollonchat.configuration.NetworkConfiguration
import com.example.apollonchat.databinding.ActivityMainBinding
import com.example.apollonchat.networking.ApollonProtocolHandler.ApollonProtocolHandler
import com.example.apollonchat.networking.Networking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        // This does only work when the Navigation component is inside a <fragment> tag, NOTHING ELSE!
        val navController = findNavController(R.id.navHostFragment)

        // Decide what fragments should be "top-level" and SHOULD NOT! have a "back" arrow
        val appBarConfiguration = AppBarConfiguration.Builder(setOf(R.id.navigation_chat_list, R.id.navigation_user_creation)).build()
        // The back arrow in the actionBar (title bar on top)
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)

        // Starting the networking service here since it will be used throughout the whole app
        val networkConfig = Networking.Configuration()
        Networking.initialize(networkConfig)
        Networking.start(applicationContext)
        ApollonProtocolHandler.initialize(4227087116u, application)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }
}