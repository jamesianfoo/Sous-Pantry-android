package com.souspantry.app.ui.pantry

import android.graphics.Bitmap
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

sealed interface ReceiptScanState {
    data object Ready                                            : ReceiptScanState
    data object Loading                                          : ReceiptScanState
    data class  Results(val items: List<ReceiptLineItem>)        : ReceiptScanState
    data class  Error(val message: String)                       : ReceiptScanState
    data object Saved                                            : ReceiptScanState
}

@HiltViewModel
class ReceiptScanViewModel @Inject constructor(
    private val api  : ApiService,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ReceiptScanState>(ReceiptScanState.Ready)
    val state = _state.asStateFlow()

    fun processImage(bitmap: Bitmap) = viewModelScope.launch {
        _state.value = ReceiptScanState.Loading
        val baos   = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
        runCatching { api.parseReceiptImage(mapOf("image" to base64)) }
            .onSuccess { _state.value = ReceiptScanState.Results(it) }
            .onFailure { _state.value = ReceiptScanState.Error("Couldn't read receipt. Try again.") }
    }

    fun saveAll(items: List<ReceiptLineItem>) = viewModelScope.launch {
        repo.addAll(items.map { PantryItem(name = it.name, category = it.category) })
        _state.value = ReceiptScanState.Saved
    }

    fun retry() { _state.value = ReceiptScanState.Ready }
}
