package com.souspantry.app.services

import com.souspantry.app.data.models.PriceRecord
import com.souspantry.app.data.models.displayStore
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.abs

/**
 * Pure computations behind the Home "Price Trends" sheet, mirroring iOS
 * PriceTrendsSheet.swift. Records are grouped per item by trimmed,
 * case-insensitive [PriceRecord.itemName]. Monthly spend and the spending
 * streak read haul totals (iOS HaulStore.haulHistory), supplied as [HaulEntry].
 * Currency formatting is the caller's job (wizard currency), so copy that
 * embeds an amount takes the already-formatted string.
 */
object PriceInsights {

    const val DEFAULT_SPEND_THRESHOLD = 150.0
    /** The SPENDING STREAK section is hidden below this many visits. */
    const val MIN_STREAK_VISITS       = 2

    const val EMPTY_TOP_MOVERS    = "No price changes yet. Record 2+ prices for an item to see movement."
    const val EMPTY_MONTHLY_SPEND = "Record a few shops to see monthly spending trends."

    data class TopMover(
        val itemName : String,
        val oldPrice : Double,
        val newPrice : Double,
        val newStore : String,
        val changePct: Double,   // fraction: 0.12 == +12%
        val unitLabel: String,
    ) {
        val isUp  : Boolean get() = changePct >= 0
        /** "+12%" / "-8%", as the iOS capsule shows it. */
        val pctText: String get() = String.format(Locale.US, "%+.0f%%", changePct * 100)
    }

    data class MonthlySpend(val month: YearMonth, val total: Double)

    data class BestStore(
        val itemName      : String,
        val cheapStore    : String,
        val expensiveStore: String,
        val savings       : Double,
    )

    /** One shop: when it happened (epoch millis) and what it cost. */
    data class HaulEntry(val date: Long, val cost: Double)

    private fun List<PriceRecord>.byItem(): Collection<List<PriceRecord>> =
        groupBy { it.itemName.trim().lowercase() }.values

    /** Items with 2+ records: up to 5 with the largest |% change| oldest → newest. */
    fun topMovers(records: List<PriceRecord>): List<TopMover> =
        records.byItem().mapNotNull { recs ->
            val sorted = recs.sortedBy { it.datePurchased }
            if (sorted.size < 2) return@mapNotNull null
            val oldest = sorted.first()
            val newest = sorted.last()
            if (oldest.pricePerUnit <= 0 || newest.pricePerUnit <= 0) return@mapNotNull null
            TopMover(
                itemName  = newest.itemName,
                oldPrice  = oldest.pricePerUnit,
                newPrice  = newest.pricePerUnit,
                newStore  = newest.displayStore,
                changePct = (newest.pricePerUnit - oldest.pricePerUnit) / oldest.pricePerUnit,
                unitLabel = newest.unitLabel.ifEmpty { "each" },
            )
        }
            .sortedByDescending { abs(it.changePct) }
            .take(5)

    /** Cost totals for the 3 most recent months that have hauls, newest first. */
    fun monthlySpend(hauls: List<HaulEntry>, zone: ZoneId = ZoneId.systemDefault()): List<MonthlySpend> =
        hauls.groupBy { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(zone)) }
            .map { (month, entries) -> MonthlySpend(month, entries.sumOf { it.cost }) }
            .sortedByDescending { it.month }
            .take(3)

    /**
     * Items priced at 2+ stores: latest price per store, cheapest vs priciest,
     * savings over $0.05, top 3 by savings.
     */
    fun bestStores(records: List<PriceRecord>): List<BestStore> =
        records.byItem().mapNotNull { recs ->
            val latestPerStore = recs.filter { it.pricePerUnit > 0 }
                .groupBy { it.displayStore }
                .mapValues { (_, rs) -> rs.maxBy { it.datePurchased } }
            if (latestPerStore.size < 2) return@mapNotNull null
            val cheap  = latestPerStore.entries.minBy { it.value.pricePerUnit }
            val pricey = latestPerStore.entries.maxBy { it.value.pricePerUnit }
            if (cheap.key == pricey.key) return@mapNotNull null
            val savings = pricey.value.pricePerUnit - cheap.value.pricePerUnit
            if (savings <= 0.05) return@mapNotNull null
            BestStore(recs.first().itemName, cheap.key, pricey.key, savings)
        }
            .sortedByDescending { it.savings }
            .take(3)

    /** "{item}: {cheap} saves ~{savings} vs {pricey}" — the 🛒 is a separate glyph in the row. */
    fun bestStoreLine(c: BestStore, savingsText: String) =
        "${c.itemName}: ${c.cheapStore} saves ~$savingsText vs ${c.expensiveStore}"

    /** Consecutive most-recent hauls that cost at most [threshold]. */
    fun spendingStreak(hauls: List<HaulEntry>, threshold: Double = DEFAULT_SPEND_THRESHOLD): Int =
        hauls.sortedByDescending { it.date }.takeWhile { it.cost <= threshold }.size

    fun streakTitle(thresholdText: String) = "Under $thresholdText/shop"

    fun streakBody(thresholdText: String, visits: Int) =
        "You've stayed under $thresholdText/shop for $visits visits in a row 🎉"
}
