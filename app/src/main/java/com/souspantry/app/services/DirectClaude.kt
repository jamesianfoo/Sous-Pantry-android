package com.souspantry.app.services

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.souspantry.app.BuildConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.data.models.SuggestedMeal
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

// ── Anthropic Messages API, via the Sous Pantry Worker proxy ────────────────

interface AnthropicService {
    /** Worker path; it forwards to the provider and adds the provider key. */
    @POST("anthropic")
    suspend fun messages(@Body body: AnthropicRequest): AnthropicResponse
}

data class AnthropicRequest(
    val model      : String = "claude-haiku-4-5-20251001",
    val max_tokens : Int    = 2000,
    val messages   : List<AnthropicMessage>,
)

data class AnthropicMessage(val role: String, val content: String)

data class AnthropicResponse(val content: List<AnthropicContentBlock> = emptyList())

data class AnthropicContentBlock(val text: String? = null)

/** A parsed receipt: the store the model detected (if any) and its grocery lines. */
data class ParsedReceipt(val storeName: String?, val items: List<ReceiptLineItem>)

/**
 * Sous AI model calls, routed through the Cloudflare Worker proxy — no
 * provider key ever ships in the APK. Works with no local backend running.
 *
 * Prompts mirror backend/src/routes/meals.js (/chat) and shopping.js
 * (/generate, /staples) so both paths return the same shapes.
 */
@Singleton
class DirectClaude @Inject constructor(
    private val api: AnthropicService,
) {
    /** False only if the app secret wasn't configured; callers fall back to the backend. */
    val enabled: Boolean get() = BuildConfig.SP_PROXY_SECRET.isNotBlank()

    private val gson = Gson()

    private suspend fun call(prompt: String, maxTokens: Int = 2000): String =
        api.messages(
            AnthropicRequest(max_tokens = maxTokens, messages = listOf(AnthropicMessage("user", prompt)))
        ).content.firstOrNull()?.text.orEmpty()

    private inline fun <reified T> parseJsonArray(raw: String): List<T> {
        val clean = raw
            .replace(Regex("^```[a-zA-Z]*\\n?"), "")
            .replace(Regex("```\\s*$"), "")
            .trim()
        return gson.fromJson(clean, TypeToken.getParameterized(List::class.java, T::class.java).type)
    }

    // ── Discover chat ────────────────────────────────────────────────────────

    suspend fun chatMeals(
        pantryItems    : List<PantryItem>,
        userMessage    : String?,
        moods          : Set<String>,
        cuisines       : Set<String>,
        dietaryContext : String = "",
    ): List<SuggestedMeal> {
        val list        = if (pantryItems.isEmpty()) "Empty pantry"
                          else pantryItems.joinToString("\n") { "- ${it.name}" }
        val moodLine    = if (moods.isNotEmpty())    "\nMood: ${moods.joinToString(", ")}" else ""
        val cuisineLine = if (cuisines.isNotEmpty()) "\nPreferred cuisines: ${cuisines.joinToString(", ")}" else ""
        val requestLine = userMessage?.takeIf { it.isNotBlank() }?.let { "\nUser request: $it" } ?: ""

        val dietLine = if (dietaryContext.isNotBlank()) "\n            $dietaryContext" else ""
        val prompt = """$dietLine
            You are Sous Pantry's AI chef for an Australian household. Suggest exactly 3 meals that best satisfy the user's request below, prioritising what's already in their pantry where it fits. Use real recipes inspired by recipetineats.com and gatherandfeast.com. All ingredients MUST include quantities. The "usedPantryItems" field must ONLY list exact pantry item names from the list below. Include 4-6 step-by-step cooking instructions per recipe. "prepTime" and "cookTime" are short strings like "15 mins". "servings" is an integer, how many people the recipe as written serves (usually 2-4). For "imageQuery", provide 2-3 keywords for a food photo.

            Count by ingredient line items, not by weight. Salt/pepper/water/oil that most homes have count as matched if common staples. matchPct = 100 ONLY if every non-staple ingredient is in the pantry. NEVER list basic staples the user is always assumed to have: salt, pepper, water, cooking/vegetable oil, and sugar. Treat these as available and exclude them from missingIngredients entirely.

            Pantry:
            $list$moodLine$cuisineLine$requestLine

            If the user's request contains a recipe URL, return exactly ONE recipe for THAT page and echo the URL verbatim in "sourceURL" with its bare domain in "sourceSite" — never invent a different link. Otherwise leave both empty.

            Return a JSON array only — no markdown:
            [{"title":"","description":"one sentence","cuisine":"","prepTime":"","cookTime":"","servings":2,"difficulty":"Easy|Medium|Hard","ingredients":["quantity + ingredient"],"usedPantryItems":["exact pantry item names"],"instructions":["Step 1: ..."],"imageQuery":"dish keywords","sourceURL":"","sourceSite":""}]
        """.trimIndent()

        return parseJsonArray(call(prompt))
    }

    // ── Receipts ─────────────────────────────────────────────────────────────

    /**
     * Turns receipt text into grocery lines — a paper receipt's OCR text or a
     * supermarket's receipt/order page. Store-agnostic by design (mirrors iOS
     * ReceiptScanService): no per-store parsing, any currency, any region.
     *
     * @param knownStores the user's region store names, so a detected store
     *   comes back with the app's spelling
     */
    suspend fun parseReceiptText(
        text        : String,
        knownStores : List<String>,
        categories  : List<String>,
    ): ParsedReceipt {
        val storeRule = if (knownStores.isEmpty())
            "- Return the store name as shown, or null if none is shown."
        else
            "- If it matches one of these (case-insensitive): ${knownStores.joinToString(", ") { "\"$it\"" }}, return exactly that spelling. Otherwise return the store name as shown, or null if none is shown."

        val prompt = """
            The following text comes from a grocery receipt: either OCR from a paper receipt or the text of a supermarket's online receipt / order page. Identify every food and grocery item purchased, and detect the store name.

            Rules:
            - Include all food, drinks, cleaning products, and household grocery items.
            - EXCLUDE from the item list: date, time, cashier name, subtotals, tax (GST/VAT/SST), loyalty/rewards points, payment method, bag fees, delivery or service fees, and any website navigation, account, or marketing text.
            - EXCLUDE all non-food/non-grocery products including: homewares, kitchenware, air fresheners, electrical items, batteries, light bulbs, pest control products, brooms, mops, cleaning tools, party supplies, stationery, pens, notebooks, gardening items, seeds, plant pots, clothing, shoes, accessories, toys, books, magazines, and any other non-pantry household goods.
            - Fix OCR artefacts in product names (e.g. "Wh0le M1lk" → "Whole Milk").
            - Expand abbreviations to full descriptive names (e.g. "ORG FF MILK 2L" → "Organic Full Fat Milk").
            - For quantity: capture the pack WEIGHT or VOLUME of a single unit if shown (e.g. "500g", "2L", "6pk") — otherwise null. This is the pack SIZE, NOT how many were bought.
            - For count: HOW MANY units of this line were purchased, as an integer. Receipts show this as a leading quantity, "2 x", a "QTY 2" column, or a multiplier before the price (e.g. "2 @ 5.00"). If no explicit count is shown, return 1. Never return 0 or null.
            - For price: the item's price in the receipt's local currency, as a plain number. Prefer the unit price if shown; otherwise line total ÷ count. Null if not visible or ambiguous.
            - For category, choose the single best match from: ${categories.joinToString(", ")}.

            Store detection:
            - Find the store name in the receipt header or page.
            $storeRule

            If the text is not a receipt or order (e.g. a store homepage or login page), return an empty items array.

            Receipt text:
            $text

            Return a JSON object only — no markdown, no explanation:
            {"storeName":"Store name or null","items":[{"name":"Full product name","quantity":"500g or null","count":1,"category":"Category name","price":4.99}]}
        """.trimIndent()

        val raw = call(prompt, maxTokens = 4000)
            .replace(Regex("^```[a-zA-Z]*\\n?"), "")
            .replace(Regex("```\\s*$"), "")
            .trim()
        return parseReceiptJson(raw)
    }

    // ── Shopping list ────────────────────────────────────────────────────────

    suspend fun shoppingRestock(pantryItems: List<PantryItem>): List<ShoppingItem> {
        val pantry = pantryItems.joinToString("\n") { "- ${it.name}" }
        val prompt = """
            You are Sous Pantry's AI chef for an Australian household. Based on the pantry below, suggest a practical weekly shopping list of items that are low, missing, or commonly needed alongside what's in stock. Aim for 10-15 items. Prioritise items that unlock the most meals.

            Pantry:
            $pantry

            Return a JSON array only — no markdown:
            [{"name":"item name","category":"grocery category","quantity":"e.g. 1 bunch or null","priority":"Essential|Nice to Have","reason":"one short reason"}]
        """.trimIndent()
        return parseJsonArray(call(prompt))
    }

    suspend fun shoppingStaples(): List<ShoppingItem> {
        val prompt = """
            You are Sous Pantry's AI chef. This user has an empty pantry. Generate a practical Australian household starter pack of 15-20 pantry staples — everyday essentials like eggs, flour, rice, pasta, canned tomatoes, olive oil, butter, onions, garlic, and common spices.

            Return a JSON array only — no markdown:
            [{"name":"item name","category":"grocery category","quantity":"e.g. 1 dozen or null","priority":"Essential|Nice to Have","reason":"one short reason"}]
        """.trimIndent()
        return parseJsonArray(call(prompt))
    }
}

/**
 * Accepts `{"storeName": …, "items": […]}` or a bare items array. Tolerates the
 * model writing "null" as a string and omitting count (treated as 1).
 */
internal fun parseReceiptJson(raw: String): ParsedReceipt {
    val root = JsonParser.parseString(raw)
    fun JsonObject.str(key: String): String? =
        get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()
            ?.takeIf { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }

    val (store, array) = when {
        root.isJsonObject -> root.asJsonObject.let { it.str("storeName") to it.getAsJsonArray("items") }
        root.isJsonArray  -> null to root.asJsonArray
        else              -> null to null
    }
    val items = (array ?: com.google.gson.JsonArray()).mapNotNull { el ->
        val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
        val name = o.str("name") ?: return@mapNotNull null
        ReceiptLineItem(
            name     = name,
            quantity = o.str("quantity"),
            category = o.str("category"),
            count    = o.str("count")?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 1,
            price    = o.str("price")?.toDoubleOrNull()?.takeIf { it > 0 },
        )
    }
    return ParsedReceipt(store, items)
}
