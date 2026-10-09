package com.souspantry.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_CURRENCY
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.PriceRecord
import com.souspantry.app.data.repository.HaulRepository
import com.souspantry.app.data.repository.MyRecipeRepository
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.data.repository.PriceRepository
import com.souspantry.app.services.Categories
import com.souspantry.app.services.NavSignals
import com.souspantry.app.services.PriceInsights
import com.souspantry.app.services.StreakService
import com.souspantry.app.services.StreakState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

data class HomeState(
    val userName         : String                       = "",
    val pantryItems      : List<PantryItem>             = emptyList(),
    val dietaryTypes     : Set<String>                  = emptySet(),
    val foodRestrictions : Set<String>                  = emptySet(),
    val avoidIngredients : Set<String>                  = emptySet(),
    /** Most recent eReceipt sync across all stores, epoch millis; null → "No scan recorded yet." */
    val lastSyncMs       : Long?                        = null,
    /** When the Pantry "Expiring" filter was last opened; null → "Never checked". */
    val lastAuditMs      : Long?                        = null,
    val streak           : StreakState                  = StreakState(),
    val customRecipeCount: Int                          = 0,
    val priceRecords     : List<PriceRecord>            = emptyList(),
    val hauls            : List<PriceInsights.HaulEntry> = emptyList(),
    val spendThreshold   : Int                          = PriceInsights.DEFAULT_SPEND_THRESHOLD.toInt(),
    /** Wizard currency code ("AUD"); null falls back to the device locale. */
    val currencyCode     : String?                      = null,
) {
    /** Items expiring within 7 days (including expired), sorted by expiry ascending. */
    val auditItems: List<PantryItem>
        get() {
            val cutoff = System.currentTimeMillis() + 7L * 86_400_000L
            return pantryItems
                .filter { it.expiryDate != null && it.expiryDate <= cutoff }
                .sortedBy { it.expiryDate }
        }

    /** Top categories by item count (iOS displayCategory grouping), each with fraction (0-1) of the pantry. */
    val categoryBreakdown: List<Triple<String, Float, Int>>
        get() {
            if (pantryItems.isEmpty()) return emptyList()
            val total = pantryItems.size.toFloat()
            return pantryItems
                .groupBy { Categories.normalizeCategory(it.category) ?: "Other" }
                .map { (cat, list) -> Triple(cat, list.size / total, list.size) }
                .sortedByDescending { it.third }
        }

    /** iOS: min(max(max(1, count / 8), 3), 25) — 48 items → 6. Zero when the pantry is empty. */
    val estimatedRecipeCount: Int
        get() = if (pantryItems.isEmpty()) 0 else minOf(maxOf(maxOf(1, pantryItems.size / 8), 3), 25)

    val sousMessage: String get() = "Suggest $estimatedRecipeCount meals I can make with my current pantry."
}

/** Money in the wizard currency (device locale when unset / unknown), fixed [decimals]. */
fun formatMoney(amount: Double, currencyCode: String?, decimals: Int): String {
    val f = NumberFormat.getCurrencyInstance(Locale.getDefault())
    if (!currencyCode.isNullOrBlank()) runCatching { f.currency = Currency.getInstance(currencyCode) }
    f.minimumFractionDigits = decimals
    f.maximumFractionDigits = decimals
    return f.format(amount)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo     : PantryRepository,
    private val prefs    : UserPreferencesRepository,
    private val streak   : StreakService,
    private val signals  : NavSignals,
    myRecipes            : MyRecipeRepository,
    prices               : PriceRepository,
    hauls                : HaulRepository,
) : ViewModel() {

    private data class Profile(val name: String, val diet: Set<String>, val restrictions: Set<String>, val avoid: Set<String>)
    private data class Activity(val lastSync: Long?, val lastAudit: Long?, val streak: StreakState, val recipeCount: Int)
    private data class Spend(val records: List<PriceRecord>, val hauls: List<PriceInsights.HaulEntry>, val threshold: Int, val currency: String?)

    private val profile = combine(prefs.userName, prefs.dietaryTypes, prefs.foodRestrictions, prefs.avoidIngredients) { name, diet, restr, avoid ->
        Profile(name, diet, restr, avoid)
    }

    private val activity = combine(prefs.lastSyncDates, prefs.lastAuditDate, streak.state, myRecipes.recipes) { sync, audit, st, recipes ->
        Activity(sync.values.maxOrNull(), audit, st, recipes.size)
    }

    private val spend = combine(
        prices.records, hauls.entries, prefs.spendThreshold,
        prefs.preferences.map { it[KEY_FUNNEL_CURRENCY] },
    ) { records, hauls, threshold, currency -> Spend(records, hauls, threshold, currency) }

    val state: StateFlow<HomeState> = combine(repo.items, profile, activity, spend) { items, p, a, s ->
        HomeState(
            userName          = p.name,
            pantryItems       = items,
            dietaryTypes      = p.diet,
            foodRestrictions  = p.restrictions,
            avoidIngredients  = p.avoid,
            lastSyncMs        = a.lastSync,
            lastAuditMs       = a.lastAudit,
            streak            = a.streak,
            customRecipeCount = a.recipeCount,
            priceRecords      = s.records,
            hauls             = s.hauls,
            spendThreshold    = s.threshold,
            currencyCode      = s.currency,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    /** Home has shown the 🎉 Congrats banner for [StreakState.bannerReward]. */
    fun clearBanner() = viewModelScope.launch { streak.clearBanner() }

    /** "View Expiring Items": Pantry opens on the Expiring filter and the audit is stamped (iOS showExpiringFilter). */
    fun viewExpiringItems() {
        signals.showExpiringFilter.value = true
        viewModelScope.launch { prefs.setLastAuditDate() }
    }

    /** "See what to cook →": Plan & Cook auto-sends the meal-suggestion prompt (iOS pendingSousMessage). */
    fun seeWhatToCook() {
        signals.pendingSousMessage.value = state.value.sousMessage
    }
}
