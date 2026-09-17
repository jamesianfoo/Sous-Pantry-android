package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Spec: android-buy-list-fix-prompt.md — the list comes from the page, never the AI's guess. */
class BuyListTest {

    // RecipeTinEats "Creamy Garlic Prawn Pasta" line format, including dual metric/imperial measures.
    private val prawnPasta = listOf(
        "250g / 8 oz fettuccine or other long strand pasta",
        "40g / 3 tbsp unsalted butter",
        "500 g / 1 lb small or medium peeled prawns , raw",
        "4 garlic cloves , finely minced",
        "3/4 cup Chardonnay or other dry white wine (Note 2)",
        "1 1/4 cups heavy / thickened cream",
        "1/2 cup chicken broth / stock , low sodium",
        "1/2 cup parmesan , finely grated",
        "1/4 tsp salt",
        "1/4 tsp black pepper",
        "1 tbsp parsley , finely chopped",
        "1/4 to 1/3 cup milk",
    )

    @Test fun `cleaner drops the second measure of a dual metric-imperial quantity`() {
        assertEquals("Fettuccine or other long strand pasta", IngredientScaler.cleanName(prawnPasta[0]))
        assertEquals("Unsalted butter", IngredientScaler.cleanName(prawnPasta[1]))
        assertEquals("Small or medium peeled prawns", IngredientScaler.cleanName(prawnPasta[2]))
    }

    @Test fun `cleaner removes parentheticals whole, including nested ones`() {
        assertEquals("Peanut oil", IngredientScaler.cleanName("2 tbsp peanut oil (or canola or vegetable oil)"))
        assertEquals("Raw cashews", IngredientScaler.cleanName("1/2 cup raw cashews, unsalted (Note 1 for roasted)"))
        assertEquals("Jasmine rice", IngredientScaler.cleanName("Jasmine rice, for serving (or other rice of choice)"))
        assertEquals("Green onions", IngredientScaler.cleanName("2 green onions, cut into 2.5cm / 1\" lengths"))
        assertEquals("Peanut oil", IngredientScaler.cleanName("2 tbsp peanut oil (or canola (Note 2) or vegetable oil)"))
    }

    @Test fun `cleaner never leaves a stray bracket`() {
        assertEquals("Peanut oil", IngredientScaler.cleanName("peanut oil)"))
        assertEquals("Peanut oil", IngredientScaler.cleanName("2 tbsp peanut oil (or canola"))
    }

    @Test fun `buy list is the page ingredients minus staples and pantry`() {
        val result = BuyList.fromScraped(prawnPasta, listOf("Garlic", "Olive oil", "Milk"))
        assertEquals(
            listOf(
                "Fettuccine or other long strand pasta",
                "Unsalted butter",
                "Small or medium peeled prawns",
                "Chardonnay or other dry white wine",
                "Heavy / thickened cream",
                "Chicken broth / stock",
                "Parmesan",
                "Parsley",
            ),
            result.missing,
        )
        val lower = result.missing.map { it.lowercase() }
        assertFalse(lower.any { "rice" in it || "half-and-half" in it })
        assertFalse(lower.any { "pepper" in it || it == "salt" })
    }

    @Test fun `match percent agrees with the list`() {
        val result = BuyList.fromScraped(prawnPasta, listOf("Garlic", "Olive oil", "Milk"))
        assertEquals((12 - 8) * 100 / 12, result.matchPercent)
    }

    @Test fun `duplicates collapse case-insensitively in first-seen order`() {
        val result = BuyList.fromScraped(listOf("2 tbsp Butter", "1 tbsp butter", "1 lemon"), emptyList())
        assertEquals(listOf("Butter", "Lemon"), result.missing)
    }

    @Test fun `a fully stocked pantry leaves nothing to buy`() {
        val result = BuyList.fromScraped(listOf("1 tsp salt", "200g spaghetti"), listOf("Spaghetti"))
        assertTrue(result.missing.isEmpty())
        assertEquals(100, result.matchPercent)
    }
}
