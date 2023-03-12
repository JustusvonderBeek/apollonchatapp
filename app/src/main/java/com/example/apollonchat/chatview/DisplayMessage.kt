package com.example.apollonchat.chatview

import androidx.databinding.ObservableField
import androidx.lifecycle.MutableLiveData

data class DisplayMessage(
    var ID : Int,
    var own : Boolean,
    var content : String,
    var timestamp : String,
)
