package com.souspantry.app.services

/**
 * The "What to buy" list for an external recipe, computed from the page's real
 * scraped JSON-LD ingredients. Mirrors iOS `MissingIngredientsSheet`.
 *
 * The AI's ingredient list is only a fallback for pages without structured
 * data: the model never reads the page, so it guesses ("white rice" for a
 * prawn pasta).
 */
object BuyList {

    data class Result(val missing: List<String>, val matchPercent: Int)

    fun fromScraped(lines: List<String>, pantryNames: List<String>): Result {
        val pantry = pantryNames.map { it.trim() }.filter { it.isNotEmpty() }
        val missing = mutableListOf<String>()
        for (raw in lines) {
            val name = IngredientScaler.cleanName(raw).trim()
            if (name.isEmpty() || IngredientStaples.isPantryStaple(name)) continue
            val inPantry = pantry.any { name.contains(it, ignoreCase = true) || it.contains(name, ignoreCase = true) }
            if (!inPantry && missing.none { it.equals(name, ignoreCase = true) }) missing += name
        }
        // Staples and pantry items count as on hand, so the % agrees with the list.
        val pct = if (lines.isEmpty()) 0 else (lines.size - missing.size) * 100 / lines.size
        return Result(missing, pct)
    }
}
