package com.souspantry.app.ui.plancook

import com.souspantry.app.data.models.SuggestedMeal
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

data class MyPlansState(
    val entries    : List<WeekMealEntry> = emptyList(),
    val weekOffset : Int                 = 0,           // 0 = current week, +1 = next, -1 = prev
)

class MyPlansViewModel : ViewModel() {

    private val _state = MutableStateFlow(MyPlansState())
    val state = _state.asStateFlow()

    // ── Week navigation ──────────────────────────────────────────────────────

    fun nextWeek()     = _state.update { it.copy(weekOffset = it.weekOffset + 1) }
    fun prevWeek()     = _state.update { it.copy(weekOffset = it.weekOffset - 1) }

    /** Returns the Mon–Sun dates for the currently viewed week. */
    fun weekDates(): List<LocalDate> {
        val offset = _state.value.weekOffset
        val monday = LocalDate.now()
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .plusWeeks(offset.toLong())
        return (0..6).map { monday.plusDays(it.toLong()) }
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun addEntry(entry: WeekMealEntry) =
        _state.update { it.copy(entries = it.entries + entry) }

    /** Add a SuggestedMeal from the Discover tab to a specific date. */
    fun addFromMeal(meal: SuggestedMeal, date: LocalDate) = addEntry(weekMealEntryFrom(meal, date))

    fun addCustomMeal(name: String, date: LocalDate) = addEntry(weekMealEntryCustom(name, date))

    fun removeEntry(id: String) =
        _state.update { it.copy(entries = it.entries.filterNot { e -> e.id == id }) }

    fun moveEntry(id: String, newDate: LocalDate) =
        _state.update { s ->
            s.copy(entries = s.entries.map { e -> if (e.id == id) e.copy(scheduledDate = newDate) else e })
        }
}
