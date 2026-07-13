package com.souspantry.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeState(
    val userName    : String          = "",
    val pantryItems : List<PantryItem> = emptyList(),

    // Cooking streak — no cooking-history store yet, so these stay at defaults.
    val mealsThisWeek    : Int      = 0,
    val totalMealsCooked : Int      = 0,
    val currentStreak    : Int      = 0,
    val weeklySavings    : Double   = 0.0,
    val cookedWeekdays   : Set<Int> = emptySet(),   // 0 = Mon … 6 = Sun
) {
    /** Items expiring within 7 days (including expired), sorted by expiry ascending. */
    val auditItems: List<PantryItem>
        get() {
            val nowMs  = System.currentTimeMillis()
            val cutoff = nowMs + 7L * 86_400_000L
            return pantryItems
                .filter { it.expiryDate != null && it.expiryDate <= cutoff }
                .sortedBy { it.expiryDate }
        }

    /** Top categories by item count, each with fraction (0-1) of the pantry. */
    val categoryBreakdown: List<Triple<String, Float, Int>>
        get() {
            if (pantryItems.isEmpty()) return emptyList()
            val byCat = pantryItems.groupBy { it.category ?: "Other" }
                .map { (cat, list) -> cat to list.size }
                .sortedByDescending { it.second }
            val total = pantryItems.size.toFloat()
            return byCat.map { (cat, count) -> Triple(cat, count / total, count) }
        }

    /** Rough estimate of cookable meals from pantry size (mirrors iOS heuristic). */
    val estimatedRecipeCount: Int
        get() = (pantryItems.size * 1.2).toInt().coerceAtLeast(if (pantryItems.isNotEmpty()) 1 else 0)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo  : PantryRepository,
    private val prefs : UserPreferencesRepository,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(repo.items, prefs.userName) { items, name ->
        HomeState(pantryItems = items, userName = name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
