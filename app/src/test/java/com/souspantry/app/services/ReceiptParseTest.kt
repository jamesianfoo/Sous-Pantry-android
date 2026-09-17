package com.souspantry.app.services

import com.souspantry.app.ui.ereceipt.REGION_STORES
import com.souspantry.app.ui.funnel.REGION_SHOPS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: android-ereceipt-all-stores-prompt.md — one generic pipeline for every store. */
class ReceiptParseTest {

    @Test fun `object shape gives store, counts and prices`() {
        val r = parseReceiptJson(
            """{"storeName":"Jaya Grocer","items":[
                {"name":"Full Cream Milk","quantity":"1L","count":2,"category":"Dairy & Eggs","price":7.9},
                {"name":"Jasmine Rice","quantity":"5kg","count":1,"category":"Pantry & Dry Goods","price":32.5}
            ]}""",
        )
        assertEquals("Jaya Grocer", r.storeName)
        assertEquals(listOf("Full Cream Milk", "Jasmine Rice"), r.items.map { it.name })
        assertEquals(listOf(2, 1), r.items.map { it.count })
        assertEquals(7.9, r.items[0].price!!, 0.001)
        assertEquals("1L", r.items[0].quantity)
    }

    @Test fun `bare array is accepted`() {
        val r = parseReceiptJson("""[{"name":"Bananas","quantity":null,"count":3,"category":"Fruits","price":null}]""")
        assertNull(r.storeName)
        assertEquals(3, r.items.single().count)
        assertNull(r.items.single().price)
    }

    @Test fun `model quirks are tolerated`() {
        val r = parseReceiptJson(
            """{"storeName":"null","items":[
                {"name":"Eggs","quantity":"null","count":0,"category":"Dairy & Eggs","price":"5.50"},
                {"name":"Bread","category":"Bakery & Bread"},
                {"name":"","count":1}
            ]}""",
        )
        assertNull(r.storeName)
        assertEquals(listOf("Eggs", "Bread"), r.items.map { it.name })   // nameless line dropped
        assertEquals(listOf(1, 1), r.items.map { it.count })             // 0 and missing → 1
        assertNull(r.items[0].quantity)                                   // "null" string → null
        assertEquals(5.5, r.items[0].price!!, 0.001)                      // numeric string price
    }

    @Test fun `a page that is not a receipt yields no items`() {
        assertTrue(parseReceiptJson("""{"storeName":"Tesco","items":[]}""").items.isEmpty())
    }

    @Test fun `every wizard region with shops has sync stores`() {
        assertEquals(REGION_SHOPS.keys, REGION_STORES.keys)
    }

    @Test fun `store ids follow the wizard slug convention`() {
        val ids = REGION_STORES.values.flatten().map { it.id }.toSet()
        listOf("sainsburys", "lotuss", "paknsave", "trader_joes", "jaya_grocer", "aldi", "iga").forEach {
            assertTrue("missing slug $it", it in ids)
        }
        // AU ids predate the convention and must not change, or existing connections vanish.
        assertTrue("coles_instore" in ids && "coles_online" in ids && "woolworths" in ids)
    }

    @Test fun `metro canada avoids the blocked account route`() {
        val metro = REGION_STORES.getValue("CA").single { it.name == "Metro" }
        assertEquals("https://www.metro.ca/en", metro.receiptUrl)
    }
}
