package com.souspantry.app.services

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_BANNER
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_CURRENT
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_LAST_COOKED
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_MEALS_WEEK
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_TOTAL
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.KEY_STREAK_WEEK_START
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.streakAwardedKey
import com.souspantry.app.data.repository.MyRecipeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import javax.inject.Inject
import javax.inject.Singleton

// ── Awards (iOS StreakReward, declaration order == Awards grid order) ────────

/** Icon family of an award; the UI maps these to the nearest Material icon (iOS uses SF Symbols). */
enum class AwardIcon { BASKET, LEAF, BOLT, FLAME, MEDAL, TROPHY, STAR, LAUREL, CROWN }

/** Card tint family; Home paints the badge gold when the tier is NAVY, green otherwise. */
enum class AwardTier { AMBER, TEAL, NAVY, GRAY, GOLD }

/**
 * Snapshot of counters evaluated against each award's unlock rule.
 * Scan / rescue / speed counters are 0 on iOS too ("not wired yet").
 */
data class AwardCounters(
    val totalMealsCooked    : Int,
    val currentStreak       : Int,
    val customRecipeCount   : Int,
    val lifetimeItemsScanned: Int = 0,
    val expiringItemsCooked : Int = 0,
    val mealsUnderFifteenMin: Int = 0,
    val mealsUnderFiveMin   : Int = 0,
)

/** The 24 awards. [id] is the iOS rawValue and the DataStore flag suffix (`streak_awarded_{id}`). */
enum class StreakReward(
    val id         : String,
    val title      : String,
    val icon       : AwardIcon,
    val tier       : AwardTier,
    val description: String,
    val isUnlocked : (AwardCounters) -> Boolean,
) {
    // ── Pantry scan ladder ──
    KITCHEN_EXPLORER("kitchenExplorer",     "Kitchen Explorer",       AwardIcon.BASKET, AwardTier.TEAL,  "Scanned 20 items into your pantry.",   { it.lifetimeItemsScanned >= 20 }),
    PANTRY_EXPERT("pantryExpert",        "Pantry Expert",          AwardIcon.BASKET, AwardTier.TEAL,  "Scanned 150 items into your pantry.",  { it.lifetimeItemsScanned >= 150 }),
    ULTIMATE_PANTRY_EXPERT("ultimatePantryExpert","Ultimate Pantry Expert", AwardIcon.BASKET, AwardTier.TEAL,  "Scanned 500 items into your pantry.",  { it.lifetimeItemsScanned >= 500 }),
    PANTRY_SPECIALIST("pantrySpecialist",    "Pantry Specialist",      AwardIcon.BASKET, AwardTier.TEAL,  "Scanned 1000 items into your pantry.", { it.lifetimeItemsScanned >= 1000 }),
    // ── Expiring-ingredient rescue ──
    IMPACT_MAKER("impactMaker",         "Impact Maker",           AwardIcon.LEAF,   AwardTier.TEAL,  "Cooked 50 expiring ingredients — zero waste hero.",       { it.expiringItemsCooked >= 50 }),
    IMPACT_CATALYST("impactCatalyst",      "Impact Catalyst",        AwardIcon.LEAF,   AwardTier.TEAL,  "Cooked 100 expiring ingredients — food-waste champion.",  { it.expiringItemsCooked >= 100 }),
    // ── Speed cook ──
    SPEED_COOK("speedCook",           "Speed Cook",             AwardIcon.BOLT,   AwardTier.NAVY,  "Cooked 5 meals in under 15 minutes.",  { it.mealsUnderFifteenMin >= 5 }),
    SUPER_SPEED_COOK("superSpeedCook",      "Super Speed Cook",       AwardIcon.BOLT,   AwardTier.NAVY,  "Cooked 20 meals in under 15 minutes.", { it.mealsUnderFifteenMin >= 20 }),
    SONIC_SPEED_COOK("sonicSpeedCook",      "Sonic Speed Cook",       AwardIcon.BOLT,   AwardTier.NAVY,  "Cooked 20 meals in under 5 minutes.",  { it.mealsUnderFiveMin >= 20 }),
    // ── Streak milestones ──
    PERFECT_WEEK("perfectWeek",         "Perfect Week",           AwardIcon.FLAME,  AwardTier.AMBER, "Cooked 7 days in a row.",              { it.currentStreak >= 7 }),
    PERFECT_STREAK("perfectStreak",       "Perfect Streak",         AwardIcon.FLAME,  AwardTier.AMBER, "Cooked 30 days in a row.",             { it.currentStreak >= 30 }),
    // ── Chef ladder (meals cooked) ──
    HOME_COOK("homeCook",            "Home Cook",              AwardIcon.MEDAL,  AwardTier.AMBER, "Cooked 5 meals.",    { it.totalMealsCooked >= 5 }),
    SUPER_HOME_COOK("superHomeCook",       "Super Home Cook",        AwardIcon.TROPHY, AwardTier.AMBER, "Cooked 20 meals.",   { it.totalMealsCooked >= 20 }),
    AMAZING_HOME_COOK("amazingHomeCook",     "Amazing Home Cook",      AwardIcon.STAR,   AwardTier.AMBER, "Cooked 40 meals.",   { it.totalMealsCooked >= 40 }),
    STELLAR_CHEF("stellarChef",         "Stellar Chef",           AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 60 meals.",   { it.totalMealsCooked >= 60 }),
    EXECUTIVE_CHEF("executiveChef",       "Executive Chef",         AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 80 meals.",   { it.totalMealsCooked >= 80 }),
    HEAD_CHEF("headChef",            "Head Chef",              AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 100 meals.",  { it.totalMealsCooked >= 100 }),
    CULINARY_MASTER("culinaryMaster",      "Culinary Master",        AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 120 meals.",  { it.totalMealsCooked >= 120 }),
    MASTER_CHEF("masterChef",          "Master Chef",            AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 150 meals.",  { it.totalMealsCooked >= 150 }),
    TOP_CHEF("topChef",             "Top Chef",               AwardIcon.LAUREL, AwardTier.GOLD,  "Cooked 200 meals.",  { it.totalMealsCooked >= 200 }),
    GRAND_MASTER_CHEF("grandMasterChef",     "Grand Master Chef",      AwardIcon.CROWN,  AwardTier.GOLD,  "Cooked 250 meals + own 20 recipes.",    { it.totalMealsCooked >= 250  && it.customRecipeCount >= 20 }),
    LEGENDARY_CHEF("legendaryChef",       "Legendary Chef",         AwardIcon.CROWN,  AwardTier.GOLD,  "Cooked 500 meals + own 50 recipes.",    { it.totalMealsCooked >= 500  && it.customRecipeCount >= 50 }),
    SUPER_LEGENDARY_CHEF("superLegendaryChef",  "Super Legendary Chef",   AwardIcon.CROWN,  AwardTier.GOLD,  "Cooked 1500 meals + own 150 recipes.",  { it.totalMealsCooked >= 1500 && it.customRecipeCount >= 150 }),
    GRAND_LEGENDARY_CHEF("grandLegendaryChef",  "Grand Legendary Chef",   AwardIcon.CROWN,  AwardTier.GOLD,  "Cooked 3000 meals.",                    { it.totalMealsCooked >= 3000 });

    companion object {
        fun fromId(id: String?): StreakReward? = entries.firstOrNull { it.id == id }
    }
}

// ── State ────────────────────────────────────────────────────────────────────

data class StreakState(
    val currentStreak   : Int  = 0,
    val mealsThisWeek   : Int  = 0,
    val totalMealsCooked: Int  = 0,
    /** Epoch millis of the last cook, or null before the first one. */
    val lastCookedDate  : Long? = null,
    /** Every award ever unlocked, in declaration (grid) order. */
    val earnedAwards    : List<StreakReward> = emptyList(),
    /** Set when a cook unlocks an award; Home shows the 🎉 banner and calls [StreakService.clearBanner]. */
    val bannerReward    : StreakReward? = null,
) {
    /** Lifetime "$ saved from eating out", whole dollars. */
    val estimatedSavings: Int get() = totalMealsCooked * StreakService.SAVING_PER_MEAL
    /** This week's savings, whole dollars — the Home streak card pill. */
    val weeklySavings   : Int get() = mealsThisWeek * StreakService.SAVING_PER_MEAL
    /** Icon shown beside the name in the hero (iOS `earnedRewards().last`). */
    val latestAward     : StreakReward? get() = earnedAwards.lastOrNull()
}

// ── Service ──────────────────────────────────────────────────────────────────

/**
 * Port of iOS StreakService: daily cooking streak, weekly / lifetime meal
 * counts and award unlocks, persisted in DataStore under the same `streak_*`
 * keys. All day boundaries use Australia/Sydney, as iOS does deliberately.
 */
@Singleton
class StreakService @Inject constructor(
    private val prefs    : UserPreferencesRepository,
    private val myRecipes: MyRecipeRepository,
) {
    companion object {
        /** AU average takeaway $22 minus home cook $6. */
        const val SAVING_PER_MEAL = 16
        val ZONE: ZoneId = ZoneId.of("Australia/Sydney")

        fun toDay(epochMillis: Long): LocalDate = Instant.ofEpochMilli(epochMillis).atZone(ZONE).toLocalDate()

        // ── Pure day-boundary logic (unit-tested) ──

        /** Streak after a cook on [today]: same day keeps it, the next day extends it, any gap restarts at 1. */
        fun streakAfterCooking(lastCookedDay: LocalDate?, today: LocalDate, current: Int): Int = when {
            lastCookedDay == null                            -> 1
            lastCookedDay == today                           -> current
            ChronoUnit.DAYS.between(lastCookedDay, today) == 1L -> current + 1
            else                                             -> 1
        }

        /** Streak seen on launch: 2+ missed days zero it (iOS checkStreakExpiry). */
        fun streakAfterExpiry(lastCookedDay: LocalDate?, today: LocalDate, current: Int): Int =
            if (lastCookedDay != null && ChronoUnit.DAYS.between(lastCookedDay, today) >= 2) 0 else current

        /** ISO week identity, so "meals this week" resets when the week changes. */
        fun weekKey(day: LocalDate): Pair<Int, Int> =
            day.get(IsoFields.WEEK_BASED_YEAR) to day.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)

        /** Weekday cells (Mon=0 … Sun=6) to paint green this week — iOS cookedWeekdays(). */
        fun cookedWeekdays(lastCookedDay: LocalDate?, today: LocalDate, mealsThisWeek: Int): Set<Int> {
            if (lastCookedDay == null) return emptySet()
            val days = (0 until minOf(mealsThisWeek, 7)).map { today.minusDays(it.toLong()).dayOfWeek.value - 1 }.toMutableSet()
            days += lastCookedDay.dayOfWeek.value - 1
            return days
        }

        /** Flips the flag for every award newly satisfied by [c]; returns them in declaration order. */
        fun evaluateAwards(p: MutablePreferences, c: AwardCounters): List<StreakReward> =
            StreakReward.entries.filter { award ->
                val key = streakAwardedKey(award.id)
                if (p[key] == true || !award.isUnlocked(c)) return@filter false
                p[key] = true
                true
            }

        private fun Preferences.toState(today: LocalDate): StreakState {
            val lastCooked = this[KEY_STREAK_LAST_COOKED]
            val weekStart  = this[KEY_STREAK_WEEK_START]
            val sameWeek   = weekStart == null || weekKey(toDay(weekStart)) == weekKey(today)
            return StreakState(
                currentStreak    = streakAfterExpiry(lastCooked?.let(::toDay), today, this[KEY_STREAK_CURRENT] ?: 0),
                mealsThisWeek    = if (sameWeek) this[KEY_STREAK_MEALS_WEEK] ?: 0 else 0,
                totalMealsCooked = this[KEY_STREAK_TOTAL] ?: 0,
                lastCookedDate   = lastCooked,
                earnedAwards     = StreakReward.entries.filter { this[streakAwardedKey(it.id)] == true },
                bannerReward     = StreakReward.fromId(this[KEY_STREAK_BANNER]),
            )
        }
    }

    init {
        // iOS runs checkStreakExpiry() on launch; the singleton is built on first injection (app start).
        CoroutineScope(Dispatchers.IO).launch { checkStreakExpiry() }
    }

    /** Live streak snapshot. Expiry and week reset are applied on read, so the numbers are right even before [checkStreakExpiry] persists them. */
    val state: Flow<StreakState> = prefs.preferences.map { it.toState(LocalDate.now(ZONE)) }

    /** Call after the user confirms they cooked a meal (both Plan & Cook confirm paths). */
    suspend fun recordMealCooked() {
        val recipeCount = myRecipes.recipes.first().size
        prefs.edit { p ->
            val now     = System.currentTimeMillis()
            val today   = toDay(now)
            val lastDay = p[KEY_STREAK_LAST_COOKED]?.let(::toDay)
            if (lastDay != today) {
                p[KEY_STREAK_CURRENT]     = streakAfterCooking(lastDay, today, p[KEY_STREAK_CURRENT] ?: 0)
                p[KEY_STREAK_LAST_COOKED] = now
                resetWeekIfNeeded(p, now)
            }
            p[KEY_STREAK_MEALS_WEEK] = (p[KEY_STREAK_MEALS_WEEK] ?: 0) + 1
            p[KEY_STREAK_TOTAL]      = (p[KEY_STREAK_TOTAL] ?: 0) + 1
            val newly = evaluateAwards(p, AwardCounters(p[KEY_STREAK_TOTAL] ?: 0, p[KEY_STREAK_CURRENT] ?: 0, recipeCount))
            newly.firstOrNull()?.let { p[KEY_STREAK_BANNER] = it.id }
        }
    }

    /** Persists a zeroed streak after a 2+ day gap and rolls the week (iOS checkStreakExpiry). */
    suspend fun checkStreakExpiry() {
        prefs.edit { p ->
            val now   = System.currentTimeMillis()
            val today = toDay(now)
            p[KEY_STREAK_CURRENT] = streakAfterExpiry(p[KEY_STREAK_LAST_COOKED]?.let(::toDay), today, p[KEY_STREAK_CURRENT] ?: 0)
            resetWeekIfNeeded(p, now)
        }
    }

    /** Home calls this once it has shown the 🎉 Congrats banner. */
    suspend fun clearBanner() {
        prefs.edit { it.remove(KEY_STREAK_BANNER) }
    }

    private fun resetWeekIfNeeded(p: MutablePreferences, now: Long) {
        val stored = p[KEY_STREAK_WEEK_START]
        if (stored == null) {
            p[KEY_STREAK_WEEK_START] = now
        } else if (weekKey(toDay(stored)) != weekKey(toDay(now))) {
            p[KEY_STREAK_MEALS_WEEK] = 0
            p[KEY_STREAK_WEEK_START] = now
        }
    }
}
