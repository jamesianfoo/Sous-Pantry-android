package com.souspantry.app.ui.plancook

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MyRecipesState(
    val recipes      : List<MyRecipe> = emptyList(),
    val scanning     : Boolean        = false,   // OCR + parse in flight
    val scanError    : String?        = null,
    val toastVisible : Boolean        = false,   // "Saved to My Recipes ✓"
    /** When non-null, a freshly scanned recipe to open in the editor (create mode). */
    val scannedDraft : MyRecipe?      = null,
)

@HiltViewModel
class MyRecipesViewModel @Inject constructor(
    private val api : ApiService,
) : ViewModel() {

    private val _state = MutableStateFlow(MyRecipesState())
    val state = _state.asStateFlow()

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun save(recipe: MyRecipe) {
        _state.update { s ->
            val existing = s.recipes.indexOfFirst { it.id == recipe.id }
            val updated  = if (existing >= 0)
                s.recipes.toMutableList().apply { set(existing, recipe) }
            else
                s.recipes + recipe
            s.copy(recipes = updated)
        }
        showToast()
    }

    fun duplicate(recipe: MyRecipe) {
        val copy = recipe.copy(
            id        = java.util.UUID.randomUUID().toString(),
            title     = "Copy of ${recipe.title}",
            createdAt = System.currentTimeMillis(),
        )
        _state.update { it.copy(recipes = listOf(copy) + it.recipes) }
    }

    fun remove(id: String) =
        _state.update { it.copy(recipes = it.recipes.filterNot { r -> r.id == id }) }

    // ── Scan (OCR on-device → backend parse) ─────────────────────────────────

    fun scanFromBitmap(bitmap: Bitmap) = viewModelScope.launch {
        _state.update { it.copy(scanning = true, scanError = null) }
        runCatching {
            val text = recogniseText(bitmap)
            Log.d("RecipeScan", "OCR extracted ${text.length} chars")
            if (text.isBlank()) error("No text detected. Make sure the recipe is well-lit and in focus.")
            api.scanRecipeText(mapOf("text" to text))
        }
            .onSuccess { parsed ->
                val draft = MyRecipe(
                    title        = parsed.name,
                    description  = parsed.description.orEmpty(),
                    cuisine      = parsed.cuisine.orEmpty(),
                    prepTime     = parsed.prepTime.orEmpty(),
                    cookTime     = parsed.cookTime.orEmpty(),
                    difficulty   = parsed.difficulty.orEmpty().ifBlank { "Medium" },
                    ingredients  = parsed.ingredients,
                    instructions = parsed.steps,
                    servings     = parsed.servings ?: 2,
                    source       = MyRecipeSource.PHOTO_SCAN,
                )
                _state.update { it.copy(scanning = false, scannedDraft = draft) }
            }
            .onFailure { e ->
                Log.e("RecipeScan", "Recipe scan failed", e)
                val msg = e.message?.takeIf { it.isNotBlank() && it.length < 200 }
                    ?: "Couldn't read that recipe. Try again."
                _state.update { it.copy(scanning = false, scanError = msg) }
            }
    }

    fun consumeScannedDraft() = _state.update { it.copy(scannedDraft = null) }
    fun clearScanError()      = _state.update { it.copy(scanError = null) }

    private suspend fun recogniseText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener  { cont.resumeWithException(it) }
        }

    // ── Toast ────────────────────────────────────────────────────────────────

    private fun showToast() {
        _state.update { it.copy(toastVisible = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _state.update { it.copy(toastVisible = false) }
        }
    }
}
