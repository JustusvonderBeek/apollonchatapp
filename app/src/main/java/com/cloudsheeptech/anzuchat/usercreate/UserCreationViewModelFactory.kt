package com.cloudsheeptech.anzuchat.usercreate

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cloudsheeptech.anzuchat.database.ApollonDatabase

class UserCreationViewModelFactory(val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserCreationViewModel::class.java)) {
            val userDb = ApollonDatabase.getInstance(application).userDao()
            return UserCreationViewModel(userDb, application) as T
        }
        throw java.lang.IllegalArgumentException("Unknown init class")
    }

}