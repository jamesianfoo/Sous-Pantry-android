package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PlanTab { RECIPES, MY_WEEK, SAVED }

data class PlanCookState(
    val meals             : List<SuggestedMeal> = emptyList(),
    val loading           : Boolean             = false,
    val error             : String?             = null,
    val selectedTab       : PlanTab             = PlanTab.RECIPES,
    val selectedMood      : String?             = null,        // "Quick Meal" | "Dinner in 30 Mins" | …
    val selectedCuisines  : Set<String>         = emptySet(),
    val expandedMealId    : String?             = null,
)

@HiltViewModel
class PlanCookViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanCookState())
    val state = _state.asStateFlow()

    private var generationJob: Job? = null

    // ── Tab + filter writes ─────────────────────────────────────────────────

    fun selectTab(tab: PlanTab) = _state.update { it.copy(selectedTab = tab) }

    fun setMood(mood: String?) = _state.update { it.copy(selectedMood = mood) }

    fun toggleCuisine(cuisine: String) = _state.update { s ->
        s.copy(
            selectedCuisines = if (s.selectedCuisines.contains(cuisine))
                s.selectedCuisines - cuisine
            else s.selectedCuisines + cuisine
        )
    }

    fun clearCuisines() = _state.update { it.copy(selectedCuisines = emptySet()) }

    fun toggleExpanded(mealTitle: String) = _state.update { s ->
        s.copy(expandedMealId = if (s.expandedMealId == mealTitle) null else mealTitle)
    }

    // ── Generation ──────────────────────────────────────────────────────────

    fun generate() {
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val s         = _state.value
            val pantryItems = repo.items.first()
            val body: MutableMap<String, Any> = mutableMapOf(
                "pantryItems" to pantryItems.map { mapOf("name" to it.name, "category" to (it.category ?: "")) },
            )
            s.selectedMood?.let     { body["mood"]     = it }
            if (s.selectedCuisines.isNotEmpty()) body["cuisines"] = s.selectedCuisines.toList()

            runCatching { api.generateMeals(body) }
                .onSuccess { meals -> _state.update { it.copy(meals = meals, loading = false) } }
                .onFailure {         _state.update { it.copy(error = "Couldn't generate meal plan. Try again.", loading = false) } }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        _state.update { it.copy(loading = false) }
    }

    fun reset() = _state.update {
        it.copy(meals = emptyList(), error = null, selectedMood = null, selectedCuisines = emptySet(), expandedMealId = null)
    }
}
