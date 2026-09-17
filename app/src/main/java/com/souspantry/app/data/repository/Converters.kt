package com.souspantry.app.data.repository

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.plancook.MyRecipeSource
import java.time.LocalDate

/**
 * Room TypeConverters for the Plan & Cook entities.
 * Handles List<String> (JSON), List<SuggestedMeal> (JSON), LocalDate
 * (epoch-day Long), and the MyRecipeSource enum (name string).
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toStringList(value: String): List<String> =
        runCatching {
            gson.fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromSuggestedMealList(value: List<SuggestedMeal>): String = gson.toJson(value)

    @TypeConverter
    fun toSuggestedMealList(value: String): List<SuggestedMeal> =
        runCatching {
            gson.fromJson<List<SuggestedMeal>>(value, object : TypeToken<List<SuggestedMeal>>() {}.type)
        }.getOrDefault(emptyList())

    @TypeConverter
    fun fromLocalDate(date: LocalDate): Long = date.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)

    @TypeConverter
    fun fromRecipeSource(source: MyRecipeSource): String = source.name

    @TypeConverter
    fun toRecipeSource(value: String): MyRecipeSource =
        runCatching { MyRecipeSource.valueOf(value) }.getOrDefault(MyRecipeSource.MY_CREATION)
}
