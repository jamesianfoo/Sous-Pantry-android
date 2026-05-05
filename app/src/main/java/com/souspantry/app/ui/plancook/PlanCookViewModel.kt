package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanCookState(
    val meals  : List<SuggestedMeal> = emptyList(),
    val loading: Boolean             = false,
    val error  : String?             = null,
)

@HiltViewModel
class PlanCookViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PlanCookState())
    val state = _state.asStateFlow()

    fun generate() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        val pantryItems = repo.items.first()
        val body = mapOf("pantryItems" to pantryItems.map { mapOf("name" to it.name, "category" to (it.category ?: "")) })

        runCatching { api.generateMeals(body) }
            .onSuccess { meals -> _state.update { it.copy(meals = meals, loading = false) } }
            .onFailure {         _state.update { it.copy(error = "Couldn't generate meal plan. Try again.", loading = false) } }
    }
}
