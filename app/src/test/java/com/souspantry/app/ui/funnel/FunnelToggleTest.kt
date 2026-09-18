package com.souspantry.app.ui.funnel

import org.junit.Assert.assertEquals
import org.junit.Test

/** "None" is exclusive on every multi-select step (dietary, allergies, …), as on iOS. */
class FunnelToggleTest {

    @Test fun `tapping none clears everything else`() {
        assertEquals(setOf("none"), toggleMultiSelection(setOf("vegetarian"), "none", max = 4))
        assertEquals(setOf("none"), toggleMultiSelection(setOf("vegetarian", "vegan"), "none", max = 4))
    }

    @Test fun `tapping an option while none is selected replaces none`() {
        assertEquals(setOf("pescatarian"), toggleMultiSelection(setOf("none"), "pescatarian", max = 4))
    }

    @Test fun `tapping a selected option deselects it`() {
        assertEquals(setOf("vegan"), toggleMultiSelection(setOf("vegan", "halal"), "halal", max = 4))
        assertEquals(emptySet<String>(), toggleMultiSelection(setOf("none"), "none", max = 4))
    }

    @Test fun `the cap still holds`() {
        val full = setOf("a", "b", "c")
        assertEquals(full, toggleMultiSelection(full, "d", max = 3))
        assertEquals(setOf("a", "b", "c", "d"), toggleMultiSelection(full, "d", max = 4))
    }

    @Test fun `none is dropped before the cap is measured`() {
        // "none" + 2 others at max 3 → none goes, so there's room for the new option.
        assertEquals(setOf("a", "b", "c"), toggleMultiSelection(setOf("none", "a", "b"), "c", max = 3))
    }
}
