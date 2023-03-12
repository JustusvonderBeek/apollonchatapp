package com.example.apollonchat

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.apollonchat.databinding.ActivityMainBinding
import com.google.android.material.appbar.AppBarLayout

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
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.navHostFragment)
        return navController.navigateUp()
    }
}