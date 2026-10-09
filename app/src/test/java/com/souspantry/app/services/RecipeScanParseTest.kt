package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Model JSON → ScannedRecipe, as returned by DirectClaude.scanRecipeText. */
class RecipeScanParseTest {

    @Test fun `full object with code fences and prose`() {
        val r = parseScannedRecipe("""Here you go:
            ```json
            {"name":"Chicken Curry","description":"A weeknight curry.","cuisine":"Indian","difficulty":"Easy",
             "prepTime":"10 min","cookTime":"30 min","servings":4,
             "ingredients":["500g chicken thigh","2 cloves garlic, minced",""],
             "steps":["Brown the chicken.","Simmer 20 min."]}
            ```""")
        assertEquals("Chicken Curry", r.name)
        assertEquals("Indian", r.cuisine)
        assertEquals(4, r.servings)
        assertEquals(listOf("500g chicken thigh", "2 cloves garlic, minced"), r.ingredients)
        assertEquals(2, r.steps.size)
    }

    @Test fun `empty strings become null`() {
        val r = parseScannedRecipe("""{"name":"Toast","cuisine":"","prepTime":"","servings":"2"}""")
        assertNull(r.cuisine)
        assertNull(r.prepTime)
        assertEquals(2, r.servings)
        assertEquals(emptyList<String>(), r.steps)
    }

    @Test fun `no_recipe, missing name and garbage throw`() {
        assertThrows(IllegalStateException::class.java) { parseScannedRecipe("""{"error":"no_recipe"}""") }
        assertThrows(IllegalStateException::class.java) { parseScannedRecipe("""{"ingredients":["x"]}""") }
        assertThrows(IllegalStateException::class.java) { parseScannedRecipe("sorry, blurry") }
    }
}
