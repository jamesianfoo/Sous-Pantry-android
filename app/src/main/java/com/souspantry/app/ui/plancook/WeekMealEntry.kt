package com.souspantry.app.ui.plancook

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.souspantry.app.data.models.SuggestedMeal
import java.time.LocalDate
import java.util.UUID

/**
 * A meal scheduled on a specific date in "My Plans".
 * Mirrors iOS WeekMealEntry (SwiftData model). Persisted via Room.
 */
@Entity(tableName = "week_meal_entries")
data class WeekMealEntry(
    @PrimaryKey val id : String     = UUID.randomUUID().toString(),
    val title        : String,
    val description  : String       = "",
    val cuisine      : String       = "",
    val prepTime     : String       = "",
    val cookTime     : String       = "",
    val difficulty   : String       = "",
    val ingredients  : List<String> = emptyList(),
    val instructions : List<String> = emptyList(),
    val scheduledDate: LocalDate    = LocalDate.now(),
    val isUserAdded  : Boolean      = false,
)

/** Convenience factory — creates a WeekMealEntry from a SuggestedMeal + date. */
fun weekMealEntryFrom(meal: SuggestedMeal, date: LocalDate) = WeekMealEntry(
    title         = meal.title,
    description   = meal.description,
    cuisine       = meal.cuisine,
    prepTime      = meal.prepTime,
    cookTime      = meal.cookTime,
    difficulty    = meal.difficulty,
    ingredients   = meal.ingredients,
    instructions  = meal.instructions,
    scheduledDate = date,
)

/** Convenience factory — creates a user-typed custom meal entry. */
fun weekMealEntryCustom(name: String, date: LocalDate) = WeekMealEntry(
    title         = name.trim(),
    scheduledDate = date,
    isUserAdded   = true,
)
