package com.souspantry.app.services

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Scales ingredient quantities in recipe strings when serving size changes.
 * Mirrors iOS Services/IngredientScaler.swift.
 */
object IngredientScaler {

    private const val UNIT_WORDS = "tbsp|tsp|cups?|oz|g|kg|ml|l|lbs?|pieces?|whole|cloves?|cans?|slices?|" +
        "bunches?|handfuls?|sprigs?|sheets?|tablespoons?|teaspoons?|pinch(?:es)?|dash(?:es)?|sticks?|tins?|" +
        "jars?|packets?|packs?|bottles?|bags?"

    // Innermost parenthetical only; applied until none remain so nested notes
    // ("oil (or canola (Note 2) or vegetable)") come out whole.
    private val PAREN_NOTE = Regex("""\s*\([^()]*\)""")
    private val QTY        = """(?:[\d¼½¾⅓⅔⅛⅜⅝⅞/.]+|to|or|-|–|x|×|\s)+"""
    private val UNIT_PATTERN = Regex("^$QTY(?:$UNIT_WORDS)" + """\b\.?\s*""", RegexOption.IGNORE_CASE)
    // Dual metric/imperial measures ("250g / 8 oz pasta") leave "/ 8 oz …" after the first strip.
    private val SECOND_MEASURE = Regex("^/\\s*$QTY(?:$UNIT_WORDS)" + """\b\.?\s*""", RegexOption.IGNORE_CASE)
    private val PLAIN_NUM    = Regex("""^[\d¼½¾⅓⅔⅛⅜⅝⅞/.]+\s+(?=[a-zA-Z])""")
    private val MEASURE_OF   = Regex(
        """^(?:piece|pieces|knob|thumb|head|clove|cloves|bunch|bunches|sprig|sprigs|handful|handfuls|pinch|dash|slice|slices|stick|sticks|tin|jar|packet|pack|bottle|bag|can|cans|inch|inches)\b[^,]*?\bof\s+(.+)$""",
        RegexOption.IGNORE_CASE,
    )
    private val LEADING_FILLER = Regex("""^(?:of|a|an|the)\s+""", RegexOption.IGNORE_CASE)
    private val TRAIL_PATTERNS = listOf(
        """\s+to\s+serve$""", """\s+for\s+serving$""", """\s+to\s+taste$""",
        """\s+as\s+needed$""", """\s+optional$""", """\s+for\s+garnish$""", """\s+garnish$""",
        """\s+roughly\s+chopped$""", """\s+finely\s+chopped$""", """\s+chopped$""",
        """\s+diced$""", """\s+sliced$""", """\s+grated$""", """\s+minced$""", """\s+crushed$""",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    /**
     * Strips quantity, unit, and preparation notes from a raw ingredient string,
     * returning a clean name suitable for a pantry or shopping list entry.
     * e.g. "400g chicken thighs, sliced" -> "Chicken thighs"
     */
    fun cleanName(raw: String): String {
        var name = raw.trim()

        while (PAREN_NOTE.containsMatchIn(name)) name = PAREN_NOTE.replace(name, "")
        name = name.trim()

        val unitMatch = UNIT_PATTERN.find(name)
        name = if (unitMatch != null) {
            val rest = name.substring(unitMatch.range.last + 1).trim()
            SECOND_MEASURE.find(rest)?.let { rest.substring(it.range.last + 1).trim() } ?: rest
        } else {
            val plainMatch = PLAIN_NUM.find(name)
            if (plainMatch != null) name.substring(plainMatch.range.last + 1).trim() else name
        }

        MEASURE_OF.find(name)?.groupValues?.getOrNull(1)?.let { name = it.trim() }

        LEADING_FILLER.find(name)?.let { name = name.substring(it.range.last + 1).trim() }

        val commaIdx = name.indexOf(',')
        if (commaIdx >= 0) name = name.substring(0, commaIdx).trim()

        for (pattern in TRAIL_PATTERNS) {
            val m = pattern.find(name)
            if (m != null) name = name.substring(0, m.range.first).trim()
        }

        // Safety net for malformed notes: an unmatched "(" means the note was cut
        // off, so drop it; a stray ")" is just removed.
        name.indexOf('(').takeIf { it >= 0 }?.let { name = name.substring(0, it) }
        name = name.replace(")", "").trim()

        name = normalizeCasing(name)
        return name.ifEmpty { raw }
    }

    /**
     * Normalises display casing. ALL-CAPS names (from receipt OCR / barcode
     * lookups) become Title Case; otherwise just the first letter is capitalised
     * so intentional mixed-case names (e.g. "iScream") are preserved.
     */
    fun normalizeCasing(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return raw
        return if (trimmed.none { it.isLowerCase() }) {
            trimmed.split(" ").joinToString(" ") { w ->
                w.lowercase().replaceFirstChar { c -> c.uppercase() }
            }
        } else {
            trimmed.replaceFirstChar { c -> c.uppercase() }
        }
    }

    private val MIXED_RE = Regex("""^(\d+)\s+(\d+)\s*/\s*(\d+)(.*)""", RegexOption.DOT_MATCHES_ALL)
    private val FRAC_RE  = Regex("""^(\d+)\s*/\s*(\d+)(.*)""", RegexOption.DOT_MATCHES_ALL)
    private val NUM_RE   = Regex("""^(\d+(?:\.\d+)?)(.*)""", RegexOption.DOT_MATCHES_ALL)

    /** Scales the leading quantity in an ingredient string. e.g. scale("400g chicken", 0.5) -> "200g chicken" */
    fun scale(ingredient: String, factor: Double): String {
        if (abs(factor - 1.0) <= 0.001 || factor <= 0) return ingredient
        val s = ingredient.trim()

        MIXED_RE.find(s)?.let { m ->
            val whole = m.groupValues[1].toDoubleOrNull()
            val num   = m.groupValues[2].toDoubleOrNull()
            val den   = m.groupValues[3].toDoubleOrNull()
            if (whole != null && num != null && den != null && den != 0.0) {
                return format((whole + num / den) * factor) + m.groupValues[4]
            }
        }

        FRAC_RE.find(s)?.let { m ->
            val num = m.groupValues[1].toDoubleOrNull()
            val den = m.groupValues[2].toDoubleOrNull()
            if (num != null && den != null && den != 0.0) {
                return format((num / den) * factor) + m.groupValues[3]
            }
        }

        NUM_RE.find(s)?.let { m ->
            val value = m.groupValues[1].toDoubleOrNull()
            if (value != null) return format(value * factor) + m.groupValues[2]
        }

        return s
    }

    private val FRACTIONS = listOf(
        0.125 to "⅛", 0.25 to "¼", 0.333 to "⅓", 0.375 to "⅜",
        0.5 to "½", 0.625 to "⅝", 0.667 to "⅔", 0.75 to "¾", 0.875 to "⅞",
    )

    private fun format(value: Double): String {
        val whole = value.toInt()
        val frac  = value - whole

        if (whole == 0) {
            FRACTIONS.firstOrNull { abs(frac - it.first) < 0.07 }?.let { return it.second }
        }
        if (whole > 0 && frac > 0.07) {
            FRACTIONS.firstOrNull { abs(frac - it.first) < 0.07 }?.let { return "$whole${it.second}" }
        }
        if (abs(frac) < 0.07) return "$whole"
        if (value >= 10) return "${value.roundToInt()}"
        return "%.1f".format(value)
    }
}
