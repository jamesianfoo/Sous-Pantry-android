package com.souspantry.app.ui.setup

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

data class SetupWizardState(
    val step                 : Int         = 0,
    val selectedGoals        : Set<String> = emptySet(),
    val selectedDietary      : String      = "",
    val selectedRestrictions : Set<String> = emptySet(),
    val finishing            : Boolean     = false,
) {
    val canContinue: Boolean
        get() = when (step) {
            0    -> selectedGoals.isNotEmpty()
            1    -> selectedDietary.isNotBlank()
            2    -> selectedRestrictions.isNotEmpty()
            else -> true
        }
}

@HiltViewModel
class SetupWizardViewModel @Inject constructor(
    private val prefs  : UserPreferencesRepository,
    private val pantry : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupWizardState())
    val state = _state.asStateFlow()

    // ── Navigation ───────────────────────────────────────────────────────────

    fun next()     = _state.update { if (it.step < 2) it.copy(step = it.step + 1) else it }
    fun back()     = _state.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    // ── Selection ────────────────────────────────────────────────────────────

    fun toggleGoal(id: String) = _state.update { s ->
        s.copy(selectedGoals = if (s.selectedGoals.contains(id)) s.selectedGoals - id else s.selectedGoals + id)
    }

    fun selectDietary(id: String) = _state.update { it.copy(selectedDietary = id) }

    fun toggleRestriction(id: String) = _state.update { s ->
        val next = when {
            id == "none"                       -> setOf("none")           // "None" clears the rest
            s.selectedRestrictions.contains(id) -> s.selectedRestrictions - id - "none"
            else                                -> s.selectedRestrictions - "none" + id
        }
        s.copy(selectedRestrictions = next)
    }

    // ── Finish ───────────────────────────────────────────────────────────────

    /**
     * Persists the wizard selections, pre-fills the pantry with staples if the
     * user chose that goal, and marks onboarding done.
     *
     * TODO(paywall): iOS shows the paywall here before completing — wire that in
     *   once the Paywall screen (#5) exists. For now we proceed straight to Home.
     */
    fun finish(onDone: () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(finishing = true) }
        val s = _state.value

        // Dietary (single) + restrictions (multi) → DataStore. "none" is dropped.
        prefs.setDietaryTypes(if (s.selectedDietary.isBlank() || s.selectedDietary == "none") emptySet() else setOf(s.selectedDietary))
        prefs.setFoodRestrictions(s.selectedRestrictions.filterNot { it == "none" }.toSet())

        // Pre-fill staples
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
