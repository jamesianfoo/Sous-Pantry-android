package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.SavedRecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedRecipesState(
    val recipes : List<SavedRecipe> = emptyList(),
)

@HiltViewModel
class SavedRecipesViewModel @Inject constructor(
    private val repo: SavedRecipeRepository,
) : ViewModel() {

    val state: StateFlow<SavedRecipesState> = repo.recipes
        .map { SavedRecipesState(recipes = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SavedRecipesState())

    /** Synchronous check against the current snapshot — used to colour the bookmark. */
    fun isSaved(title: String): Boolean =
        state.value.recipes.any { it.title.equals(title.trim(), ignoreCase = true) }

    /** Toggle a Sous AI suggestion in/out of the saved collection. */
    fun toggle(meal: SuggestedMeal) = viewModelScope.launch {
        if (repo.isSaved(meal.title)) repo.deleteByTitle(meal.title)
        else                          repo.upsert(savedRecipeFrom(meal))
    }

    fun remove(id: String) = viewModelScope.launch { repo.delete(id) }
}
