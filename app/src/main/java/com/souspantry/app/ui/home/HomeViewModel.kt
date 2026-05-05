package com.souspantry.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.*
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeState(
    val suggestedRecipes   : List<SuggestedMeal>      = emptyList(),
    val trendingRecipes    : List<TrendingRecipe>      = emptyList(),
    val pantryRecipes      : List<SuggestedMeal>       = emptyList(),
    val adventurousRecipes : List<AdventurousRecipe>   = emptyList(),

    val suggestedLoading   : Boolean = false,
    val trendingLoading    : Boolean = false,
    val pantryLoading      : Boolean = false,
    val adventurousLoading : Boolean = false,

    val suggestedError     : Boolean = false,
    val trendingError      : Boolean = false,
    val pantryError        : Boolean = false,
    val adventurousError   : Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        loadAll()
    }

    private fun loadAll() {
        loadSuggested()
        loadTrending()
        loadPantryMeals()
        loadAdventurous()
    }

    fun loadSuggested() = viewModelScope.launch {
        _state.update { it.copy(suggestedLoading = true, suggestedError = false) }
        runCatching { api.fetchSuggestedMeals(emptyMap()) }
            .onSuccess  { meals -> _state.update { it.copy(suggestedRecipes = meals,   suggestedLoading = false) } }
            .onFailure  {        _state.update { it.copy(suggestedError = true,        suggestedLoading = false) } }
    }

    fun loadTrending() = viewModelScope.launch {
        _state.update { it.copy(trendingLoading = true, trendingError = false) }
        runCatching { api.fetchTrending(emptyMap()) }
            .onSuccess  { r -> _state.update { it.copy(trendingRecipes = r, trendingLoading = false) } }
            .onFailure  {     _state.update { it.copy(trendingError = true, trendingLoading = false) } }
    }

    fun loadPantryMeals() = viewModelScope.launch {
        _state.update { it.copy(pantryLoading = true, pantryError = false) }
        val items = repo.items.first()
        val body  = mapOf("pantryItems" to items.map { mapOf("name" to it.name) })
        runCatching { api.fetchPantryMeals(body) }
            .onSuccess  { r -> _state.update { it.copy(pantryRecipes = r, pantryLoading = false) } }
            .onFailure  {     _state.update { it.copy(pantryError = true, pantryLoading = false) } }
    }

    fun loadAdventurous() = viewModelScope.launch {
        _state.update { it.copy(adventurousLoading = true, adventurousError = false) }
        val items = repo.items.first()
        val body  = mapOf("pantryItems" to items.map { mapOf("name" to it.name) })
        runCatching { api.fetchAdventurous(body) }
            .onSuccess  { r -> _state.update { it.copy(adventurousRecipes = r, adventurousLoading = false) } }
            .onFailure  {     _state.update { it.copy(adventurousError = true, adventurousLoading = false) } }
    }
}
