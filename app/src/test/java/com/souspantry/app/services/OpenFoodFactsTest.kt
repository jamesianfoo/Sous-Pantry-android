package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure OFF JSON → BarcodeResult mapping, mirroring iOS OpenFoodFactsService. */
class OpenFoodFactsTest {

    private fun off(product: String) = """{"code":"3017620422003","status":1,"status_verbose":"product found","product":$product}"""

    @Test fun `prefers English name over contributor language`() {
        val r = parseOffProduct(off("""{
            "product_name":"Sirop De Maïs","product_name_en":"Corn Syrup","generic_name_en":"Sweet syrup",
            "brands":"Lyle's, Tate & Lyle","categories":"Sweeteners, Syrups, Corn syrups",
            "quantity":"500 g","image_url":"https://images.openfoodfacts.org/1.jpg"
        }"""))!!
        assertEquals("Corn Syrup", r.name)
        assertEquals("Lyle's", r.brand)
        assertEquals("500 g", r.quantity)
        assertEquals("https://images.openfoodfacts.org/1.jpg", r.imageUrl)
        assertEquals(Categories.normalizeCategory("Sweeteners"), r.category)
    }

    @Test fun `falls back generic_name_en, product_name, generic_name in order`() {
        assertEquals("Hazelnut spread", parseOffProduct(off("""{"product_name":"Pâte à tartiner","generic_name_en":"Hazelnut spread"}"""))!!.name)
        assertEquals("Pâte à tartiner", parseOffProduct(off("""{"product_name":"Pâte à tartiner","product_name_en":"  ","generic_name":"Pâte"}"""))!!.name)
        assertEquals("Pâte", parseOffProduct(off("""{"generic_name":"Pâte"}"""))!!.name)
    }

    @Test fun `missing product or name is not found`() {
        assertNull(parseOffProduct("""{"code":"000","status":0,"status_verbose":"product not found"}"""))
        assertNull(parseOffProduct(off("""{"brands":"Acme"}""")))
        assertNull(parseOffProduct("not json"))
    }

    @Test fun `optional fields are null, not blank`() {
        val r = parseOffProduct(off("""{"product_name":"Milk","brands":"","quantity":"","categories":""}"""))!!
        assertEquals("Milk", r.name)
        assertNull(r.brand)
        assertNull(r.quantity)
        assertNull(r.category)
        assertNull(r.imageUrl)
    }

    @Test fun `non-English heuristic`() {
        assertTrue(looksNonEnglish("Sirop De Maïs"))
        assertTrue(looksNonEnglish("Huile d'olive"))
        assertTrue(looksNonEnglish("Jus de pomme"))
        assertFalse(looksNonEnglish("Corn Syrup"))
        assertFalse(looksNonEnglish("Demerara Sugar"))
    }
}
