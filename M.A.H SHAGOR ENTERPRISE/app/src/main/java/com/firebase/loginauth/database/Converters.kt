package com.firebase.loginauth.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromCylinderItemList(value: List<CylinderItem>?): String {
        return Gson().toJson(value)
    }
    
    @TypeConverter
    fun toCylinderItemList(value: String): List<CylinderItem>? {
        val listType = object : TypeToken<List<CylinderItem>>() {}.type
        return Gson().fromJson(value, listType)
    }
}

data class CylinderItem(
    val brand: String,
    val size: String,
    val quantity: Int,
    val pricePerUnit: Double
)
