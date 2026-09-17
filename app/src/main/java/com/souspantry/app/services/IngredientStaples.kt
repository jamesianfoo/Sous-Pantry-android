package com.souspantry.app.services

/**
 * Basics every kitchen is assumed to have. Mirrors iOS `IngredientScaler.swift`.
 *
 * Staples always count as in-pantry, never appear in a to-buy list, and never
 * drag down a pantry-match percentage. This is the single source of truth —
 * do not re-declare the list per screen.
 */
object IngredientStaples {

    /**
     * Deliberately excludes real ingredients that genuinely run out:
     * olive oil, butter, rice, flour, soy sauce, stock. Don't extend
     * without checking with James.
     */
    private val STAPLES = setOf(
        "water", "tap water", "cold water", "hot water", "boiling water", "warm water", "ice",
        "salt", "table salt", "sea salt", "kosher salt", "fine salt", "salt and pepper", "seasoning",
        "pepper", "black pepper", "ground pepper", "white pepper", "ground black pepper",
        "sugar", "white sugar", "granulated sugar",
        "oil", "cooking oil", "vegetable oil", "canola oil",
    )

    private val LEADING = listOf(
        "a pinch of ", "pinch of ", "a splash of ", "splash of ", "a drizzle of ", "drizzle of ",
        "a dash of ", "dash of ", "freshly ground ", "freshly cracked ", "fresh ",
    )

    private val TRAILING = listOf(
        ", to taste", " to taste", " to season", " for seasoning", " for frying", " for greasing",
        " for dusting", " for drizzling", " as needed", " (optional)", " optional",
    )

    /**
     * Exact match only, never substring — "salted butter", "sugar snap peas" and
     * "water chestnuts" are real ingredients and must not match.
     *
     * Accepts either a bare name ("salt to taste") or a full recipe line
     * ("500ml boiling water"), since callers have both.
     */
    fun isPantryStaple(name: String): Boolean =
        normalize(name) in STAPLES || normalize(IngredientScaler.cleanName(name)) in STAPLES

    private fun normalize(raw: String): String {
        var s = raw.lowercase().trim()
        for (p in LEADING) if (s.startsWith(p)) { s = s.removePrefix(p); break }
        for (p in TRAILING) if (s.endsWith(p)) { s = s.removeSuffix(p); break }
        return s.trim()
    }
}
