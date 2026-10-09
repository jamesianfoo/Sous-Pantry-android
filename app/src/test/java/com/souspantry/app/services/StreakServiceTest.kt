package com.souspantry.app.services

import androidx.datastore.preferences.core.mutablePreferencesOf
import com.souspantry.app.data.local.UserPreferencesRepository.Companion.streakAwardedKey
import com.souspantry.app.services.StreakService.Companion.cookedWeekdays
import com.souspantry.app.services.StreakService.Companion.evaluateAwards
import com.souspantry.app.services.StreakService.Companion.streakAfterCooking
import com.souspantry.app.services.StreakService.Companion.streakAfterExpiry
import com.souspantry.app.services.StreakService.Companion.weekKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Mirrors iOS StreakService.swift — spec android-home-dashboard-prompt.md §5 / §5a. */
class StreakServiceTest {

    private val today = LocalDate.of(2026, 10, 9)   // a Friday

    // ── Day-boundary / consecutive logic ─────────────────────────────────────

    @Test fun `first cook starts a 1-day streak`() {
        assertEquals(1, streakAfterCooking(null, today, 0))
    }

    @Test fun `same day keeps the streak, next day extends it, a gap restarts it`() {
        assertEquals(3, streakAfterCooking(today,              today, 3))
        assertEquals(4, streakAfterCooking(today.minusDays(1), today, 3))
        assertEquals(1, streakAfterCooking(today.minusDays(2), today, 3))
        assertEquals(1, streakAfterCooking(today.minusDays(30), today, 9))
    }

    @Test fun `expiry zeroes only after two missed days`() {
        assertEquals(5, streakAfterExpiry(null,               today, 5))
        assertEquals(5, streakAfterExpiry(today,              today, 5))
        assertEquals(5, streakAfterExpiry(today.minusDays(1), today, 5))
        assertEquals(0, streakAfterExpiry(today.minusDays(2), today, 5))
    }

    @Test fun `sydney day boundary`() {
        // 2026-10-08 23:30 Sydney (AEDT, UTC+11) is 12:30 UTC the same day; 2026-10-09 00:30 Sydney is 13:30 UTC on the 8th.
        assertEquals(LocalDate.of(2026, 10, 8), StreakService.toDay(1791462600000L))   // 2026-10-08T12:30:00Z
        assertEquals(LocalDate.of(2026, 10, 9), StreakService.toDay(1791466200000L))   // 2026-10-08T13:30:00Z
    }

    @Test fun `week key follows ISO weeks`() {
        assertEquals(weekKey(LocalDate.of(2026, 10, 5)), weekKey(LocalDate.of(2026, 10, 11)))   // Mon..Sun
        assertTrue(weekKey(LocalDate.of(2026, 10, 11)) != weekKey(LocalDate.of(2026, 10, 12)))
    }

    @Test fun `cooked weekdays are today backwards plus the last cook`() {
        assertEquals(emptySet<Int>(), cookedWeekdays(null, today, 3))
        // Friday=4; 3 meals this week → Fri, Thu, Wed; last cook Monday adds 0.
        assertEquals(setOf(4, 3, 2, 0), cookedWeekdays(today.minusDays(4), today, 3))
    }

    // ── Awards ────────────────────────────────────────────────────────────────

    @Test fun `award list matches iOS order and count`() {
        assertEquals(24, StreakReward.entries.size)
        assertEquals("kitchenExplorer",     StreakReward.entries.first().id)
        assertEquals("homeCook",            StreakReward.entries[11].id)
        assertEquals("grandLegendaryChef",  StreakReward.entries.last().id)
        assertEquals(StreakReward.HOME_COOK, StreakReward.fromId("homeCook"))
    }

    @Test fun `thresholds unlock in declaration order and only once`() {
        val p = mutablePreferencesOf()
        assertEquals(emptyList<StreakReward>(), evaluateAwards(p, AwardCounters(4, 1, 0)))
        assertEquals(listOf(StreakReward.HOME_COOK), evaluateAwards(p, AwardCounters(5, 1, 0)))
        assertEquals(true, p[streakAwardedKey("homeCook")])
        // Already flagged → not returned again; streak 7 and 20 meals both land, streak first (declaration order).
        assertEquals(listOf(StreakReward.PERFECT_WEEK, StreakReward.SUPER_HOME_COOK), evaluateAwards(p, AwardCounters(20, 7, 0)))
    }

    @Test fun `compound chef tiers need own recipes, the top tier does not`() {
        val c = AwardCounters(totalMealsCooked = 3000, currentStreak = 0, customRecipeCount = 0)
        assertTrue(!StreakReward.GRAND_MASTER_CHEF.isUnlocked(c))
        assertTrue(!StreakReward.LEGENDARY_CHEF.isUnlocked(c))
        assertTrue(StreakReward.GRAND_LEGENDARY_CHEF.isUnlocked(c))
        assertTrue(StreakReward.GRAND_MASTER_CHEF.isUnlocked(c.copy(customRecipeCount = 20)))
        // Scan / rescue / speed ladders stay locked with the default zero counters.
        assertTrue(!StreakReward.KITCHEN_EXPLORER.isUnlocked(c))
        assertTrue(!StreakReward.SPEED_COOK.isUnlocked(c))
    }

    @Test fun `savings are whole dollars at 16 per meal`() {
        val s = StreakState(mealsThisWeek = 3, totalMealsCooked = 10, earnedAwards = listOf(StreakReward.HOME_COOK, StreakReward.PERFECT_WEEK))
        assertEquals(48, s.weeklySavings)
        assertEquals(160, s.estimatedSavings)
        assertEquals(StreakReward.PERFECT_WEEK, s.latestAward)
    }
}
