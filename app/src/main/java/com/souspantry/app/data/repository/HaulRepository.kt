package com.souspantry.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.souspantry.app.data.models.HaulRecord
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.services.PriceInsights
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** iOS keeps the last 36 hauls (≈ 3 years of weekly shops). */
const val HAUL_HISTORY_CAP = 36

/** Per-unit price × units bought, so "2 x 500g" counts twice (iOS insertItems totalCost). */
fun haulCost(lines: List<ReceiptLineItem>): Double =
    lines.sumOf { (it.price ?: 0.0) * maxOf(1, it.count) }

@Dao
interface HaulDao {
    @Query("SELECT * FROM haul_history ORDER BY date ASC")
    fun getAll(): Flow<List<HaulRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(haul: HaulRecord)

    @Query("DELETE FROM haul_history WHERE id NOT IN (SELECT id FROM haul_history ORDER BY date DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Query("DELETE FROM haul_history")
    suspend fun deleteAll()
}

@Singleton
class HaulRepository @Inject constructor(private val dao: HaulDao) {
    /** All recorded hauls, oldest first (iOS HaulStore.haulHistory). */
    val history: Flow<List<HaulRecord>> = dao.getAll()

    /** The same history in the shape [PriceInsights.monthlySpend] / [PriceInsights.spendingStreak] take. */
    val entries: Flow<List<PriceInsights.HaulEntry>> =
        history.map { hauls -> hauls.map { PriceInsights.HaulEntry(it.date, it.cost) } }

    /**
     * Mirrors iOS HaulStore.recordHaul: appends to history only when the
     * receipt carried a real cost, so trend data stays meaningful.
     */
    suspend fun record(lines: List<ReceiptLineItem>, storeName: String?) {
        val cost = haulCost(lines)
        if (cost <= 0 || lines.isEmpty()) return
        dao.insert(
            HaulRecord(
                cost      = cost,
                count     = lines.size,   // iOS: insertedItems.count (line items, not units)
                storeName = storeName?.trim()?.ifBlank { null },
            )
        )
        dao.trimTo(HAUL_HISTORY_CAP)
    }

    suspend fun deleteAll() = dao.deleteAll()
}
