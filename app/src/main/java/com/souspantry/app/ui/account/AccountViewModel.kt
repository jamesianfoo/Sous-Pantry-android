package com.souspantry.app.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.repository.MyRecipeRepository
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.data.repository.SavedRecipeRepository
import com.souspantry.app.data.repository.ShoppingRepository
import com.souspantry.app.data.repository.WeekPlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountState(
    val userName            : String      = "",
    val userEmail           : String      = "",
    val gender              : String      = "",
    val cookingFor          : Int         = 2,
    val measurementSystem   : String      = "metric",
    val temperatureUnit     : String      = "celsius",
    val dietaryTypes        : Set<String> = emptySet(),
    val foodRestrictions    : Set<String> = emptySet(),
    val avoidIngredients    : Set<String> = emptySet(),
    val alertExpiring       : Boolean     = true,
    val alertReceipt        : Boolean     = true,
    val alertPantryStale    : Boolean     = true,
    val recommendedSubs     : Boolean     = true,
    val sousAIEnabled       : Boolean     = true,
    val deviceId            : String      = "",
    val isPremium           : Boolean     = false, // TODO: wire to BillingManager (Task 8)
    val forcePremium        : Boolean     = false, // Debug-only override
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val prefs     : UserPreferencesRepository,
    private val pantry    : PantryRepository,
    private val weekPlan  : WeekPlanRepository,
    private val myRecipe  : MyRecipeRepository,
    private val saved     : SavedRecipeRepository,
    private val shopping  : ShoppingRepository,
) : ViewModel() {

    // Compose 13 flows into one state. We chain two combines because combine() has
    // a hard limit of 5 sources per call before generics blow up.
    private val identityState = combine(
        prefs.userName, prefs.userEmail, prefs.gender, prefs.cookingFor, prefs.deviceId,
    ) { name, email, gender, cookingFor, deviceId ->
        AccountState(userName = name, userEmail = email, gender = gender, cookingFor = cookingFor, deviceId = deviceId)
    }

    private val unitsState = combine(
        prefs.measurementSystem, prefs.temperatureUnit,
    ) { measurement, temperature -> measurement to temperature }

    private val dietState = combine(
        prefs.dietaryTypes, prefs.foodRestrictions, prefs.avoidIngredients,
    ) { types, restrictions, ingredients -> Triple(types, restrictions, ingredients) }

    private val notifState = combine(
        prefs.alertExpiring, prefs.alertReceipt, prefs.alertPantryStale,
    ) { expiring, receipt, stale -> Triple(expiring, receipt, stale) }

    private val customisationState = combine(
        prefs.recommendedSubs, prefs.sousAIEnabled, prefs.forcePremium,
    ) { subs, sousAi, forcePremium -> Triple(subs, sousAi, forcePremium) }

    val state: StateFlow<AccountState> = combine(
        identityState, unitsState, dietState, notifState, customisationState,
    ) { identity, units, diet, notif, cust ->
        identity.copy(
            measurementSystem = units.first,
            temperatureUnit   = units.second,
            dietaryTypes      = diet.first,
            foodRestrictions  = diet.second,
            avoidIngredients  = diet.third,
            alertExpiring     = notif.first,
            alertReceipt      = notif.second,
            alertPantryStale  = notif.third,
            recommendedSubs   = cust.first,
            sousAIEnabled     = cust.second,
            forcePremium      = cust.third,
            isPremium         = cust.third, // Real subscription wiring lands in Task 8; for now this is the only source of premium-truthiness.
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountState())

    // ── Writes ──────────────────────────────────────────────────────────────

    fun setUserName(value: String)         = viewModelScope.launch { prefs.setUserName(value) }
    fun setGender(value: String)           = viewModelScope.launch { prefs.setGender(value) }
    fun setCookingFor(count: Int)          = viewModelScope.launch { prefs.setCookingFor(count) }
    fun setMeasurementSystem(value: String) = viewModelScope.launch { prefs.setMeasurementSystem(value) }
    fun setTemperatureUnit(value: String)  = viewModelScope.launch { prefs.setTemperatureUnit(value) }
    fun setDietaryTypes(values: Set<String>)     = viewModelScope.launch { prefs.setDietaryTypes(values) }
    fun setFoodRestrictions(values: Set<String>) = viewModelScope.launch { prefs.setFoodRestrictions(values) }
    fun setAvoidIngredients(values: Set<String>) = viewModelScope.launch { prefs.setAvoidIngredients(values) }
    fun setAlertExpiring(on: Boolean)      = viewModelScope.launch { prefs.setAlertExpiring(on) }
    fun setAlertReceipt(on: Boolean)       = viewModelScope.launch { prefs.setAlertReceipt(on) }
    fun setAlertPantryStale(on: Boolean)   = viewModelScope.launch { prefs.setAlertPantryStale(on) }
    fun setRecommendedSubs(on: Boolean)    = viewModelScope.launch { prefs.setRecommendedSubs(on) }
    fun setSousAIEnabled(on: Boolean)      = viewModelScope.launch { prefs.setSousAIEnabled(on) }
    fun setForcePremium(on: Boolean)       = viewModelScope.launch { prefs.setForcePremium(on) }

    // ── Account actions ─────────────────────────────────────────────────────

    /**
     * Logs out the local session. Clears the Supabase session token and the
     * user's profile-identity prefs (name/email), but KEEPS local data (pantry,
     * plans, recipes, shopping) — logging out shouldn't destroy your data.
     * When Supabase auth lands (Task 7) this also calls the remote sign-out.
     *
     * @param onComplete invoked on the main scope once the local sign-out finishes
     *                   so the UI can navigate to the welcome/auth flow.
     */
    fun logOut(onComplete: () -> Unit = {}) = viewModelScope.launch {
        prefs.clearSupabaseSession()
        prefs.setSignedIn(false)
        prefs.setUserName("")
        prefs.setUserEmail("")
        onComplete()
    }

    /**
     * Hard factory reset. Erases EVERY local store — pantry, weekly plans,
     * My Recipes, Saved Recipes, shopping list — and clears all DataStore prefs
     * (profile, diet, notifications, onboarding flag; deviceId is preserved).
     * Mirrors iOS Delete Account, minus the Supabase server-side erase which we
     * add when auth lands.
     *
     * @param onComplete invoked once the wipe finishes so the UI can return to
     *                   the onboarding flow (the app is now in a fresh state).
     */
    fun deleteAccount(onComplete: () -> Unit = {}) = viewModelScope.launch {
        // Wipe every Room table
        pantry.deleteAll()
        weekPlan.deleteAll()
        myRecipe.deleteAll()
        saved.deleteAll()
        shopping.deleteAll()
        // Wipe DataStore (keeps deviceId; onboarding flag is cleared → onboarding shows)
        prefs.clearAll()
        onComplete()
    }
}
