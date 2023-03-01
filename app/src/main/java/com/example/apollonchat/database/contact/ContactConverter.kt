package com.example.apollonchat.database.contact

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ContactConverter {
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