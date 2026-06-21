package com.souspantry.app.ui.plancook

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.souspantry.app.data.models.SuggestedMeal
import java.util.UUID

/**
 * A recipe the user bookmarked from a Sous AI suggestion.
 * Distinct from MyRecipe (user-authored) — this just stores the AI suggestion
 * so it can be revisited. Mirrors iOS SavedRecipe (SwiftData). Persisted via Room.
 */
@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey val id : String     = UUID.randomUUID().toString(),
    val title        : String,
    val description  : String       = "",
    val cuisine      : String       = "",
    val prepTime     : String       = "",
    val difficulty   : String       = "",
    val ingredients  : List<String> = emptyList(),
    val instructions : List<String> = emptyList(),
    val savedAt      : Long         = System.currentTimeMillis(),
)

fun savedRecipeFrom(meal: SuggestedMeal) = SavedRecipe(
    title        = meal.title,
    description  = meal.description,
    cuisine      = meal.cuisine,
    prepTime     = meal.prepTime,
    difficulty   = meal.difficulty,
    ingredients  = meal.ingredients,
    instructions = meal.instructions,
)
