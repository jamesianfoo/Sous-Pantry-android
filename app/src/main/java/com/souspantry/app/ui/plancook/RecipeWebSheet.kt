package com.souspantry.app.ui.plancook

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.souspantry.app.services.RecipeLinkResolver
import com.souspantry.app.ui.theme.Cream
import com.souspantry.app.ui.theme.Green
import com.souspantry.app.ui.theme.Navy
import com.souspantry.app.ui.theme.Slate

/**
 * In-app browser for an external recipe page (Android equivalent of the iOS
 * Safari sheet). Sticky "Mark as Cooked" footer appears only when a JSON-LD
 * scrape gave us ingredients to deduct; otherwise the footer is hidden.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RecipeWebSheet(
    url         : String,
    title       : String,
    ingredients : List<String>,
    onDismiss   : () -> Unit,
    onCooked    : (List<String>) -> Unit,
) {
    var loading    by remember { mutableStateOf(true) }
    var webView    by remember { mutableStateOf<WebView?>(null) }
    var confirming by remember { mutableStateOf(false) }
    // Ticking these off removes them from the pantry, so the user reviews the
    // scraped list first — same as the cooking sheet's checklist.
    val used = remember(ingredients) { mutableStateListOf<String>().apply { addAll(ingredients) } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Back walks the page history first, so tapping a link inside the
        // recipe doesn't cost you your place.
        BackHandler {
            val wv = webView
            if (wv != null && wv.canGoBack()) wv.goBack() else onDismiss()
        }

        Column(modifier = Modifier.fillMaxSize().background(Cream)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(RecipeLinkResolver.host(url), color = Slate, fontSize = 11.sp, maxLines = 1)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate)
                }
            }
            if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Green)

            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) { loading = false }
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
            )

            if (ingredients.isNotEmpty()) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Button(
                        onClick  = { confirming = true },
                        modifier = Modifier.fillMaxWidth().padding(16.dp).height(50.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Green),
                        shape    = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Mark as Cooked", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Used these?", color = Navy) },
            text  = {
                Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        "Anything you leave ticked comes out of your pantry.",
                        color = Slate, fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ingredients.forEach { ing ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (used.contains(ing)) used.remove(ing) else used.add(ing)
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = used.contains(ing),
                                onCheckedChange = { on -> if (on) used.add(ing) else used.remove(ing) },
                                colors = CheckboxDefaults.colors(checkedColor = Green),
                            )
                            Text(ing, color = Navy, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onCooked(used.toList()); confirming = false; onDismiss() },
                ) { Text("Mark as Cooked", color = Green, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Cancel", color = Slate) }
            },
            containerColor = Cream,
        )
    }
}
