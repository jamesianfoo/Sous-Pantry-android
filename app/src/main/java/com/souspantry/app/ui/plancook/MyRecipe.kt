package com.souspantry.app.ui.plancook

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** Origin of a MyRecipe entry. Mirrors iOS MyRecipeSource. */
enum class MyRecipeSource { TWEAKED, MY_CREATION, PHOTO_SCAN }

/**
 * A user-owned recipe in the My Recipes collection.
 * Distinct from a bookmarked AI suggestion — these are recipes the user has
 * authored, tweaked, or scanned from a photo. Mirrors iOS MyRecipe (SwiftData).
 * Persisted via Room.
 */
@Entity(tableName = "my_recipes")
data class MyRecipe(
    @PrimaryKey val id : String         = UUID.randomUUID().toString(),
    val title        : String,
    val description  : String           = "",
    val cuisine      : String           = "",
    val prepTime     : String           = "",   // e.g. "15 min"
    val cookTime     : String           = "",
    val difficulty   : String           = "",
    val ingredients  : List<String>     = emptyList(),
    val instructions : List<String>     = emptyList(),
    val servings     : Int              = 2,
    val notes        : String           = "",
    val source       : MyRecipeSource   = MyRecipeSource.MY_CREATION,
    val createdAt    : Long             = System.currentTimeMillis(),
)
