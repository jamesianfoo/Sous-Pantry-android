package com.souspantry.app.ui.ereceipt

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun EReceiptSyncScreen(
    onBack : () -> Unit,
    vm     : EReceiptViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var webStore     by remember { mutableStateOf<Store?>(null) }
    var showAddStore by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp),
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("eReceipt Sync", style = MaterialTheme.typography.headlineLarge, color = Navy)
                    Text("Sync your shopping in one tap", style = MaterialTheme.typography.bodyMedium, color = Slate)
                }
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }

            // ── How it works ─────────────────────────────────────────────────
            Text(
                "HOW IT WORKS",
                color      = Slate,
                fontSize   = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Surface(
                modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape           = RoundedCornerShape(14.dp),
                color           = Color.White,
                shadowElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(vertical = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                        StepCircle("1"); Connector(); StepCircle("2"); Connector(); StepCircle("3")
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        StepLabel("Connect\nyour store")
                        StepLabel("Open your\nreceipt")
                        StepLabel("Tap Sync\nto import")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Store list ───────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                state.stores.forEach { store ->
                    StoreCard(
                        store       = store,
                        lastSync    = state.lastSync[store.id],
                        onConnect   = { webStore = store },
                        onRemove    = if (!store.isBuiltIn) ({ vm.removeCustomStore(store) }) else null,
                    )
                }

                // Add store
                Surface(
                    modifier        = Modifier.fillMaxWidth().clickable { showAddStore = true },
                    shape           = RoundedCornerShape(14.dp),
                    color           = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Row(
                        modifier          = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(Icons.Filled.AddCircle, null, tint = Green)
                        Column(Modifier.weight(1f)) {
                            Text("Add Store", color = Navy, fontWeight = FontWeight.SemiBold)
                            Text("Connect any store with digital receipts", color = Slate, fontSize = 12.sp)
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = Slate.copy(alpha = 0.4f))
                    }
                }
            }

            // ── Privacy footer ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Filled.Lock, null, tint = Slate.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                Text(
                    "Your login details are entered directly on the store's own site. Sous Pantry only reads the receipt text you choose to sync.",
                    color    = Slate.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                )
            }
        }
    }

    // ── WebView overlay ───────────────────────────────────────────────────────
    webStore?.let { store ->
        ReceiptWebView(
            store     = store,
            syncing   = state.syncing,
            result    = state.result,
            onSync    = { pageText -> vm.syncFromPageText(store, pageText) },
            onDismiss = { vm.clearResult(); webStore = null },
            onClearResult = { vm.clearResult() },
        )
    }

    // ── Add custom store sheet ────────────────────────────────────────────────
    if (showAddStore) {
        AddStoreSheet(
            onDismiss = { showAddStore = false },
            onSave    = { name, url -> vm.addCustomStore(name, url); showAddStore = false },
        )
    }
}

// ── How-it-works step helpers ─────────────────────────────────────────────────

@Composable
private fun RowScope.StepCircle(number: String) {
    Box(
        modifier         = Modifier.weight(1f),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier         = Modifier.size(24.dp).clip(CircleShape).background(Green),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Connector() {
    Box(modifier = Modifier.width(20.dp).height(2.dp).background(SoftMint))
}

@Composable
private fun RowScope.StepLabel(text: String) {
    Text(
        text,
        color     = Slate,
        fontSize  = 11.sp,
        textAlign = TextAlign.Center,
        modifier  = Modifier.weight(1f),
    )
}

// ── Store card ────────────────────────────────────────────────────────────────

@Composable
private fun StoreCard(
    store     : Store,
    lastSync  : Int?,
    onConnect : () -> Unit,
    onRemove  : (() -> Unit)?,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        color           = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(storeColor(store.id)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(store.name, color = Navy, fontWeight = FontWeight.SemiBold)
                Text(
                    if (lastSync != null) "Last sync: $lastSync items" else "Tap to connect & sync",
                    color    = if (lastSync != null) Green else Slate,
                    fontSize = 12.sp,
                )
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) { Icon(Icons.Filled.Close, "Remove", tint = Slate.copy(alpha = 0.5f)) }
            }
            Surface(
                shape    = CircleShape,
                color    = Green,
                modifier = Modifier.clickable(onClick = onConnect),
            ) {
                Text(
                    "Connect",
                    color      = Color.White,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun storeColor(id: String): Color = when {
    id.contains("woolworths") -> Color(0xFF008745)
    id.contains("coles")      -> Color(0xFFD81F1F)
    else                      -> Slate
}

// ── WebView ─────────────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ReceiptWebView(
    store         : Store,
    syncing       : Boolean,
    result        : SyncResult?,
    onSync        : (pageText: String) -> Unit,
    onDismiss     : () -> Unit,
    onClearResult : () -> Unit,
) {
    var webView      by remember { mutableStateOf<WebView?>(null) }
    var triggerSync  by remember { mutableStateOf(false) }

    // When the user taps Sync: expand hidden lines, wait for re-render, read innerText.
    LaunchedEffect(triggerSync) {
        if (!triggerSync) return@LaunchedEffect
        val wv = webView ?: run { triggerSync = false; return@LaunchedEffect }
        wv.evaluateJavascript(EXPAND_SCRIPT, null)
        delay(1500)
        wv.evaluateJavascript("document.body.innerText") { raw ->
            // raw is a JSON-encoded string; strip quotes and unescape
            val text = raw
                ?.removeSurrounding("\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: ""
            onSync(text)
        }
        triggerSync = false
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text("Done", color = Navy, fontWeight = FontWeight.SemiBold) }
                    Text(store.name, color = Navy, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    TextButton(onClick = { if (!syncing) triggerSync = true }, enabled = !syncing) {
                        if (syncing) {
                            CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Sync, null, tint = Green, modifier = Modifier.size(16.dp))
                                Text("Sync", color = Green, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // WebView
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory  = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled   = true
                        settings.userAgentString   = settings.userAgentString.replace("; wv", "")
                        webViewClient   = WebViewClient()
                        webChromeClient = WebChromeClient()
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        loadUrl(store.receiptUrl)
                        webView = this
                    }
                },
            )
        }

        // Instruction hint (bottom)
        if (!syncing && result == null) {
            Surface(
                modifier        = Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding(),
                shape           = RoundedCornerShape(24.dp),
                color           = Color.White,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Info, null, tint = Slate, modifier = Modifier.size(18.dp))
                    Text(receiptInstruction(store.id), color = Navy, fontSize = 12.sp)
                }
            }
        }

        // Syncing overlay
        if (syncing) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(
                        modifier            = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(color = Green)
                        Text("Reading receipt…", color = Navy, fontWeight = FontWeight.SemiBold)
                        Text("Sous Pantry is identifying your items", color = Slate, fontSize = 12.sp)
                    }
                }
            }
        }

        // Result banner
        result?.let { r ->
            val isError = r is SyncResult.Error
            Surface(
                modifier        = Modifier.align(Alignment.BottomCenter).padding(16.dp).navigationBarsPadding(),
                shape           = RoundedCornerShape(14.dp),
                color           = Color.White,
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier          = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        if (isError) Icons.Filled.Info else Icons.Filled.CheckCircle,
                        null,
                        tint = if (isError) Gold else Green,
                    )
                    Text(
                        when (r) {
                            is SyncResult.Success -> "Added ${r.count} item${if (r.count == 1) "" else "s"} to your pantry!"
                            is SyncResult.Error   -> r.message
                        },
                        color    = Navy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { if (r is SyncResult.Success) onDismiss() else onClearResult() }) {
                        Icon(Icons.Filled.Close, "Dismiss", tint = Slate)
                    }
                }
            }
        }
    }
}

private const val EXPAND_SCRIPT = """
(function() {
    var keywords = ['view all', 'show all', 'see all', 'show more', 'view order'];
    document.querySelectorAll('button, a, [role="button"], span, div').forEach(function(el) {
        var t = (el.textContent || '').toLowerCase().trim();
        if (keywords.some(function(k) { return t.startsWith(k); })) {
            try { el.click(); } catch(e) {}
        }
    });
})();
"""

// ── Add store sheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddStoreSheet(
    onDismiss : () -> Unit,
    onSave    : (name: String, url: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url  by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Add a Store", style = MaterialTheme.typography.headlineMedium, color = Navy, fontWeight = FontWeight.SemiBold)
            Text(
                "Enter the store name and the web address of its receipts / order history page.",
                color    = Slate,
                fontSize = 13.sp,
            )
            OutlinedTextField(
                value         = name,
                onValueChange = { name = it },
                label         = { Text("Store name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value         = url,
                onValueChange = { url = it },
                label         = { Text("Receipts page URL") },
                placeholder   = { Text("https://…") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
            )
            Button(
                onClick  = { if (name.isNotBlank() && url.isNotBlank()) onSave(name.trim(), url.trim()) },
                enabled  = name.isNotBlank() && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Text("Add Store", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
