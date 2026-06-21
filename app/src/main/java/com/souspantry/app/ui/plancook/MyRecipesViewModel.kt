package com.souspantry.app.ui.plancook

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.souspantry.app.data.repository.MyRecipeRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

/** Transient (non-persisted) UI flags layered on top of the persisted recipe list. */
private data class TransientState(
    val scanning     : Boolean   = false,
    val scanError    : String?   = null,
    val toastVisible : Boolean   = false,
    val scannedDraft : MyRecipe? = null,
)

@HiltViewModel
class MyRecipesViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : MyRecipeRepository,
) : ViewModel() {

    private val _transient = MutableStateFlow(TransientState())

    val state: StateFlow<MyRecipesState> = combine(repo.recipes, _transient) { recipes, t ->
        MyRecipesState(
            recipes      = recipes,
            scanning     = t.scanning,
            scanError    = t.scanError,
            toastVisible = t.toastVisible,
            scannedDraft = t.scannedDraft,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyRecipesState())

    // ── CRUD ─────────────────────────────────────────────────────────────────

    fun save(recipe: MyRecipe) {
        viewModelScope.launch { repo.upsert(recipe) }
        showToast()
    }

    fun duplicate(recipe: MyRecipe) = viewModelScope.launch {
        repo.upsert(
            recipe.copy(
                id        = java.util.UUID.randomUUID().toString(),
                title     = "Copy of ${recipe.title}",
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    fun remove(id: String) = viewModelScope.launch { repo.delete(id) }

    // ── Scan (OCR on-device → backend parse) ─────────────────────────────────

    fun scanFromBitmap(bitmap: Bitmap) = viewModelScope.launch {
        _transient.update { it.copy(scanning = true, scanError = null) }
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
                _transient.update { it.copy(scanning = false, scannedDraft = draft) }
            }
            .onFailure { e ->
                Log.e("RecipeScan", "Recipe scan failed", e)
                val msg = e.message?.takeIf { it.isNotBlank() && it.length < 200 }
                    ?: "Couldn't read that recipe. Try again."
                _transient.update { it.copy(scanning = false, scanError = msg) }
            }
    }

    fun consumeScannedDraft() = _transient.update { it.copy(scannedDraft = null) }
    fun clearScanError()      = _transient.update { it.copy(scanError = null) }

    private suspend fun recogniseText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it.text) }
                .addOnFailureListener  { cont.resumeWithException(it) }
        }

    // ── Toast ────────────────────────────────────────────────────────────────

    private fun showToast() {
        _transient.update { it.copy(toastVisible = true) }
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _transient.update { it.copy(toastVisible = false) }
        }
    }
}
