package com.souspantry.app.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// ── Room entity (mirrors PantryItem SwiftData model) ─────────────────────────

@Entity(tableName = "pantry_items")
data class PantryItem(
    @PrimaryKey val id           : String  = java.util.UUID.randomUUID().toString(),
    val name                     : String,
    val brand                    : String? = null,
    val category                 : String? = null,
    val quantity                 : Int     = 1,
    val notes                    : String? = null,
    val expiryDate               : Long?   = null,   // epoch millis
    val dateAdded                : Long    = System.currentTimeMillis(),
)

// ── API response models ───────────────────────────────────────────────────────

data class IdentifyResponse(
    val name       : String?,
    val brand      : String?,
    val category   : String?,
    val quantity   : String?,
    val expiry_date: String?,
    val confidence : Float,
)

data class ReceiptLineItem(
    val name    : String,
    /** Pack size of ONE unit, e.g. "500g" — not how many were bought. */
    val quantity: String?,
    val category: String?,
    /** How many units this line bought ("2 x 500g" → 2). Always ≥ 1. */
    val count   : Int     = 1,
    /** Unit price in the receipt's own currency, when shown. */
    val price   : Double? = null,
)

data class SuggestedMeal(
    val title           : String,
    val description     : String,
    val cuisine         : String,
    val prepTime        : String,
    val cookTime        : String       = "",
    val servings        : Int          = 2,
    val difficulty      : String,
    val ingredients     : List<String>,
    val usedPantryItems : List<String>,
    val instructions    : List<String>,
    val imageQuery      : String,
    /** External recipe page, when this card came from (or echoes) a real site. */
    val sourceURL       : String       = "",
    /** Bare domain of [sourceURL], e.g. "recipetineats.com". */
    val sourceSite      : String       = "",
)

data class TrendingRecipe(
    val title          : String,
    val description    : String,
    val cuisine        : String,
    val platform       : String,
    val prepTime       : String,
    val difficulty     : String,
    val trendingStats  : String,
    val ingredients    : List<String>,
    val imageQuery     : String,
)

data class AdventurousRecipe(
    val title               : String,
    val description         : String,
    val cuisine             : String,
    val prepTime            : String,
    val difficulty          : String,
    val ingredients         : List<String>,
    val pantryIngredients   : List<String>,
    val missingIngredients  : List<String>,
    val matchPercent        : Int,
    val imageQuery          : String,
)

/** [ShoppingItem.reason] prefix for items added from a recipe ("For <recipe title>"). */
const val RECIPE_REASON_PREFIX = "For "

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey val id : String = java.util.UUID.randomUUID().toString(),
    val name    : String,
    val category: String?,
    val quantity: String?,
    val priority: String,
    val reason  : String?,
    var checked : Boolean  = false,
)

data class BarcodeResult(
    val name    : String?,
    val brand   : String?,
    val category: String?,
    val quantity: String?,
    val imageUrl: String?,
)

data class ScannedRecipe(
    val name        : String,
    val description : String?      = null,
    val cuisine     : String?      = null,
    val difficulty  : String?      = null,
    val prepTime    : String?      = null,
    val cookTime    : String?      = null,
    val servings    : Int?         = null,
    val ingredients : List<String> = emptyList(),
    val steps       : List<String> = emptyList(),
)
