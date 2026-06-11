package com.souspantry.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.AdventurousRecipe
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.models.TrendingRecipe
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    // Profile + pantry
    val userName           : String          = "",
    val pantryItems        : List<PantryItem> = emptyList(),

    // Recipe carousels (existing API wiring)
    val suggestedRecipes   : List<SuggestedMeal>    = emptyList(),
    val trendingRecipes    : List<TrendingRecipe>   = emptyList(),
    val pantryRecipes      : List<SuggestedMeal>    = emptyList(),
    val adventurousRecipes : List<AdventurousRecipe> = emptyList(),

    val suggestedLoading   : Boolean = false,
    val trendingLoading    : Boolean = false,
    val pantryLoading      : Boolean = false,
    val adventurousLoading : Boolean = false,

    val suggestedError     : Boolean = false,
    val trendingError      : Boolean = false,
    val pantryError        : Boolean = false,
    val adventurousError   : Boolean = false,
) {
    /** Items expiring within 7 days (including expired), sorted by expiry ascending. */
    val auditItems: List<PantryItem>
        get() {
            val nowMs   = System.currentTimeMillis()
            val cutoff  = nowMs + 7L * 86_400_000L
            return pantryItems
                .filter { it.expiryDate != null && it.expiryDate <= cutoff }
                .sortedBy { it.expiryDate }
        }

    /** Top categories by item count with fraction (0-1) of pantry. */
    val categoryBreakdown: List<Triple<String, Float, Int>>
        get() {
            if (pantryItems.isEmpty()) return emptyList()
            val byCat = pantryItems.groupBy { it.category ?: "Other" }
                .map { (cat, list) -> cat to list.size }
                .sortedByDescending { it.second }
            val total = pantryItems.size.toFloat()
            return byCat.map { (cat, count) ->
                Triple(cat, count / total, count)
            }
        }

    /** Estimated number of cookable meals based on pantry size. Mirrors iOS rough heuristic. */
    val estimatedRecipeCount: Int
        get() = (pantryItems.size * 1.2).toInt().coerceAtLeast(if (pantryItems.isNotEmpty()) 1 else 0)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api   : ApiService,
    private val repo  : PantryRepository,
    private val prefs : UserPreferencesRepository,
) : ViewModel() {

    private val _recipeState = MutableStateFlow(HomeState())

    val state: StateFlow<HomeState> = combine(
        _recipeState,
        repo.items,
        prefs.userName,
    ) { recipeState, items, name ->
        recipeState.copy(pantryItems = items, userName = name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    init { loadAll() }

    fun loadAll() {
        loadSuggested()
        loadTrending()
        loadPantryMeals()
        loadAdventurous()
    }

    fun loadSuggested() = viewModelScope.launch {
        _recipeState.update { it.copy(suggestedLoading = true, suggestedError = false) }
        runCatching { api.fetchSuggestedMeals(emptyMap()) }
            .onSuccess  { meals -> _recipeState.update { it.copy(suggestedRecipes = meals, suggestedLoading = false) } }
            .onFailure  {        _recipeState.update { it.copy(suggestedError = true,    suggestedLoading = false) } }
    }

    fun loadTrending() = viewModelScope.launch {
        _recipeState.update { it.copy(trendingLoading = true, trendingError = false) }
        runCatching { api.fetchTrending(emptyMap()) }
            .onSuccess  { r -> _recipeState.update { it.copy(trendingRecipes = r, trendingLoading = false) } }
            .onFailure  {     _recipeState.update { it.copy(trendingError = true, trendingLoading = false) } }
    }

    fun loadPantryMeals() = viewModelScope.launch {
        _recipeState.update { it.copy(pantryLoading = true, pantryError = false) }
        val items = repo.items.first()
        val body  = mapOf("pantryItems" to items.map { mapOf("name" to it.name) })
        runCatching { api.fetchPantryMeals(body) }
            .onSuccess  { r -> _recipeState.update { it.copy(pantryRecipes = r, pantryLoading = false) } }
            .onFailure  {     _recipeState.update { it.copy(pantryError = true, pantryLoading = false) } }
    }

    fun loadAdventurous() = viewModelScope.launch {
        _recipeState.update { it.copy(adventurousLoading = true, adventurousError = false) }
        val items = repo.items.first()
        val body  = mapOf("pantryItems" to items.map { mapOf("name" to it.name) })
        runCatching { api.fetchAdventurous(body) }
            .onSuccess  { r -> _recipeState.update { it.copy(adventurousRecipes = r, adventurousLoading = false) } }
            .onFailure  {     _recipeState.update { it.copy(adventurousError = true, adventurousLoading = false) } }
    }
}
