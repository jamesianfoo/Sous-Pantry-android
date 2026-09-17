package com.souspantry.app.services

import android.util.Patterns
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What to open when the user taps "View Recipe" on an external card. */
sealed interface RecipeLink {
    /** Open this page. [ingredients] non-empty only when a scrape succeeded and matched. */
    data class Direct(val url: String, val ingredients: List<String> = emptyList()) : RecipeLink
    /** Last resort for hallucinated/dead links. */
    data class Search(val url: String) : RecipeLink
}

private data class Scrape(val title: String, val ingredients: List<String>)

/**
 * Decides what a recipe card's "View Recipe" opens — mirrors iOS
 * SousLandingView.resolveExternalSheetContext.
 *
 * Core principle: a URL the USER pasted is trusted outright and opened
 * directly, never replaced by a search. Google site-search is only ever a
 * fallback for AI-invented or dead links.
 */
@Singleton
class RecipeLinkResolver @Inject constructor() {

    // Bare client: must NOT reuse the AI-proxy client, or its Authorization
    // header would be sent to arbitrary third-party sites.
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Per-session, repeat taps are free. Synchronized: cards pre-scrape in parallel.
    private val scrapeCache = java.util.Collections.synchronizedMap(mutableMapOf<String, Scrape?>())

    companion object {
        private const val UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/124.0 Mobile Safari/537.36"

        /** Every http/https URL in a block of text (any domain, with or without surrounding words). */
        fun extractUrls(text: String): List<String> {
            val m = Patterns.WEB_URL.matcher(text)
            val out = mutableListOf<String>()
            while (m.find()) {
                val raw = m.group()
                if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) out += raw
            }
            return out
        }

        /** Lowercased host with any "www." stripped; "" if unparseable. */
        fun host(url: String): String = runCatching {
            java.net.URI(url).host.orEmpty().lowercase().removePrefix("www.")
        }.getOrDefault("")

        /** Subdomain-tolerant, both directions (iOS parity). */
        fun hostMatchesDomain(h: String, domain: String): Boolean {
            val a = h.lowercase().removePrefix("www.")
            val b = domain.lowercase().removePrefix("www.")
            if (a.isBlank() || b.isBlank()) return false
            return a == b || a.endsWith(".$b") || b.endsWith(a)
        }

        /** host + path, scheme/query/trailing-slash insensitive — for "did the user paste this exact page?". */
        fun normalize(url: String): String = runCatching {
            val u = java.net.URI(url)
            val h = u.host.orEmpty().lowercase().removePrefix("www.")
            val p = u.path.orEmpty().trimEnd('/')
            "$h$p"
        }.getOrDefault("")

        fun googleSiteSearch(domain: String, recipeName: String): String {
            val query = if (domain.isBlank()) recipeName else "site:$domain $recipeName"
            return "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
        }

        /** ≥60% of the recipe's ≥3-char tokens present in the page title. Tolerates "… Recipe | Site". */
        fun titleMatches(recipeName: String, pageTitle: String): Boolean {
            val tokens = recipeName.lowercase()
                .split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }
            if (tokens.isEmpty()) return false
            val title = pageTitle.lowercase()
            return tokens.count { title.contains(it) }.toDouble() / tokens.size >= 0.6
        }
    }

    /**
     * @param cardUrl     sourceURL from the AI card, if any
     * @param cardDomain  sourceSite from the AI card, if any
     * @param recipeName  card title, for title-match and the search fallback
     * @param userTexts   the user's chat messages, newest first (never assistant messages)
     */
    suspend fun resolve(
        cardUrl    : String?,
        cardDomain : String?,
        recipeName : String,
        userTexts  : List<String>,
    ): RecipeLink = withContext(Dispatchers.IO) {
        val pastedUrls = userTexts.flatMap { extractUrls(it) }

        // 1. The card's URL, 2. else a pasted link recovered by domain — but a
        //    link the user pasted on that domain always beats an AI-generated
        //    one, or a hallucinated sibling URL would send their own paste to
        //    a search. Any pasted link is better than none.
        val aiUrl = cardUrl?.takeIf { it.startsWith("http", true) }
        val domain = cardDomain?.takeIf { it.isNotBlank() } ?: aiUrl?.let { host(it) }
        val pastedOnDomain = domain?.let { d -> pastedUrls.firstOrNull { hostMatchesDomain(host(it), d) } }

        val direct = pastedOnDomain ?: aiUrl ?: pastedUrls.firstOrNull()
            ?: return@withContext RecipeLink.Search(googleSiteSearch(cardDomain.orEmpty(), recipeName))

        // 3. Title verification when a scrape succeeded → open with ingredients.
        val scrape = scrapeCached(direct)
        if (scrape != null && titleMatches(recipeName, scrape.title)) {
            return@withContext RecipeLink.Direct(direct, scrape.ingredients)
        }

        // 4. The user pasted this exact page → trust outright, NO probe.
        //    (Bot-blocking sites 403 a probe yet render fine in a browser.)
        val target = normalize(direct)
        if (pastedUrls.any { normalize(it) == target }) {
            return@withContext RecipeLink.Direct(direct)
        }

        // 5. Not user-pasted → probe. Anything but 404/410 counts as existing.
        if (exists(direct)) return@withContext RecipeLink.Direct(direct)

        // 6. Dead/hallucinated → Google site-search, the only search path.
        RecipeLink.Search(googleSiteSearch(cardDomain ?: host(direct), recipeName))
    }

    /** The page's schema.org/Recipe ingredient lines, cached per session; empty when it has none. */
    suspend fun scrapedIngredients(url: String): List<String> =
        withContext(Dispatchers.IO) { scrapeCached(url)?.ingredients.orEmpty() }

    private fun scrapeCached(url: String): Scrape? {
        if (scrapeCache.containsKey(url)) return scrapeCache[url]
        return runCatching { scrapeJsonLd(url) }.getOrNull().also { scrapeCache[url] = it }
    }

    /** Pull schema.org/Recipe JSON-LD (name + recipeIngredient) out of the page HTML. */
    private fun scrapeJsonLd(url: String): Scrape? {
        val req = Request.Builder().url(url).header("User-Agent", UA).build()
        val html = http.newCall(req).execute().use { r ->
            if (!r.isSuccessful) return null
            r.body?.string() ?: return null
        }
        val blocks = Regex(
            """<script[^>]+type=["']application/ld\+json["'][^>]*>(.*?)</script>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        ).findAll(html).map { it.groupValues[1] }

        for (raw in blocks) {
            val recipe = runCatching { findRecipe(JsonParser.parseString(raw)) }.getOrNull() ?: continue
            val name = recipe["name"]?.takeIf { it.isJsonPrimitive }?.asString.orEmpty()
            val ingredients = recipe["recipeIngredient"]?.takeIf { it.isJsonArray }
                ?.asJsonArray?.mapNotNull { e -> e.takeIf { it.isJsonPrimitive }?.asString }
                ?: emptyList()
            if (name.isNotBlank()) return Scrape(name, ingredients)
        }
        // Fall back to <title> so step 3 can still attempt a match.
        val title = Regex("""<title[^>]*>(.*?)</title>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(html)?.groupValues?.get(1)?.trim()
        return title?.takeIf { it.isNotBlank() }?.let { Scrape(it, emptyList()) }
    }

    /** JSON-LD may be an object, an array, or wrapped in @graph. */
    private fun findRecipe(el: JsonElement): JsonObject? = when {
        el.isJsonArray  -> el.asJsonArray.firstNotNullOfOrNull { findRecipe(it) }
        el.isJsonObject -> {
            val o = el.asJsonObject
            val type = o["@type"]
            val isRecipe = when {
                type == null         -> false
                type.isJsonArray     -> type.asJsonArray.any { it.isJsonPrimitive && it.asString.equals("Recipe", true) }
                type.isJsonPrimitive -> type.asString.equals("Recipe", true)
                else                 -> false
            }
            if (isRecipe) o else o["@graph"]?.let { findRecipe(it) }
        }
        else -> null
    }

    /** HEAD (falling back to GET); the page "exists" for any status except 404/410. */
    private fun exists(url: String): Boolean {
        fun status(method: String): Int? = runCatching {
            val b = Request.Builder().url(url).header("User-Agent", UA)
            if (method == "HEAD") b.head()
            http.newCall(b.build()).execute().use { it.code }
        }.getOrNull()

        val head = status("HEAD")
        val code = if (head == null || head == 405 || head == 501) status("GET") else head
        return code != null && code != 404 && code != 410
    }
}
