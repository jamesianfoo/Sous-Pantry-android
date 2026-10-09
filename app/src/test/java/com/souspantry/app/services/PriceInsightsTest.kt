package com.souspantry.app.services

import com.souspantry.app.data.models.OTHERS_STORE
import com.souspantry.app.data.models.PriceRecord
import com.souspantry.app.services.PriceInsights.HaulEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

/** Spec: android-home-dashboard-prompt.md §3a, mirroring iOS PriceTrendsSheet.swift. */
class PriceInsightsTest {

    private val zone = ZoneOffset.UTC

    private fun day(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun rec(name: String, price: Double, store: String, date: Long, custom: String? = null, unit: String = "each") =
        PriceRecord(itemName = name, pricePerUnit = price, unitLabel = unit, store = store, customStore = custom, datePurchased = date)

    // ── Top movers ────────────────────────────────────────────────────────────

    @Test fun `top movers need two records and compare oldest to newest`() {
        val recs = listOf(
            rec("Milk",  3.00, "Coles",      day(2026, 1, 1), unit = "1L"),
            rec("milk ", 3.60, "Woolworths", day(2026, 2, 1), unit = "1L"),   // same item, different casing
            rec("Eggs",  6.00, "Aldi",       day(2026, 1, 1)),                 // single record → excluded
        )
        val movers = PriceInsights.topMovers(recs)
        assertEquals(1, movers.size)
        val m = movers.single()
        assertEquals("Woolworths", m.newStore)
        assertEquals(0.20, m.changePct, 0.0001)
        assertEquals("+20%", m.pctText)
        assertEquals("1L", m.unitLabel)
        assertTrue(m.isUp)
    }

    @Test fun `top movers sort by absolute change and cap at five`() {
        val recs = (1..7).flatMap { i ->
            listOf(
                rec("Item$i", 10.0,           "Coles", day(2026, 1, 1)),
                rec("Item$i", 10.0 - i * 0.5, "Coles", day(2026, 1, 2)),   // Item7 drops most
            )
        }
        val movers = PriceInsights.topMovers(recs)
        assertEquals(5, movers.size)
        assertEquals("Item7", movers.first().itemName)
        assertEquals("-35%", movers.first().pctText)
        assertTrue(!movers.first().isUp)
    }

    @Test fun `top movers empty without data`() {
        assertTrue(PriceInsights.topMovers(emptyList()).isEmpty())
        assertEquals("No price changes yet. Record 2+ prices for an item to see movement.", PriceInsights.EMPTY_TOP_MOVERS)
    }

    // ── Monthly spend ─────────────────────────────────────────────────────────

    @Test fun `monthly spend sums per month, newest first, last three months`() {
        val hauls = listOf(
            HaulEntry(day(2025, 10, 3), 80.0),
            HaulEntry(day(2025, 11, 5), 90.0),
            HaulEntry(day(2025, 12, 1), 100.0),
            HaulEntry(day(2025, 12, 20), 50.0),
            HaulEntry(day(2026, 1, 9), 120.0),
        )
        val rows = PriceInsights.monthlySpend(hauls, zone)
        assertEquals(listOf(YearMonth.of(2026, 1), YearMonth.of(2025, 12), YearMonth.of(2025, 11)), rows.map { it.month })
        assertEquals(listOf(120.0, 150.0, 90.0), rows.map { it.total })
    }

    @Test fun `monthly spend empty without hauls`() {
        assertTrue(PriceInsights.monthlySpend(emptyList(), zone).isEmpty())
        assertEquals("Record a few shops to see monthly spending trends.", PriceInsights.EMPTY_MONTHLY_SPEND)
    }

    // ── Best store ────────────────────────────────────────────────────────────

    @Test fun `best store uses latest price per store and the savings floor`() {
        val recs = listOf(
            rec("Butter", 5.00, "Coles",      day(2026, 1, 1)),
            rec("Butter", 6.00, "Coles",      day(2026, 1, 8)),                          // latest Coles = 6.00
            rec("Butter", 4.50, OTHERS_STORE, day(2026, 1, 5), custom = "Jaya Grocer"),  // display name from customStore
            rec("Salt",   1.00, "Coles",      day(2026, 1, 1)),
            rec("Salt",   1.04, "Aldi",       day(2026, 1, 2)),                          // 0.04 ≤ 0.05 → excluded
            rec("Rice",   9.00, "Coles",      day(2026, 1, 1)),                          // one store → excluded
        )
        val best = PriceInsights.bestStores(recs)
        assertEquals(1, best.size)
        val b = best.single()
        assertEquals("Butter", b.itemName)
        assertEquals("Jaya Grocer", b.cheapStore)
        assertEquals("Coles", b.expensiveStore)
        assertEquals(1.50, b.savings, 0.0001)
        assertEquals("Butter: Jaya Grocer saves ~$1.50 vs Coles", PriceInsights.bestStoreLine(b, "$1.50"))
    }

    @Test fun `best store caps at three by savings`() {
        val recs = (1..4).flatMap { i ->
            listOf(
                rec("Item$i", 10.0,     "Coles", day(2026, 1, 1)),
                rec("Item$i", 10.0 - i, "Aldi",  day(2026, 1, 1)),
            )
        }
        val best = PriceInsights.bestStores(recs)
        assertEquals(listOf("Item4", "Item3", "Item2"), best.map { it.itemName })
    }

    @Test fun `best store empty without data`() {
        assertTrue(PriceInsights.bestStores(emptyList()).isEmpty())
    }

    // ── Spending streak ───────────────────────────────────────────────────────

    @Test fun `spending streak counts recent shops under threshold until the first over`() {
        val hauls = listOf(
            HaulEntry(day(2026, 1, 1), 200.0),   // over → stops the streak
            HaulEntry(day(2026, 1, 8), 150.0),   // at threshold counts
            HaulEntry(day(2026, 1, 15), 90.0),
            HaulEntry(day(2026, 1, 22), 120.0),
        )
        assertEquals(3, PriceInsights.spendingStreak(hauls))
        assertEquals(2, PriceInsights.spendingStreak(hauls, threshold = 125.0))   // Jan 8 ($150) breaks it
        assertEquals(0, PriceInsights.spendingStreak(emptyList()))
        assertEquals(150.0, PriceInsights.DEFAULT_SPEND_THRESHOLD, 0.0)
        assertEquals(2, PriceInsights.MIN_STREAK_VISITS)
    }

    @Test fun `spending streak copy matches iOS`() {
        assertEquals("Under $150/shop", PriceInsights.streakTitle("$150"))
        assertEquals("You've stayed under $150/shop for 3 visits in a row 🎉", PriceInsights.streakBody("$150", 3))
    }
}
