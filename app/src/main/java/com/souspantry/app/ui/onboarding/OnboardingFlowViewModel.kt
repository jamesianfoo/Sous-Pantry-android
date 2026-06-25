package com.souspantry.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingFlowState(
    val selectedGoals        : Set<String> = emptySet(),
    val selectedDietary      : String      = "",
    val selectedRestrictions : Set<String> = emptySet(),
)

/**
 * State + persistence for the 10-page onboarding (feature intros + the
 * inline Goals/Dietary/Restrictions wizard). Mirrors iOS OnboardingViewModel.
 * Page index lives in the Compose PagerState; this VM only holds the wizard
 * selections and the completion logic.
 */
@HiltViewModel
class OnboardingFlowViewModel @Inject constructor(
    private val prefs  : UserPreferencesRepository,
    private val pantry : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingFlowState())
    val state = _state.asStateFlow()

    fun toggleGoal(id: String) = _state.update { s ->
        s.copy(selectedGoals = if (s.selectedGoals.contains(id)) s.selectedGoals - id else s.selectedGoals + id)
    }

    fun selectDietary(id: String) = _state.update { it.copy(selectedDietary = id) }

    fun toggleRestriction(id: String) = _state.update { s ->
        val next = when {
            id == "none"                        -> setOf("none")            // exclusive
            s.selectedRestrictions.contains(id) -> s.selectedRestrictions - id - "none"
            else                                -> s.selectedRestrictions - "none" + id
        }
        s.copy(selectedRestrictions = next)
    }

    /**
     * Persists wizard selections, pre-fills staples if chosen, and marks
     * onboarding done. The founder note + paywall follow (handled by NavHost).
     */
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        val s = _state.value
        prefs.setDietaryTypes(if (s.selectedDietary.isBlank() || s.selectedDietary == "none") emptySet() else setOf(s.selectedDietary))
        prefs.setFoodRestrictions(s.selectedRestrictions.filterNot { it == "none" }.toSet())
        if (s.selectedGoals.contains("staples")) {
            pantry.addAll(STAPLES.map { (name, category) -> PantryItem(name = name, category = category) })
        }
        prefs.setOnboardingDone()
        onDone()
    }
}

/** Everyday kitchen essentials, mapped to the app's pantry categories. */
private val STAPLES = listOf(
    "Olive oil"             to "Condiments & Sauces",
    "Salt"                  to "Condiments & Sauces",
    "Black pepper"          to "Condiments & Sauces",
    "Plain flour"           to "Pantry & Dry Goods",
    "White sugar"           to "Pantry & Dry Goods",
    "Brown sugar"           to "Pantry & Dry Goods",
    "Unsalted butter"       to "Dairy & Eggs",
    "Eggs"                  to "Dairy & Eggs",
    "Full cream milk"       to "Dairy & Eggs",
    "Garlic"                to "Vegetables",
    "Brown onion"           to "Vegetables",
    "White rice"            to "Pantry & Dry Goods",
    "Dried pasta"           to "Pantry & Dry Goods",
    "Canned diced tomatoes" to "Pantry & Dry Goods",
    "Tomato paste"          to "Condiments & Sauces",
    "Chicken stock"         to "Pantry & Dry Goods",
    "Soy sauce"             to "Condiments & Sauces",
    "White vinegar"         to "Condiments & Sauces",
    "Honey"                 to "Condiments & Sauces",
    "Dijon mustard"         to "Condiments & Sauces",
    "Smoked paprika"        to "Pantry & Dry Goods",
    "Ground cumin"          to "Pantry & Dry Goods",
    "Dried oregano"         to "Pantry & Dry Goods",
    "Chilli flakes"         to "Pantry & Dry Goods",
)
