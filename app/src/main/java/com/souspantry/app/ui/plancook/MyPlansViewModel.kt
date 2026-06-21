package com.souspantry.app.ui.plancook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.WeekPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val repo: WeekPlanRepository,
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
}
