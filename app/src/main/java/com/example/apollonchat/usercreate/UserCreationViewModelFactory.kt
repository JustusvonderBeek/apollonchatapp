package com.example.apollonchat.usercreate

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.apollonchat.database.contact.ContactDatabaseDao
import com.example.apollonchat.database.user.UserDatabaseDao

class UserCreationViewModelFactory(val database : UserDatabaseDao, val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserCreationViewModel::class.java)) {
            return UserCreationViewModel(database, application) as T
        }
        throw java.lang.IllegalArgumentException("Unknown init class")
    }

}