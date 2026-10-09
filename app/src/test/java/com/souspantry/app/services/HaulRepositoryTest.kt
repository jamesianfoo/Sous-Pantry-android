package com.souspantry.app.services

import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.data.repository.HAUL_HISTORY_CAP
import com.souspantry.app.data.repository.haulCost
import org.junit.Assert.assertEquals
import org.junit.Test

/** iOS insertItems: haul cost = Σ unit price × max(1, count); lines without a price add nothing. */
class HaulRepositoryTest {

    private fun line(price: Double?, count: Int = 1) =
        ReceiptLineItem(name = "x", quantity = null, category = null, count = count, price = price)

    @Test fun `haul cost multiplies unit price by units bought`() {
        val lines = listOf(
            line(2.50, count = 2),   // 5.00
            line(4.00),              // 4.00
            line(null, count = 3),   // no price → 0
            line(1.25, count = 0),   // count floors to 1 → 1.25
        )
        assertEquals(10.25, haulCost(lines), 0.0001)
    }

    @Test fun `no prices means no cost and no history row`() {
        assertEquals(0.0, haulCost(listOf(line(null), line(null, 4))), 0.0)
        assertEquals(0.0, haulCost(emptyList()), 0.0)
        assertEquals(36, HAUL_HISTORY_CAP)
    }
}
