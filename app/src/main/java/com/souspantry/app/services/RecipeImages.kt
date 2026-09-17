package com.souspantry.app.services

import com.google.gson.JsonParser
import com.souspantry.app.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Recipe photos, sharing iOS's image store (`FoodImageService.swift` +
 * `ImageWorkerService.swift`).
 *
 * ACCURACY RULE: a dish shows either its generated image from the Worker or an
 * honestly generic cuisine photo — never a photo searched by dish name, which
 * produced lasagna for "Tom Yum". Don't add dish-name image lookups here.
 */
object RecipeImages {

    const val WORKER = "https://souspantry-images.souspantry.workers.dev"

    // Bare client: the app secret is attached only to our own Worker POSTs,
    // never sent to Unsplash.
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Lowercased title → resolved URL. A Worker URL is final; anything else is provisional. */
    private val cache = ConcurrentHashMap<String, String>()

    /** Slugs already sent to /generate this session. The Worker is idempotent too. */
    private val requested = ConcurrentHashMap.newKeySet<String>()

    /** MUST match the Worker's `slugKey()` and iOS `ImageWorkerService.slug`. */
    fun slug(name: String): String {
        val collapsed = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(80)
        return collapsed.ifEmpty { "recipe" }
    }

    /** djb2 over the lowercased title, as iOS: stable across launches. */
    fun stableHash(s: String): Long {
        var h = 5381L
        s.lowercase().codePoints().forEach { h = (h shl 5) + h + it }
        return abs(h)
    }

    fun isWorkerUrl(url: String?) = url?.startsWith(WORKER) == true

    /** Best URL for a dish right now, or null (caller keeps its gradient placeholder). */
    suspend fun imageUrl(title: String, cuisine: String): String? = withContext(Dispatchers.IO) {
        val key = title.lowercase()
        cache[key]?.takeIf { isWorkerUrl(it) }?.let { return@withContext it }

        val generated = "$WORKER/img/${slug(title)}.webp"
        if (exists(generated)) {
            cache[key] = generated
            return@withContext generated
        }
        // A cached fallback is still valid — we only re-checked the Worker above
        // so the accurate image takes over as soon as generation lands.
        cache[key]?.let { return@withContext it }

        requestGeneration(title, cuisine)

        val variant = stableHash(title)
        val fallback = cuisine.takeIf { it.isNotBlank() }?.let { unsplash("$it food dish", variant) }
            ?: unsplash("plated food dish", variant)
        fallback?.also { cache[key] = it }
    }

    /**
     * Called when suggestions arrive, before anything is tapped: makes sure
     * generation is under way so the accurate photo exists by the time the
     * recipe is saved or opened.
     */
    fun warmUp(title: String, cuisine: String) {
        if (BuildConfig.SP_PROXY_SECRET.isBlank() || !requested.add(slug(title))) return
        scope.launch {
            if (!exists("$WORKER/img/${slug(title)}.webp")) generate(title, cuisine)
        }
    }

    private fun requestGeneration(title: String, cuisine: String) {
        if (BuildConfig.SP_PROXY_SECRET.isBlank() || !requested.add(slug(title))) return
        scope.launch { generate(title, cuisine) }
    }

    private fun generate(title: String, cuisine: String) {
        val body = buildMap {
            put("title", title)
            if (cuisine.isNotBlank()) put("cuisine", cuisine)
        }
        val req = Request.Builder()
            .url("$WORKER/generate")
            .header("Authorization", "Bearer ${BuildConfig.SP_PROXY_SECRET}")
            .post(com.google.gson.Gson().toJson(body).toRequestBody("application/json".toMediaType()))
            .build()
        runCatching { http.newCall(req).execute().close() }
    }

    // The Worker only routes GET for /img/ (a HEAD isn't matched), so probe with
    // GET and close without reading the body.
    private fun exists(url: String): Boolean = runCatching {
        http.newCall(Request.Builder().url(url).get().build()).execute().use { it.code == 200 }
    }.getOrDefault(false)

    private fun unsplash(query: String, variant: Long): String? {
        val key = BuildConfig.UNSPLASH_ACCESS_KEY
        if (key.isBlank()) return null
        val url = "https://api.unsplash.com/search/photos?query=" + URLEncoder.encode(query, "UTF-8") +
            "&per_page=10&orientation=landscape&content_filter=high"
        return runCatching {
            http.newCall(Request.Builder().url(url).header("Authorization", "Client-ID $key").build())
                .execute().use { resp ->
                    if (!resp.isSuccessful) return@use null
                    val results = JsonParser.parseString(resp.body?.string().orEmpty())
                        .asJsonObject.getAsJsonArray("results")
                    val urls = results.mapNotNull {
                        it.asJsonObject.getAsJsonObject("urls")?.get("regular")?.asString
                    }
                    // Rotate by title so two dishes sharing a cuisine don't show the same photo.
                    urls.takeIf { it.isNotEmpty() }?.let { it[Math.floorMod(variant, it.size.toLong()).toInt()] }
                }
        }.getOrNull()
    }
}
