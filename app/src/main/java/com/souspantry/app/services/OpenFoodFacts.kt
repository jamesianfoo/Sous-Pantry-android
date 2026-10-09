package com.souspantry.app.services

import com.google.gson.JsonParser
import com.souspantry.app.data.models.BarcodeResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Open Food Facts has no product for this barcode (distinct from a network failure). */
class ProductNotFoundException : IOException("Product not found")

/**
 * Barcode → product straight from Open Food Facts: no API key, no backend.
 * Mirrors iOS `OpenFoodFactsService`: prefer the English name OFF has, and
 * when the name still looks foreign ask the model (via the Worker) for the
 * common English grocery name, keeping the original on any failure.
 */
@Singleton
class OpenFoodFacts @Inject constructor(private val claude: DirectClaude) {

    // Bare client: must NOT reuse the AI-proxy client, or the app secret
    // would be sent to a third party.
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun lookup(barcode: String): BarcodeResult {
        val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
            "?fields=product_name,product_name_en,generic_name_en,generic_name,brands,categories,image_url,quantity" +
            "&lc=en"
        val body = withContext(Dispatchers.IO) {
            val req = Request.Builder().url(url).header("User-Agent", "SousPantry Android").build()
            http.newCall(req).execute().use { r ->
                when {
                    r.code == 404   -> throw ProductNotFoundException()
                    !r.isSuccessful -> throw IOException("Open Food Facts returned HTTP ${r.code}")
                    else            -> r.body?.string().orEmpty()
                }
            }
        }
        val product = parseOffProduct(body) ?: throw ProductNotFoundException()
        val name = product.name ?: return product
        if (!claude.enabled || !looksNonEnglish(name)) return product
        val cleaned = runCatching { claude.cleanProductName(name, product.brand) }.getOrNull()
        return if (cleaned.isNullOrBlank()) product else product.copy(name = cleaned)
    }
}

/**
 * OFF v2 product JSON → [BarcodeResult]. Null when `status != 1`, there is no
 * product, or no usable name. OFF is crowdsourced and `product_name` is in
 * whatever language the contributor typed, so the English fields win.
 */
internal fun parseOffProduct(json: String): BarcodeResult? {
    val root = runCatching { JsonParser.parseString(json) }.getOrNull()
        ?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    if (root.get("status")?.takeIf { it.isJsonPrimitive }?.asString?.toIntOrNull() != 1) return null
    val p = root.get("product")?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    fun field(key: String): String? =
        p.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }

    val name = field("product_name_en") ?: field("generic_name_en")
        ?: field("product_name") ?: field("generic_name") ?: return null
    return BarcodeResult(
        name     = name,
        brand    = field("brands")?.substringBefore(",")?.trim()?.takeIf { it.isNotEmpty() },
        category = Categories.normalizeCategory(field("categories")?.substringBefore(",")?.trim()),
        quantity = field("quantity"),
        imageUrl = field("image_url"),
    )
}

private val NON_ENGLISH_MARKERS = listOf(
    "sirop", "fromage", "lait", "poulet", "pomme", "jus ", "huile",
    "leche", "queso", "aceite", "zumo", "milch", "käse", "saft",
    " de ", " au ", " aux ", " con ", " mit ",
)

/** Heuristic from iOS: non-ASCII letters or common non-English food words. */
internal fun looksNonEnglish(s: String): Boolean {
    if (s.any { it.code > 127 }) return true
    val l = " ${s.lowercase()} "
    return NON_ENGLISH_MARKERS.any { it in l }
}
