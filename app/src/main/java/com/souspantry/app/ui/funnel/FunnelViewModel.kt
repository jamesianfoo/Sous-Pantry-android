package com.souspantry.app.ui.funnel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_AGE
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_ALLERGIES
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_APPLIANCES
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_BARRIER
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_BUDGET
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_COOK_TIME
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_COUNTRY
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_CURRENCY
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_DAYS
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_GOAL
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_MOODS
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_PROTEINS
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_REFERRAL
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_REGION
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_SAVE_MORE
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_FUNNEL_SHOPS
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_TRIAL_REMINDER
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.repository.ShoppingRepository
import com.souspantry.app.services.RegionHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Post-steps phases of the funnel (finale sequence). */
enum class FunnelPhase { STEPS, BUILDING, REMINDER, PAYWALL }

data class FunnelState(
    val stepIndex     : Int                       = 0,
    val phase         : FunnelPhase               = FunnelPhase.STEPS,
    val singles       : Map<String, String>       = emptyMap(),   // stepId -> optionId
    val multis        : Map<String, Set<String>>  = emptyMap(),   // stepId -> optionIds
    val name          : String                    = "",
    val otherCountry  : String                    = "",
    val household     : Int                       = 2,
    val budget        : Int                       = 80,
    val trialReminder : String                    = "1_day",
) {
    val region: String get() = singles["region"] ?: ""

    /** Steps visible for the current region (conditional other_country / shop). */
    val visibleSteps: List<FunnelStep>
        get() = FUNNEL_STEPS.mapNotNull { step ->
            when (step.id) {
                "other_country" -> if (region == "OTHER") step else null
                "shop"          -> if (region == "OTHER") null
                                   else step.copy(options = (REGION_SHOPS[region] ?: REGION_SHOPS.getValue("AU")) + FunnelOption("others", "Others"))
                "value_savings" -> step.copy(info = FOOD_WASTE_FACTS[region] ?: FOOD_WASTE_FACT_DEFAULT)
                else            -> step
            }
        }

    val currentStep: FunnelStep get() = visibleSteps[stepIndex.coerceIn(0, visibleSteps.lastIndex)]

    val currencyCode: String get() = REGION_CURRENCY[region] ?: "USD"
    val currencySymbol: String get() = CURRENCY_SYMBOL[currencyCode] ?: "$"

    val canContinue: Boolean get() = when (currentStep.kind) {
        StepKind.SINGLE        -> singles.containsKey(currentStep.id)
        StepKind.MULTI         -> (multis[currentStep.id]?.size ?: 0) >= currentStep.min
        StepKind.TEXT          -> name.isNotBlank()
        StepKind.OTHER_COUNTRY -> otherCountry.isNotBlank()
        else                   -> true
    }
}

@HiltViewModel
class FunnelViewModel @Inject constructor(
    private val prefs    : UserPreferencesRepository,
    private val shopping : ShoppingRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(FunnelState())
    val state = _state.asStateFlow()

    // ── Answers (persist immediately) ────────────────────────────────────────

    fun selectSingle(stepId: String, optionId: String) {
        _state.update { s ->
            var next = s.copy(singles = s.singles + (stepId to optionId))
            // Changing region: clear stale region-specific shop picks.
            if (stepId == "region") next = next.copy(multis = next.multis - "shop")
            next
        }
        viewModelScope.launch {
            when (stepId) {
                "goal"      -> prefs.setFunnelString(KEY_FUNNEL_GOAL, optionId)
                "barrier"   -> prefs.setFunnelString(KEY_FUNNEL_BARRIER, optionId)
                "save_more" -> prefs.setFunnelString(KEY_FUNNEL_SAVE_MORE, optionId)
                "age"       -> prefs.setFunnelString(KEY_FUNNEL_AGE, optionId)
                "cook_time" -> prefs.setFunnelString(KEY_FUNNEL_COOK_TIME, optionId)
                "referral"  -> prefs.setFunnelString(KEY_FUNNEL_REFERRAL, optionId)
                "region"    -> {
                    prefs.setFunnelString(KEY_FUNNEL_REGION, optionId)
                    prefs.setFunnelString(KEY_FUNNEL_CURRENCY, REGION_CURRENCY[optionId] ?: "USD")
                    prefs.setFunnelSet(KEY_FUNNEL_SHOPS, emptySet())
                    RegionHolder.code = optionId
                }
            }
        }
    }

    fun toggleMulti(stepId: String, optionId: String, max: Int) {
        _state.update { s ->
            val cur = s.multis[stepId] ?: emptySet()
            val next = when {
                cur.contains(optionId) -> cur - optionId
                cur.size >= max        -> cur
                else                   -> cur + optionId
            }
            s.copy(multis = s.multis + (stepId to next))
        }
        val set = _state.value.multis[stepId] ?: emptySet()
        viewModelScope.launch {
            when (stepId) {
                "dietary"    -> prefs.setDietaryTypes(set.filterNot { it == "none" }.toSet())
                "allergies"  -> {
                    prefs.setFunnelSet(KEY_FUNNEL_ALLERGIES, set)
                    prefs.setFoodRestrictions(set.filterNot { it == "none" }.toSet())
                }
                "proteins"   -> prefs.setFunnelSet(KEY_FUNNEL_PROTEINS, set)
                "moods"      -> prefs.setFunnelSet(KEY_FUNNEL_MOODS, set)
                "days"       -> prefs.setFunnelSet(KEY_FUNNEL_DAYS, set)
                "shop"       -> prefs.setFunnelSet(KEY_FUNNEL_SHOPS, set)
                "appliances" -> prefs.setFunnelSet(KEY_FUNNEL_APPLIANCES, set)
            }
        }
    }

    fun setName(v: String) {
        _state.update { it.copy(name = v) }
        viewModelScope.launch { prefs.setUserName(v.trim()) }
    }

    fun setOtherCountry(v: String) {
        _state.update { it.copy(otherCountry = v) }
        viewModelScope.launch { prefs.setFunnelString(KEY_FUNNEL_COUNTRY, v.trim()) }
    }

    fun setHousehold(v: Int) {
        _state.update { it.copy(household = v.coerceIn(1, 12)) }
        viewModelScope.launch { prefs.setCookingFor(v) }
    }

    fun setBudget(v: Int) {
        _state.update { it.copy(budget = v.coerceIn(20, 310)) }
        viewModelScope.launch { prefs.setFunnelInt(KEY_FUNNEL_BUDGET, v) }
    }

    fun setTrialReminder(v: String) {
        _state.update { it.copy(trialReminder = v) }
        viewModelScope.launch { prefs.setFunnelString(KEY_TRIAL_REMINDER, v) }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun next() = _state.update { s ->
        if (s.stepIndex < s.visibleSteps.lastIndex) s.copy(stepIndex = s.stepIndex + 1)
        else s.copy(phase = FunnelPhase.BUILDING)   // finale CTA "Generate Plan"
    }

    fun back() = _state.update { s ->
        if (s.stepIndex > 0) s.copy(stepIndex = s.stepIndex - 1) else s
    }

    fun advancePhase(to: FunnelPhase) = _state.update { it.copy(phase = to) }

    // ── Completion ───────────────────────────────────────────────────────────

    /**
     * Mark the funnel complete (key sp_hasCompletedFunnel — only written here,
     * at the paywall hand-off) and seed the starter shopping list once.
     */
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        seedStarterList()
        prefs.setFunnelDone()
        onDone()
    }

    private suspend fun seedStarterList() {
        if (shopping.items.first().isNotEmpty()) return   // seed ONCE, only if empty

        val s           = _state.value
        val dietary     = s.multis["dietary"] ?: emptySet()
        val allergies   = s.multis["allergies"] ?: emptySet()
        val proteins    = s.multis["proteins"] ?: emptySet()
        val vegan       = "vegan" in dietary
        val vegetarian  = "vegetarian" in dietary || vegan
        val pescatarian = "pescatarian" in dietary

        data class Seed(val name: String, val category: String?, val priority: String = "essential")
        val seeds = buildList {
            add(Seed("Salt", "Pantry Staples")); add(Seed("Black Pepper", "Pantry Staples")); add(Seed("Olive Oil", "Pantry Staples"))
            add(Seed("Garlic", "Vegetables")); add(Seed("Onions", "Vegetables"))
            add(Seed("Rice", null))
            add(if ("gluten_free" in allergies) Seed("Gluten-free Pasta", null, "optional") else Seed("Pasta", null))
            if (!vegan && "egg_free" !in allergies) add(Seed("Eggs", null))
            if (!vegan && "dairy_free" !in allergies) { add(Seed("Milk", null)); add(Seed("Butter", null)) }
            else add(Seed("Plant-based Milk", null, "optional"))
            when {
                vegetarian  -> { add(Seed("Tofu", null, if (vegan) "essential" else "optional")); add(Seed("Chickpeas", null, "optional")) }
                pescatarian -> add(Seed("Fish Fillets", null))
                else        -> {
                    val picks = proteins.ifEmpty { setOf("chicken") }
                    if ("chicken" in picks) add(Seed("Chicken", null))
                    if ("beef" in picks)    add(Seed("Beef Mince", null))
                    if ("pork" in picks)    add(Seed("Pork", null))
                    if ("fish" in picks)    add(Seed("Fish Fillets", null))
                }
            }
            add(Seed("Seasonal Vegetables", null, "optional"))
        }
        shopping.upsertAll(seeds.map {
            ShoppingItem(name = it.name, category = it.category, quantity = null, priority = it.priority, reason = "manual")
        })
    }
}
