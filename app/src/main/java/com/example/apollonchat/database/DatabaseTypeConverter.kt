package com.example.apollonchat.database

import androidx.room.TypeConverter
import com.example.apollonchat.database.contact.Contact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseTypeConverter {
    @TypeConverter
    fun listOfStringToString(list : List<String>) : String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun stringToList(json : String?) : List<String> {
        if (json == null) {
            return emptyList()
        }
        return Gson().fromJson(json, object : TypeToken<List<String>>() {}.type)
    }
}