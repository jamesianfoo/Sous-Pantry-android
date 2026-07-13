package com.souspantry.app.ui.pantry

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed interface ReceiptScanState {
    data object Ready                                            : ReceiptScanState
    data object Loading                                          : ReceiptScanState
    data class  Results(val items: List<ReceiptLineItem>)        : ReceiptScanState
    data class  Error(val message: String)                       : ReceiptScanState
    data object Saved                                            : ReceiptScanState
}

/**
 * Mirrors the iOS receipt flow:
 *   1) On-device OCR with ML Kit Text Recognition (no upload — fast, free, offline-capable).
 *   2) Backend `/api/receipt/text` turns the OCR text into structured line items via Claude.
 *
 * Sending the raw image to `/api/receipt/image` fails on modern phone cameras —
 * a 12+ MP JPEG base64 string blows past Express body-parser and Claude vision
 * payload limits.
 */
@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Ready)
    val state = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) = viewModelScope.launch {
        _state.value = ReceiptScanState.Loading
        runCatching {
            val ocrText = recogniseText(bitmap)
            Log.d("ReceiptScan", "OCR extracted ${ocrText.length} chars")
            if (ocrText.isBlank()) {
                error("No text detected. Make sure the receipt is well-lit and in focus.")
            }
            api.parseReceiptText(mapOf("text" to ocrText))
        }
            .onSuccess { _state.value = ReceiptScanState.Results(it) }
            .onFailure { e ->
                Log.e("ReceiptScan", "Receipt parse failed", e)
                val msg = e.message
                    ?.takeIf { it.isNotBlank() && it.length < 200 }
                    ?: "Couldn't read receipt. Try again."
                _state.value = ReceiptScanState.Error(msg)
            }
    }

    private suspend fun recogniseText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val image      = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { e            -> cont.resumeWithException(e) }
        }

    fun saveAll(items: List<ReceiptLineItem>) = viewModelScope.launch {
        repo.addAll(items.map { PantryItem(name = it.name, category = it.category, notes = it.quantity) })
        _state.value = ReceiptScanState.Saved
    }

    fun retry() { _state.value = ReceiptScanState.Ready }
}
