package com.souspantry.app.services

import com.souspantry.app.data.models.*
import retrofit2.http.*

/**
 * Retrofit interface matching the Node.js backend routes.
 * All Claude calls go through the backend — API key stays server-side.
 */
interface ApiService {

    // ── Item identification ───────────────────────────────────────────────────

    @POST("api/identify")
    suspend fun identifyItem(@Body body: Map<String, String>): IdentifyResponse

    // ── Receipt parsing ───────────────────────────────────────────────────────

    @POST("api/receipt/image")
    suspend fun parseReceiptImage(@Body body: Map<String, String>): List<ReceiptLineItem>

    @POST("api/receipt/text")
    suspend fun parseReceiptText(@Body body: Map<String, String>): List<ReceiptLineItem>

    // ── Meal plan ─────────────────────────────────────────────────────────────

    @POST("api/meals/generate")
    suspend fun generateMeals(@Body body: Map<String, Any>): List<SuggestedMeal>

    @POST("api/meals/trending")
    suspend fun fetchTrending(@Body body: Map<String, String>): List<TrendingRecipe>

    @POST("api/meals/pantry")
    suspend fun fetchPantryMeals(@Body body: Map<String, Any>): List<SuggestedMeal>

    @POST("api/meals/adventurous")
    suspend fun fetchAdventurous(@Body body: Map<String, Any>): List<AdventurousRecipe>

    @POST("api/meals/suggested")
    suspend fun fetchSuggestedMeals(@Body body: Map<String, String>): List<SuggestedMeal>

    // ── Shopping ──────────────────────────────────────────────────────────────

    @POST("api/shopping/generate")
    suspend fun generateShoppingList(@Body body: Map<String, Any>): List<ShoppingItem>

    @POST("api/shopping/staples")
    suspend fun generateStaples(@Body body: Map<String, Any>): List<ShoppingItem>

    // ── Barcode ───────────────────────────────────────────────────────────────

    @GET("api/barcode/{code}")
    suspend fun lookupBarcode(@Path("code") code: String): BarcodeResult

    // ── Recipe scan ─────────────────────────────────────────────────────────

    @POST("api/recipe/scan-text")
    suspend fun scanRecipeText(@Body body: Map<String, String>): ScannedRecipe
}
