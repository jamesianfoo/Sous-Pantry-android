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
    val quantity: String?,
    val category: String?,
)

data class SuggestedMeal(
    val title           : String,
    val description     : String,
    val cuisine         : String,
    val prepTime        : String,
    val difficulty      : String,
    val ingredients     : List<String>,
    val usedPantryItems : List<String>,
    val instructions    : List<String>,
    val imageQuery      : String,
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

data class ShoppingItem(
    val id      : String   = java.util.UUID.randomUUID().toString(),
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
