package com.souspantry.app.ui.ereceipt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** How a store's sync is triggered. Auto-sync/geofence is out of scope. */
data class Store(
    val id        : String,
    val name      : String,
    val receiptUrl: String,
    val isBuiltIn : Boolean = true,
    /** Backing "name|url" entry for custom stores, so it can be removed. */
    val customKey : String? = null,
)

/** Built-in Australian supermarket eReceipt pages (mirrors iOS supermarkets list). */
val BUILT_IN_STORES = listOf(
    Store("woolworths",   "Woolworths",      "https://www.everyday.com.au/index.html#/my-activity"),
    Store("coles_instore","Coles In-Store",  "https://www.coles.com.au/account/orders?status=instore"),
    Store("coles_online", "Coles Online",    "https://www.coles.com.au/account/orders?status=past"),
)

/** Per-store hint shown inside the login WebView. */
fun receiptInstruction(storeId: String): String = when (storeId) {
    "woolworths"                   -> "Sign in, tap the latest transaction date, then tap the eReceipt tab — then tap Sync."
    "coles_instore", "coles_online"-> "Sign in, tap View Order on your latest transaction — then tap Sync."
    else                           -> "Log in and open a receipt, then tap Sync."
}

sealed interface SyncResult {
    data class Success(val count: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

data class EReceiptState(
    val stores      : List<Store>            = BUILT_IN_STORES,
    val lastSync    : Map<String, Int>       = emptyMap(),   // storeId → item count
    val syncing     : Boolean                = false,
    val result      : SyncResult?            = null,
)

@HiltViewModel
class EReceiptViewModel @Inject constructor(
    private val api    : ApiService,
    private val pantry : PantryRepository,
    private val prefs  : UserPreferencesRepository,
) : ViewModel() {

    private val _transient = MutableStateFlow(EReceiptState())

    val state: StateFlow<EReceiptState> = combine(prefs.customStores, _transient) { custom, t ->
        val customStores = custom.mapNotNull { entry ->
            val parts = entry.split("|", limit = 2)
            if (parts.size == 2 && parts[1].isNotBlank())
                Store(
                    id         = "custom_${parts[0].hashCode()}",
                    name       = parts[0],
                    receiptUrl = parts[1],
                    isBuiltIn  = false,
                    customKey  = entry,
                )
            else null
        }
        t.copy(stores = BUILT_IN_STORES + customStores)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EReceiptState())

    // ── Custom stores ─────────────────────────────────────────────────────────

    fun addCustomStore(name: String, url: String) = viewModelScope.launch {
        val normalised = if (url.startsWith("http")) url else "https://$url"
        prefs.addCustomStore(name, normalised)
    }

    fun removeCustomStore(store: Store) = viewModelScope.launch {
        store.customKey?.let { prefs.removeCustomStore(it) }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * Parses the receipt page text (extracted from the WebView via JS) into
     * line items and adds them to the pantry. Mirrors iOS syncReceipt — minus
     * the price-history recording, which has no Android consumer yet.
     */
    fun syncFromPageText(store: Store, pageText: String) = viewModelScope.launch {
        if (pageText.isBlank()) {
            _transient.update { it.copy(result = SyncResult.Error("Couldn't read the page. Make sure you're on a receipt page.")) }
            return@launch
        }
        _transient.update { it.copy(syncing = true, result = null) }
        runCatching { api.parseReceiptText(mapOf("text" to pageText)) }
            .onSuccess { items ->
                if (items.isEmpty()) {
                    _transient.update { it.copy(syncing = false, result = SyncResult.Error("No grocery items found. Navigate to a receipt page first.")) }
                } else {
                    pantry.addAll(items.map { PantryItem(name = it.name, category = it.category, notes = it.quantity) })
                    _transient.update {
                        it.copy(
                            syncing  = false,
                            result   = SyncResult.Success(items.size),
                            lastSync = it.lastSync + (store.id to items.size),
                        )
                    }
                }
            }
            .onFailure {
                _transient.update { it.copy(syncing = false, result = SyncResult.Error("Couldn't parse the receipt. Try opening a specific order.")) }
            }
    }

    fun clearResult() = _transient.update { it.copy(result = null) }
}
