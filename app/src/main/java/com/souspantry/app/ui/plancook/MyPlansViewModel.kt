package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.data.repository.ShoppingRepository
import com.souspantry.app.data.repository.WeekPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

data class MyPlansState(
    val entries    : List<WeekMealEntry> = emptyList(),
    val weekOffset : Int                 = 0,           // 0 = current week, +1 = next, -1 = prev
)

@HiltViewModel
class MyPlansViewModel @Inject constructor(
    private val repo     : WeekPlanRepository,
    private val pantry   : PantryRepository,
    private val shopping : ShoppingRepository,
) : ViewModel() {

    private val _weekOffset = MutableStateFlow(0)

    val state: StateFlow<MyPlansState> = combine(repo.entries, _weekOffset) { entries, offset ->
        MyPlansState(entries = entries, weekOffset = offset)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyPlansState())

    // ── Week navigation ──────────────────────────────────────────────────────

    fun nextWeek() { _weekOffset.value += 1 }
    fun prevWeek() { _weekOffset.value -= 1 }

    /** Returns the Mon–Sun dates for the currently viewed week. */
    fun weekDates(): List<LocalDate> {
        val monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(_weekOffset.value.toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun addEntry(entry: WeekMealEntry) = viewModelScope.launch { repo.upsert(entry) }

    fun addFromMeal(meal: SuggestedMeal, date: LocalDate) = addEntry(weekMealEntryFrom(meal, date))

    fun addCustomMeal(name: String, date: LocalDate) = addEntry(weekMealEntryCustom(name, date))

    fun removeEntry(id: String) = viewModelScope.launch { repo.delete(id) }

    fun moveEntry(id: String, newDate: LocalDate) = viewModelScope.launch {
        val entry = _stateEntries().firstOrNull { it.id == id } ?: return@launch
        repo.upsert(entry.copy(scheduledDate = newDate))
    }

    private fun _stateEntries(): List<WeekMealEntry> = state.value.entries

    // ── Cook + Shopping actions (from the meal detail sheet) ──────────────────

    /**
     * Marks a planned meal cooked: deducts the checked (in-pantry) ingredients
     * from the pantry by name match, then removes the meal from the week.
     * Mirrors iOS markCooked (minus the cook-history record, which the streak
     * store will consume once it exists).
     */
    fun markCooked(entry: WeekMealEntry, checkedIngredients: List<String>) = viewModelScope.launch {
        val pantryItems = pantry.items.first()
        checkedIngredients.forEach { ing ->
            val match = pantryItems.firstOrNull {
                it.name.contains(ing, ignoreCase = true) || ing.contains(it.name, ignoreCase = true)
            }
            if (match != null) pantry.delete(match)
        }
        repo.delete(entry.id)
    }

    /** Adds the missing recipe ingredients to the shopping list (Essential, AI source). */
    fun addMissingToShopping(missing: List<String>) = viewModelScope.launch {
        missing.forEach { name ->
            if (!shopping.exists(name)) {
                shopping.upsert(
                    ShoppingItem(
                        name     = name,
                        category = null,
                        quantity = null,
                        priority = "essential",
                        reason   = "From a planned meal",
                    )
                )
            }
        }
    }
}
