package com.souspantry.app.ui.home

import com.souspantry.app.data.models.PantryItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Home dashboard derived values — spec android-home-dashboard-prompt.md §2–§4. */
class HomeStateTest {

    private fun items(n: Int, category: String? = null, expiry: Long? = null) =
        List(n) { PantryItem(name = "item$it", category = category, expiryDate = expiry) }

    @Test fun `meal estimate follows the iOS formula`() {
        assertEquals(0,  HomeState(pantryItems = emptyList()).estimatedRecipeCount)
        assertEquals(3,  HomeState(pantryItems = items(1)).estimatedRecipeCount)
        assertEquals(6,  HomeState(pantryItems = items(48)).estimatedRecipeCount)
        assertEquals(25, HomeState(pantryItems = items(400)).estimatedRecipeCount)
        assertEquals("Suggest 6 meals I can make with my current pantry.", HomeState(pantryItems = items(48)).sousMessage)
    }

    @Test fun `audit items are those within 7 days including expired, soonest first`() {
        val now   = System.currentTimeMillis()
        val day   = 86_400_000L
        val state = HomeState(pantryItems = items(1, expiry = now + 10 * day) + items(1, expiry = now + day) + items(1, expiry = now - 2 * day))
        assertEquals(listOf(now - 2 * day, now + day), state.auditItems.map { it.expiryDate })
    }

    @Test fun `category breakdown groups by normalised category with fractions`() {
        val state = HomeState(pantryItems = items(3, "fresh fruit") + items(1, "Snacks"))
        assertEquals(listOf("Fruits" to 3, "Snacks" to 1), state.categoryBreakdown.map { it.first to it.third })
        assertEquals(0.75f, state.categoryBreakdown.first().second, 0.001f)
    }
}
