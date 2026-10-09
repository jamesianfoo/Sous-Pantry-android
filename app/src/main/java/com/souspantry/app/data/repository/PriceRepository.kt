package com.souspantry.app.data.repository

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.souspantry.app.data.models.OTHERS_STORE
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.PriceRecord
import com.souspantry.app.data.models.ReceiptLineItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Dao
interface PriceRecordDao {
    @Query("SELECT * FROM price_records ORDER BY datePurchased DESC")
    fun getAll(): Flow<List<PriceRecord>>

    @Query("SELECT * FROM price_records WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:name)) ORDER BY datePurchased DESC")
    suspend fun byItemName(name: String): List<PriceRecord>

    @Query("SELECT * FROM price_records WHERE datePurchased BETWEEN :from AND :to ORDER BY datePurchased DESC")
    suspend fun between(from: Long, to: Long): List<PriceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: PriceRecord)

    /** iOS keeps only the 3 most recent records per item (PantryItem.recordPrice). */
    @Query(
        """
        DELETE FROM price_records
        WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:name))
          AND id NOT IN (
            SELECT id FROM price_records
            WHERE LOWER(TRIM(itemName)) = LOWER(TRIM(:name))
            ORDER BY datePurchased DESC LIMIT 3
          )
        """
    )
    suspend fun trimToRecent(name: String)

    @Query("DELETE FROM price_records")
    suspend fun deleteAll()
}

@Singleton
class PriceRepository @Inject constructor(private val dao: PriceRecordDao) {
    val records: Flow<List<PriceRecord>> = dao.getAll()

    suspend fun record(record: PriceRecord) {
        dao.upsert(record)
        dao.trimToRecent(record.itemName)
    }

    suspend fun byItemName(name: String)       = dao.byItemName(name)
    suspend fun between(from: Long, to: Long)  = dao.between(from, to)
    suspend fun deleteAll()                    = dao.deleteAll()

    /**
     * Mirrors iOS insertItems / syncReceipt: one record per receipt line that
     * carries a unit price, dated now. [lines] and [items] are parallel lists.
     * [storeName] is the parsed store (or the eReceipt session store); when it
     * matches one of [knownStores] (case-insensitive) that canonical label is
     * used, otherwise the record is filed under "Others" with the raw name kept.
     * Amounts are stored as printed on the receipt — no currency conversion.
     */
    suspend fun recordReceipt(
        lines      : List<ReceiptLineItem>,
        items      : List<PantryItem>,
        storeName  : String?,
        knownStores: List<String>,
    ) {
        val raw       = storeName?.trim()?.ifBlank { null }
        val canonical = knownStores.firstOrNull { it.equals(raw, ignoreCase = true) }
        for ((line, item) in lines.zip(items)) {
            val price = line.price ?: continue
            if (price <= 0) continue
            record(
                PriceRecord(
                    pantryItemId = item.id,
                    itemName     = item.name,
                    pricePerUnit = price,
                    unitLabel    = line.quantity ?: "each",
                    store        = canonical ?: OTHERS_STORE,
                    customStore  = if (canonical == null) raw else null,
                )
            )
        }
    }
}
