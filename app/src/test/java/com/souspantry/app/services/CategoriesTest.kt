package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Port of iOS PantryItem.normalizeCategory — spec android-home-dashboard-prompt.md §4. */
class CategoriesTest {

    @Test fun `null stays null`() {
        assertNull(Categories.normalizeCategory(null))
    }

    @Test fun `canonical names pass through regardless of case`() {
        assertEquals("Pantry Staples", Categories.normalizeCategory("pantry staples"))
        assertEquals("Other",          Categories.normalizeCategory("Other"))
        assertEquals(14, Categories.ORDER.size)
    }

    @Test fun `keywords map in iOS order`() {
        assertEquals("Fruits",              Categories.normalizeCategory("Fresh Fruit & Veg"))   // fruit wins over veg
        assertEquals("Vegetables",          Categories.normalizeCategory("Sweet Potato"))        // vegetables before snacks
        assertEquals("Meat & Seafood",      Categories.normalizeCategory("Deli"))
        assertEquals("Dairy & Eggs",        Categories.normalizeCategory("Eggs"))
        assertEquals("Bakery & Bread",      Categories.normalizeCategory("Bread Rolls"))
        assertEquals("Cereals & Breakfast", Categories.normalizeCategory("Oats"))
        assertEquals("Snacks",              Categories.normalizeCategory("Sweets"))
        assertEquals("Beverages",           Categories.normalizeCategory("Soft Drink"))
        assertEquals("Frozen Foods",        Categories.normalizeCategory("Frozen"))
        assertEquals("Condiments & Sauces", Categories.normalizeCategory("Olive Oil"))
        assertEquals("Vegetables",          Categories.normalizeCategory("Tinned Tomatoes"))     // tomato wins over tinned
        assertEquals("Canned & Packaged",   Categories.normalizeCategory("Canned Goods"))
        assertEquals("Pantry Staples",      Categories.normalizeCategory("Pasta & Rice"))
        assertEquals("Health & Nutrition",  Categories.normalizeCategory("Protein Powder"))
    }

    @Test fun `spring roll is not bakery and unknown falls to Other`() {
        assertEquals("Other", Categories.normalizeCategory("Spring Roll"))
        assertEquals("Other", Categories.normalizeCategory("Cleaning & Household"))
    }
}
