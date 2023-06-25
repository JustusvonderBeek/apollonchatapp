package com.cloudsheeptech.anzuchat.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseTypeConverter {
    @TypeConverter
    fun listOfStringToString(list : MutableList<String>) : String {
        return Gson().toJson(list)
    }

    @TypeConverter
    fun stringToList(json : String?) : MutableList<String> {
        if (json == null) {
            return mutableListOf()
        }
        return Gson().fromJson(json, object : TypeToken<MutableList<String>>() {}.type)
    }
}