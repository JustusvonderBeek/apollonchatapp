package com.cloudsheeptech.anzuchat.database

import androidx.lifecycle.LiveData
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

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

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

}