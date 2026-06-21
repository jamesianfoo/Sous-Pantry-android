package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import com.souspantry.app.data.models.SuggestedMeal
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SavedRecipesState(
    val recipes : List<SavedRecipe> = emptyList(),
)

/**
 * Plain ViewModel (no Hilt) — hoisted at PlanCookScreen so the Discover tab's
 * bookmark button and the Saved Recipes tab share one in-memory store.
 */
class SavedRecipesViewModel : ViewModel() {

    private val _state = MutableStateFlow(SavedRecipesState())
    val state = _state.asStateFlow()

    fun isSaved(title: String): Boolean =
        _state.value.recipes.any { it.title.equals(title.trim(), ignoreCase = true) }

    /** Toggle a Sous AI suggestion in/out of the saved collection. */
    fun toggle(meal: SuggestedMeal) {
        val exists = isSaved(meal.title)
        _state.update { s ->
            if (exists) s.copy(recipes = s.recipes.filterNot { it.title.equals(meal.title.trim(), ignoreCase = true) })
            else        s.copy(recipes = listOf(savedRecipeFrom(meal)) + s.recipes)
        }
    }

    fun remove(id: String) =
        _state.update { it.copy(recipes = it.recipes.filterNot { r -> r.id == id }) }
}
