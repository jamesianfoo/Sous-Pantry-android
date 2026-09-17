package com.souspantry.app.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * The slug must match the Worker's `slugKey()` and iOS byte-for-byte, or Android
 * looks up a different image than iOS generated. Expected values were computed
 * from the Worker's JS regex, not from this implementation.
 */
class RecipeImagesTest {

    @Test fun `slug matches the worker`() {
        assertEquals("kimchi-fried-rice-with-tuna", RecipeImages.slug("Kimchi Fried Rice with Tuna!"))
        assertEquals("tom-yum-goong", RecipeImages.slug("  --Tom Yum Goong!!  "))
        assertEquals("cr-me-br-l-e", RecipeImages.slug("Crème Brûlée"))
    }

    @Test fun `empty or symbol-only titles fall back to recipe`() {
        assertEquals("recipe", RecipeImages.slug(""))
        assertEquals("recipe", RecipeImages.slug("!!!"))
    }

    @Test fun `slug is capped at 80 characters`() {
        assertEquals(80, RecipeImages.slug("a".repeat(100)).length)
    }

    @Test fun `stable hash is djb2 and case-insensitive`() {
        assertEquals(229484126523376L, RecipeImages.stableHash("Tom Yum"))
        assertEquals(RecipeImages.stableHash("Tom Yum"), RecipeImages.stableHash("tom yum"))
    }

    @Test fun `two dishes sharing a cuisine pick different fallback photos`() {
        assertNotEquals(RecipeImages.stableHash("Pad Thai") % 10, RecipeImages.stableHash("Green Curry") % 10)
    }
}
