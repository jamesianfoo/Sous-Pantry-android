package com.souspantry.app.ui.pantry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.OpenFoodFacts
import com.souspantry.app.services.ProductNotFoundException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BarcodeScanState {
    data object Scanning                             : BarcodeScanState
    data object Loading                              : BarcodeScanState
    data class  Result(val item: PantryItem)         : BarcodeScanState
    data class  Error(val message: String)           : BarcodeScanState
    data object Saved                                : BarcodeScanState
}

@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val off  : OpenFoodFacts,
    private val repo : PantryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<BarcodeScanState>(BarcodeScanState.Scanning)
    val state = _state.asStateFlow()
    private var lastScanned = ""

    fun onBarcodeDetected(code: String) {
        if (code == lastScanned || _state.value is BarcodeScanState.Loading) return
        lastScanned = code
        viewModelScope.launch {
            _state.value = BarcodeScanState.Loading
            runCatching { off.lookup(code) }
                .onSuccess { r ->
                    _state.value = BarcodeScanState.Result(
                        PantryItem(name = r.name ?: "Unknown", brand = r.brand, category = r.category)
                    )
                }
                .onFailure { e ->
                    _state.value = BarcodeScanState.Error(
                        if (e is ProductNotFoundException) "Product not found. Add manually."
                        else "Couldn't reach Open Food Facts. Check your connection and try again."
                    )
                }
        }
    }

    fun saveItem(item: PantryItem) = viewModelScope.launch {
        repo.add(item)
        _state.value = BarcodeScanState.Saved
    }

    fun rescan() { lastScanned = ""; _state.value = BarcodeScanState.Scanning }
}
