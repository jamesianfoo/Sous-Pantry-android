package com.souspantry.app.ui.ereceipt

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.ApiService
import com.souspantry.app.services.DirectClaude
import com.souspantry.app.ui.funnel.REGION_SHOPS
import com.souspantry.app.ui.pantry.CATEGORY_ORDER
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val GENERIC_HINT    = "Log in and open a receipt, then tap Sync."
private const val WOOLWORTHS_HINT = "Sign in, tap the latest transaction date, then tap the eReceipt tab — then tap Sync."
private const val COLES_HINT      = "Sign in, tap View Order on your latest transaction — then tap Sync."

/** A store whose receipts are read from its own website. Auto-sync/geofence is out of scope. */
data class Store(
    val id        : String,
    val name      : String,
    val receiptUrl: String,
    val isBuiltIn : Boolean = true,
    /** Backing "name|url" entry for custom stores, so it can be removed. */
    val customKey : String? = null,
    /** Shown inside the login WebView. */
    val hint      : String  = GENERIC_HINT,
)

/** Slug convention shared with the wizard: lowercase, spaces→_, apostrophes/dots removed. */
private fun store(name: String, url: String) =
    Store(name.lowercase().replace("'", "").replace(".", "").replace(" ", "_"), name, url)

/**
 * Built-in store cards per wizard region (mirrors iOS SupermarketView.swift).
 * Every store runs the same generic sync — no store-specific parsing.
 */
val REGION_STORES: Map<String, List<Store>> = mapOf(
    // AU ids predate the slug convention; kept so existing connections survive.
    "AU" to listOf(
        Store("woolworths",    "Woolworths",     "https://www.everyday.com.au/index.html#/my-activity", hint = WOOLWORTHS_HINT),
        Store("coles_instore", "Coles In-Store", "https://www.coles.com.au/account/orders?status=instore", hint = COLES_HINT),
        Store("coles_online",  "Coles Online",   "https://www.coles.com.au/account/orders?status=past", hint = COLES_HINT),
        store("ALDI", "https://www.aldi.com.au/"),
        store("IGA",  "https://www.iga.com.au/"),
    ),
    "GB" to listOf(
        store("Tesco",       "https://www.tesco.com/account/auth/en-GB/login"),
        store("Sainsbury's", "https://www.sainsburys.co.uk/gol-ui/account/orders"),
        store("Asda",        "https://groceries.asda.com/account/orders"),
        store("Morrisons",   "https://groceries.morrisons.com/my-account/orders"),
        store("Aldi",        "https://www.aldi.co.uk/"),
        store("Lidl",        "https://www.lidl.co.uk/"),
    ),
    "EU" to listOf(
        store("Lidl",      "https://www.lidl.de/"),
        store("Aldi",      "https://www.aldi.de/"),
        store("Carrefour", "https://www.carrefour.fr/mon-compte/commandes"),
        store("REWE",      "https://www.rewe.de/"),
        store("Auchan",    "https://www.auchan.fr/"),
    ),
    "US" to listOf(
        store("Walmart",      "https://www.walmart.com/orders"),
        store("Kroger",       "https://www.kroger.com/mypurchases"),
        store("Costco",       "https://www.costco.com/OrderStatusCmd"),
        store("Trader Joe's", "https://www.traderjoes.com/"),
        store("Whole Foods",  "https://www.amazon.com/gp/css/order-history"),
        store("Safeway",      "https://www.safeway.com/account/orders.html"),
    ),
    "CA" to listOf(
        store("Loblaws",   "https://www.loblaws.ca/orders"),
        store("No Frills", "https://www.nofrills.ca/orders"),
        // Not /my-account — that routes to a Cloudflare-blocked domain.
        store("Metro",     "https://www.metro.ca/en"),
        store("Sobeys",    "https://www.sobeys.com/"),
        store("Costco",    "https://www.costco.ca/OrderStatusCmd"),
        store("Walmart",   "https://www.walmart.ca/orders"),
    ),
    "NZ" to listOf(
        store("Woolworths",  "https://www.woolworths.co.nz/shop/myaccount/orders"),
        store("New World",   "https://www.newworld.co.nz/"),
        store("Pak'nSave",   "https://www.paknsave.co.nz/"),
        store("Four Square", "https://www.foursquare.co.nz/"),
    ),
    "MY" to listOf(
        store("Lotus's",        "https://www.lotuss.com.my/"),
        store("Giant",          "https://giant.com.my/"),
        store("AEON",           "https://www.aeon.com.my/"),
        store("Mydin",          "https://www.mydin.com.my/"),
        store("Jaya Grocer",    "https://jayagrocer.com/account/orders"),
        store("Village Grocer", "https://villagegrocer.com.my/"),
    ),
    "SG" to listOf(
        store("FairPrice",    "https://www.fairprice.com.sg/account/orders"),
        store("Cold Storage", "https://coldstorage.com.sg/account/orders"),
        store("Sheng Siong",  "https://shengsiong.com.sg/"),
        store("Giant",        "https://giant.sg/"),
    ),
    "TH" to listOf(
        store("Lotus's",      "https://www.lotuss.com/en/"),
        store("Big C",        "https://www.bigc.co.th/en"),
        store("Tops",         "https://www.tops.co.th/en"),
        store("Makro",        "https://www.makro.pro/th/en"),
        store("Villa Market", "https://www.villamarket.com/"),
    ),
)

/**
 * Users who finished setup before the wizard existed have no region — the app was
 * Australia-only then. Unlisted countries (OTHER) get no built-ins: they add their
 * store's URL themselves and use the identical pipeline.
 */
private fun regionOrDefault(region: String) = region.ifBlank { "AU" }

sealed interface SyncResult {
    data class Success(val count: Int) : SyncResult
    data class Error(val message: String) : SyncResult
}

data class EReceiptState(
    /** Stores shown on the main screen — connected built-ins + custom stores. */
    val stores      : List<Store>            = emptyList(),
    /** The region's built-in stores not yet connected — pills in the Add Store sheet. */
    val available   : List<Store>            = emptyList(),
    val lastSync    : Map<String, Int>       = emptyMap(),   // storeId → units added
    val syncing     : Boolean                = false,
    val result      : SyncResult?            = null,
)

@HiltViewModel
class EReceiptViewModel @Inject constructor(
    private val api    : ApiService,
    private val claude : DirectClaude,
    private val pantry : PantryRepository,
    private val prefs  : UserPreferencesRepository,
) : ViewModel() {

    private val _transient = MutableStateFlow(EReceiptState())

    val state: StateFlow<EReceiptState> =
        combine(prefs.funnelRegion, prefs.customStores, prefs.connectedStores, _transient) { region, custom, connected, t ->
            val builtIns = REGION_STORES[regionOrDefault(region)].orEmpty()
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
            t.copy(
                stores    = builtIns.filter { it.id in connected } + customStores,
                available = builtIns.filter { it.id !in connected },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EReceiptState())

    // ── Stores ──────────────────────────────────────────────────────────────

    /** Adds a built-in "popular" store to the main list. */
    fun connectStore(store: Store) = viewModelScope.launch {
        prefs.addConnectedStore(store.id)
    }

    fun addCustomStore(name: String, url: String) = viewModelScope.launch {
        val normalised = if (url.startsWith("http")) url else "https://$url"
        prefs.addCustomStore(name, normalised)
    }

    /** Removes a store from the main list — disconnects built-ins, deletes custom stores. */
    fun removeStore(store: Store) = viewModelScope.launch {
        if (store.isBuiltIn) prefs.removeConnectedStore(store.id)
        else store.customKey?.let { prefs.removeCustomStore(it) }
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    /**
     * One pipeline for every store: the page text (read from the WebView after the
     * expand script) is parsed by the model through the Worker proxy, then added to
     * the pantry. Mirrors iOS syncReceipt — minus price history, which Android
     * doesn't have yet.
     */
    fun syncFromPageText(store: Store, pageText: String) = viewModelScope.launch {
        if (pageText.isBlank()) {
            _transient.update { it.copy(result = SyncResult.Error("Couldn't read the page. Make sure you're on a receipt page.")) }
            return@launch
        }
        _transient.update { it.copy(syncing = true, result = null) }
        runCatching {
            if (claude.enabled) {
                val region = regionOrDefault(prefs.funnelRegion.first())
                val knownStores = REGION_SHOPS[region].orEmpty().map { it.label }
                claude.parseReceiptText(pageText, knownStores, CATEGORY_ORDER).items
            } else {
                api.parseReceiptText(mapOf("text" to pageText))
            }
        }
            .onSuccess { items ->
                if (items.isEmpty()) {
                    _transient.update { it.copy(syncing = false, result = SyncResult.Error("No grocery items found. Navigate to a receipt page first.")) }
                } else {
                    pantry.addAll(items.map {
                        PantryItem(name = it.name, category = it.category, quantity = it.count.coerceAtLeast(1), notes = it.quantity)
                    })
                    // "2 x 500g" counts as 2, so the banner matches the receipt.
                    val units = items.sumOf { it.count.coerceAtLeast(1) }
                    _transient.update {
                        it.copy(
                            syncing  = false,
                            result   = SyncResult.Success(units),
                            lastSync = it.lastSync + (store.id to units),
                        )
                    }
                }
            }
            .onFailure { e ->
                Log.e("EReceipt", "Receipt sync failed for ${store.name}", e)
                _transient.update { it.copy(syncing = false, result = SyncResult.Error("Couldn't parse the receipt. Try opening a specific order.")) }
            }
    }

    fun clearResult() = _transient.update { it.copy(result = null) }
}
