package com.souspantry.app.ui.pantry

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.ReceiptLineItem
import com.souspantry.app.ui.camera.CameraPreview
import com.souspantry.app.ui.theme.*
import java.io.File

@Composable
fun ReceiptScanScreen(
    onDismiss : () -> Unit,
    onSaved   : () -> Unit,
    vm        : ReceiptScanViewModel = hiltViewModel(),
) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    var capture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(state) { if (state is ReceiptScanState.Saved) onSaved() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (state is ReceiptScanState.Ready) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = { capture = it })

            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White)
            }

            FloatingActionButton(
                onClick = {
                    val file    = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture?.takePicture(options, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    ?.let { vm.processImage(it) }
                            }
                            override fun onError(e: ImageCaptureException) {
                                /* will surface as Error state on retry */
                            }
                        })
                },
                containerColor = Green,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            ) { Icon(Icons.Filled.CameraAlt, "Capture") }

            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(0.6f)) {
                Text("Point at receipt and tap capture",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    modifier = Modifier.padding(12.dp))
            }
        }

        if (state is ReceiptScanState.Loading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Reading receipt…", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                }
            }
        }

        if (state is ReceiptScanState.Results) {
            val results = (state as ReceiptScanState.Results).items
            // Local, editable copy — seeded once per result set. "Add All" saves this.
            val editable = remember(results) { mutableStateListOf<ReceiptLineItem>().apply { addAll(results) } }
            var editingIndex  by remember(results) { mutableStateOf<Int?>(null) }
            var pendingDelete by remember(results) { mutableStateOf<Int?>(null) }

            Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    // Header with Close
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text("Found ${editable.size} items", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap an item to edit it, or remove anything that's not right.",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Slate),
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, "Close", tint = Slate)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(editable) { index, line ->
                            ReviewItemCard(
                                line     = line,
                                onEdit   = { editingIndex = index },
                                onDelete = { pendingDelete = index },
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.retry() }, Modifier.weight(1f)) { Text("Retake") }
                        Button(
                            onClick  = { vm.saveAll(editable.toList()) },
                            modifier = Modifier.weight(1f),
                            enabled  = editable.isNotEmpty(),
                            colors   = ButtonDefaults.buttonColors(containerColor = Green),
                        ) { Text(if (editable.isEmpty()) "Add All" else "Add All (${editable.size})") }
                    }
                }
            }

            editingIndex?.let { idx ->
                if (idx < editable.size) {
                    EditReceiptItemSheet(
                        item      = editable[idx],
                        onDismiss = { editingIndex = null },
                        onSave    = { updated -> editable[idx] = updated; editingIndex = null },
                    )
                }
            }

            pendingDelete?.let { idx ->
                if (idx < editable.size) {
                    val itemName = editable[idx].name
                    AlertDialog(
                        onDismissRequest = { pendingDelete = null },
                        confirmButton    = {
                            TextButton(onClick = {
                                if (idx < editable.size) editable.removeAt(idx)
                                pendingDelete = null
                            }) {
                                Text("Remove", color = Color(0xFFB23A48), fontWeight = FontWeight.SemiBold)
                            }
                        },
                        dismissButton    = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
                        title            = { Text("Remove this item?") },
                        text             = { Text("\"$itemName\" won't be added to your pantry.") },
                        containerColor   = Color.White,
                    )
                }
            }
        }

        if (state is ReceiptScanState.Error) {
            Box(Modifier.fillMaxSize().background(Beige), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((state as ReceiptScanState.Error).message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.retry() }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

// ── Review item card ──────────────────────────────────────────────────────────

@Composable
private fun ReviewItemCard(
    line     : ReceiptLineItem,
    onEdit   : () -> Unit,
    onDelete : () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Row(
            modifier          = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(line.name, style = MaterialTheme.typography.titleMedium, color = Navy)
                val subtitle = listOfNotNull(
                    line.category?.takeIf { it.isNotBlank() },
                    line.quantity?.takeIf { it.isNotBlank() },
                ).joinToString("  ·  ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium.copy(color = Slate))
                }
            }
            IconButton(onClick = onEdit)   { Icon(Icons.Filled.Edit,  "Edit item",   tint = Navy) }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Close, "Remove item", tint = Slate.copy(alpha = 0.6f)) }
        }
    }
}

// ── Edit item bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditReceiptItemSheet(
    item      : ReceiptLineItem,
    onDismiss : () -> Unit,
    onSave    : (ReceiptLineItem) -> Unit,
) {
    var name     by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity ?: "") }
    var category by remember { mutableStateOf(item.category ?: "") }

    ModalBottomSheet(
        onDismissRequest    = onDismiss,
        containerColor      = Cream,
        contentWindowInsets = { WindowInsets.ime },
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Edit Item", style = MaterialTheme.typography.headlineSmall, color = Navy, fontWeight = FontWeight.SemiBold)

            EditFieldBlock("Name",     name,     { name = it },     "Product name")
            EditFieldBlock("Quantity", quantity, { quantity = it }, "e.g. 250g, 1L, 4 Pack")
            CategoryDropdownField(selected = category, onSelect = { category = it })

            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(
                        item.copy(
                            name     = name.trim(),
                            quantity = quantity.trim().ifBlank { null },
                            category = category.trim().ifBlank { null },
                        )
                    )
                },
                enabled  = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Green,
                    disabledContainerColor = Slate.copy(alpha = 0.25f),
                ),
                shape    = RoundedCornerShape(16.dp),
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditFieldBlock(
    label       : String,
    value       : String,
    onValueChange : (String) -> Unit,
    placeholder : String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = Color.White) {
            TextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = { Text(placeholder, color = Slate.copy(alpha = 0.5f)) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth(),
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                ),
            )
        }
    }
}

@Composable
private fun CategoryDropdownField(
    selected : String,
    onSelect : (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Category", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                shape    = RoundedCornerShape(12.dp),
                color    = Color.White,
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selected.ifBlank { "Select a category" },
                        color    = if (selected.isBlank()) Slate.copy(alpha = 0.5f) else Navy,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.ArrowDropDown, null, tint = Slate)
                }
            }
            DropdownMenu(
                expanded         = expanded,
                onDismissRequest = { expanded = false },
                modifier         = Modifier.background(Color.White),
            ) {
                CATEGORY_ORDER.forEach { cat ->
                    DropdownMenuItem(
                        text    = { Text(cat, color = if (cat == selected) Green else Navy) },
                        onClick = { onSelect(cat); expanded = false },
                    )
                }
            }
        }
    }
}
