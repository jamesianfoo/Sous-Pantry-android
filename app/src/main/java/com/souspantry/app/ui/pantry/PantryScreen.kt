package com.souspantry.app.ui.pantry

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ── Canonical category order (mirrors iOS PantryItem.categoryOrder) ──────────

internal val CATEGORY_ORDER = listOf(
    "Fruits", "Vegetables", "Meat & Seafood", "Dairy & Eggs", "Bakery & Bread",
    "Pantry & Dry Goods", "Frozen", "Snacks & Confectionery", "Beverages",
    "Condiments & Sauces", "Breakfast & Cereals", "Baby & Toddler",
    "Health & Wellness", "Cleaning & Household",
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantryScreen(
    onBarcodeScan  : () -> Unit = {},
    onReceiptScan  : () -> Unit = {},
    onSyncEreceipt : () -> Unit = {},
    vm             : PantryViewModel = hiltViewModel(),
) {
    // Request camera permission once on first render
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest() }

    // Data
    val items by vm.items.collectAsState(initial = emptyList())

    // UI state
    var showAdd            by remember { mutableStateOf(false) }
    var editItem           by remember { mutableStateOf<PantryItem?>(null) }
    var searchQuery        by remember { mutableStateOf("") }
    var addMenuOpen        by remember { mutableStateOf(false) }
    var isMultiSelectMode  by remember { mutableStateOf(false) }
    val selectedIds        = remember { mutableStateListOf<String>() }
    var pendingBulkDelete  by remember { mutableStateOf(false) }
    var pendingSingleDelete by remember { mutableStateOf<PantryItem?>(null) }
    var selectedCategory   by remember { mutableStateOf<String?>(null) }
    var specialFilter      by remember { mutableStateOf<String?>(null) } // "New" | "Expiring" | null

    // Voice search launcher
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { searchQuery = it }
        }
    }

    // Derived
    val nowMs = System.currentTimeMillis()
    val newCount      = items.count { nowMs - it.dateAdded < 24L * 3_600_000L }
    val expiringCount = items.count { (it.expiryDate ?: Long.MAX_VALUE) - nowMs <= 7L * 86_400_000L }

    val filtered = items
        .filter { item ->
            (searchQuery.isBlank() || item.name.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategory == null || (item.category ?: "Other") == selectedCategory) &&
            when (specialFilter) {
                "New"      -> nowMs - item.dateAdded < 24L * 3_600_000L
                "Expiring" -> (item.expiryDate ?: Long.MAX_VALUE) - nowMs <= 7L * 86_400_000L
                else       -> true
            }
        }
    val grouped = filtered.groupBy { it.category ?: "Other" }
    val orderedCategories = CATEGORY_ORDER.filter { grouped.containsKey(it) } +
                            grouped.keys.filterNot { CATEGORY_ORDER.contains(it) }
    val availableCategories = items.mapNotNull { it.category }.distinct()

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {

        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ───────────────────────────────────────────────
            HeaderBar(
                itemCount         = items.size,
                isMultiSelectMode = isMultiSelectMode,
                addMenuOpen       = addMenuOpen,
                selectedCount     = selectedIds.size,
                displayedCount    = filtered.size,
                onAddTap          = { addMenuOpen = !addMenuOpen },
                onSelectTap       = {
                    isMultiSelectMode = !isMultiSelectMode
                    if (!isMultiSelectMode) selectedIds.clear()
                },
                onSelectAll       = {
                    if (selectedIds.size == filtered.size) selectedIds.clear()
                    else { selectedIds.clear(); selectedIds.addAll(filtered.map { it.id }) }
                },
                onBulkDelete      = { if (selectedIds.isNotEmpty()) pendingBulkDelete = true },
            )

            // ── SEARCH BAR ───────────────────────────────────────────
            SearchBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                onVoiceTap    = {
                    runCatching {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search your pantry")
                        }
                        voiceLauncher.launch(intent)
                    }
                },
            )

            // ── CATEGORY FILTER PILLS ────────────────────────────────
            if (items.isNotEmpty()) {
                CategoryFilterPills(
                    availableCategories = availableCategories,
                    selectedCategory    = selectedCategory,
                    specialFilter       = specialFilter,
                    newCount            = newCount,
                    expiringCount       = expiringCount,
                    onSelectCategory    = {
                        selectedCategory = if (selectedCategory == it) null else it
                        specialFilter    = null
                    },
                    onSelectSpecial     = {
                        specialFilter    = if (specialFilter == it) null else it
                        selectedCategory = null
                    },
                )
            }

            // ── CONTENT ──────────────────────────────────────────────
            when {
                items.isEmpty()    -> EmptyPantryState(modifier = Modifier.fillMaxSize().weight(1f))
                filtered.isEmpty() -> NoResultsState(modifier = Modifier.fillMaxSize().weight(1f))
                else -> SectionedContent(
                    orderedCategories = orderedCategories,
                    grouped           = grouped,
                    isMultiSelectMode = isMultiSelectMode,
                    selectedIds       = selectedIds,
                    onToggleSelected  = { id ->
                        if (selectedIds.contains(id)) selectedIds.remove(id)
                        else selectedIds.add(id)
                    },
                    onEdit            = { editItem = it },
                    onDelete          = { pendingSingleDelete = it },
                    onQuantityChange  = { item, q -> vm.update(item.copy(quantity = q)) },
                    modifier          = Modifier.fillMaxSize().weight(1f),
                )
            }
        }

        // ── ADD PILL MENU OVERLAY ────────────────────────────────────
        if (addMenuOpen) {
            // Tap-outside scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication        = null,
                    ) { addMenuOpen = false }
            )
            AddPillMenu(
                onScanBarcode  = { addMenuOpen = false; onBarcodeScan() },
                onScanReceipt  = { addMenuOpen = false; onReceiptScan() },
                onSyncEreceipt = { addMenuOpen = false; onSyncEreceipt() },
                onAddManually  = { addMenuOpen = false; showAdd = true },
                modifier       = Modifier.align(Alignment.TopEnd).padding(top = 70.dp, end = 16.dp),
            )
        }
    }

    // ── Sheets / dialogs ─────────────────────────────────────────────
    if (showAdd) {
        AddPantryItemSheet(
            onDismiss = { showAdd = false },
            onSave    = { vm.add(it); showAdd = false },
        )
    }
    editItem?.let { item ->
        AddPantryItemSheet(
            initial   = item,
            onDismiss = { editItem = null },
            onSave    = { vm.update(it); editItem = null },
        )
    }
    pendingSingleDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingSingleDelete = null },
            confirmButton    = {
                TextButton(onClick = {
                    vm.delete(item); pendingSingleDelete = null
                }) { Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton    = { TextButton(onClick = { pendingSingleDelete = null }) { Text("Cancel") } },
            title            = { Text("Delete \"${item.name}\"?") },
            text             = { Text("This will permanently remove it from your pantry.") },
            containerColor   = Color.White,
        )
    }
    if (pendingBulkDelete) {
        val count = selectedIds.size
        AlertDialog(
            onDismissRequest = { pendingBulkDelete = false },
            confirmButton    = {
                TextButton(onClick = {
                    val toDelete = items.filter { selectedIds.contains(it.id) }
                    toDelete.forEach { vm.delete(it) }
                    selectedIds.clear()
                    isMultiSelectMode = false
                    pendingBulkDelete = false
                }) { Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold) }
            },
            dismissButton    = { TextButton(onClick = { pendingBulkDelete = false }) { Text("Cancel") } },
            title            = { Text("Delete $count item${if (count == 1) "" else "s"}?") },
            text             = { Text("These items will be permanently removed from your pantry.") },
            containerColor   = Color.White,
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HeaderBar(
    itemCount         : Int,
    isMultiSelectMode : Boolean,
    addMenuOpen       : Boolean,
    selectedCount     : Int,
    displayedCount    : Int,
    onAddTap          : () -> Unit,
    onSelectTap       : () -> Unit,
    onSelectAll       : () -> Unit,
    onBulkDelete      : () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("My Pantry", style = MaterialTheme.typography.headlineLarge, color = Navy)
                Text(
                    "$itemCount item${if (itemCount == 1) "" else "s"} in stock",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate,
                )
            }
            if (!isMultiSelectMode) {
                CircleIconButton(
                    icon            = if (addMenuOpen) Icons.Filled.Close else Icons.Filled.Add,
                    contentDesc     = if (addMenuOpen) "Close menu" else "Add",
                    containerColor  = Navy,
                    onClick         = onAddTap,
                )
                Spacer(Modifier.width(10.dp))
            }
            CircleIconButton(
                icon            = if (isMultiSelectMode) Icons.Filled.Check else Icons.Filled.Checklist,
                contentDesc     = if (isMultiSelectMode) "Done selecting" else "Select",
                containerColor  = if (isMultiSelectMode) Green else Navy,
                onClick         = onSelectTap,
            )
        }
        if (isMultiSelectMode) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSelectAll, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text       = if (selectedCount == displayedCount && displayedCount > 0) "Deselect All" else "Select All",
                        color      = Green,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.labelLarge,
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onBulkDelete, enabled = selectedCount > 0, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text       = "Delete${if (selectedCount > 0) " ($selectedCount)" else ""}",
                        color      = if (selectedCount > 0) Color(0xFFD32F2F) else Slate,
                        fontWeight = FontWeight.SemiBold,
                        style      = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun CircleIconButton(
    icon          : androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc   : String,
    containerColor: Color,
    onClick       : () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDesc, tint = Color.White,
            modifier = Modifier.size(18.dp))
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBar(
    query         : String,
    onQueryChange : (String) -> Unit,
    onVoiceTap    : () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp),
        shape           = RoundedCornerShape(28.dp),
        color           = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, "Search", tint = Slate)
            Spacer(Modifier.width(8.dp))
            TextField(
                value         = query,
                onValueChange = onQueryChange,
                placeholder   = { Text("Search pantry…", color = Slate.copy(alpha = 0.7f)) },
                modifier      = Modifier.weight(1f),
                singleLine    = true,
                colors        = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                ),
            )
            VerticalDivider(modifier = Modifier.height(20.dp), color = Slate.copy(alpha = 0.3f))
            IconButton(onClick = onVoiceTap) {
                Icon(Icons.Filled.Mic, "Voice search", tint = Navy)
            }
        }
    }
}

// ── Add pill menu (overlay) ──────────────────────────────────────────────────

@Composable
private fun AddPillMenu(
    onScanBarcode  : () -> Unit,
    onScanReceipt  : () -> Unit,
    onSyncEreceipt : () -> Unit,
    onAddManually  : () -> Unit,
    modifier       : Modifier = Modifier,
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AddPill("Scan Items",    Icons.Filled.CameraAlt, onScanBarcode)
        AddPill("Scan Receipt",  Icons.Filled.Receipt,   onScanReceipt)
        AddPill("Sync eReceipt", Icons.Filled.Sync,      onSyncEreceipt)
        AddPill("Add Item",      Icons.Filled.Add,       onAddManually)
    }
}

@Composable
private fun AddPill(
    label  : String,
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        shape           = CircleShape,
        color           = Color.White,
        shadowElevation = 4.dp,
        modifier        = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier          = Modifier.padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.width(10.dp))
            Box(
                modifier         = Modifier.size(40.dp).clip(CircleShape).background(Green),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Category filter pills ─────────────────────────────────────────────────────

@Composable
private fun CategoryFilterPills(
    availableCategories : List<String>,
    selectedCategory    : String?,
    specialFilter       : String?,
    newCount            : Int,
    expiringCount       : Int,
    onSelectCategory    : (String) -> Unit,
    onSelectSpecial     : (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label      = "All",
            isSelected = selectedCategory == null && specialFilter == null,
            color      = Navy,
            onClick    = { onSelectCategory(selectedCategory ?: "") }, // tapping All clears via category logic if selected, otherwise no-op
        )
        if (newCount > 0) {
            FilterPill("New ($newCount)",      specialFilter == "New",      Green) { onSelectSpecial("New") }
        }
        if (expiringCount > 0) {
            FilterPill("Expiring ($expiringCount)", specialFilter == "Expiring", Color(0xFFEF6C00)) { onSelectSpecial("Expiring") }
        }
        // categories — keep canonical order, then any others
        val orderedCats = CATEGORY_ORDER.filter { availableCategories.contains(it) } +
                          availableCategories.filterNot { CATEGORY_ORDER.contains(it) }
        orderedCats.forEach { cat ->
            FilterPill(cat, selectedCategory == cat, Navy) { onSelectCategory(cat) }
        }
    }
}

@Composable
private fun FilterPill(
    label      : String,
    isSelected : Boolean,
    color      : Color,
    onClick    : () -> Unit,
) {
    Surface(
        shape           = CircleShape,
        color           = if (isSelected) color else Color.White,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        border          = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color.Transparent else color.copy(alpha = 0.25f),
        ),
        modifier        = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text       = label,
            color      = if (isSelected) Color.White else color,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize   = 12.sp,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

// ── Sectioned content ─────────────────────────────────────────────────────────

@Composable
private fun SectionedContent(
    orderedCategories : List<String>,
    grouped           : Map<String, List<PantryItem>>,
    isMultiSelectMode : Boolean,
    selectedIds       : List<String>,
    onToggleSelected  : (String) -> Unit,
    onEdit            : (PantryItem) -> Unit,
    onDelete          : (PantryItem) -> Unit,
    onQuantityChange  : (PantryItem, Int) -> Unit,
    modifier          : Modifier = Modifier,
) {
    LazyColumn(
        modifier              = modifier,
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement   = Arrangement.spacedBy(12.dp),
    ) {
        orderedCategories.forEach { category ->
            val catItems = grouped[category].orEmpty()
            if (catItems.isNotEmpty()) {
                item(key = "header-$category") {
                    Text(
                        text       = category.uppercase(),
                        style      = MaterialTheme.typography.labelSmall,
                        color      = Slate,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                    )
                }
                item(key = "card-$category") {
                    Surface(
                        modifier        = Modifier.fillMaxWidth().shadow(
                            elevation = 6.dp, shape = RoundedCornerShape(14.dp),
                            ambientColor = Navy.copy(alpha = 0.06f),
                            spotColor    = Navy.copy(alpha = 0.06f),
                        ),
                        shape           = RoundedCornerShape(14.dp),
                        color           = Color.White,
                    ) {
                        Column {
                            catItems.forEachIndexed { idx, item ->
                                PantryRowView(
                                    item              = item,
                                    isMultiSelectMode = isMultiSelectMode,
                                    isSelected        = selectedIds.contains(item.id),
                                    onTap             = {
                                        if (isMultiSelectMode) onToggleSelected(item.id)
                                    },
                                    onEdit            = { onEdit(item) },
                                    onDelete          = { onDelete(item) },
                                    onIncrement       = { onQuantityChange(item, item.quantity + 1) },
                                    onDecrement       = { onQuantityChange(item, item.quantity - 1) },
                                )
                                if (idx < catItems.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = if (isMultiSelectMode) 52.dp else 14.dp),
                                        thickness = 0.5.dp,
                                        color = Slate.copy(alpha = 0.15f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // bottom breathing room for bottom nav
        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun PantryRowView(
    item              : PantryItem,
    isMultiSelectMode : Boolean,
    isSelected        : Boolean,
    onTap             : () -> Unit,
    onEdit            : () -> Unit,
    onDelete          : () -> Unit,
    onIncrement       : () -> Unit,
    onDecrement       : () -> Unit,
) {
    val isNew = System.currentTimeMillis() - item.dateAdded < 30L * 60L * 1_000L
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .background(if (isSelected) Green.copy(alpha = 0.06f) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Multi-select checkbox
        if (isMultiSelectMode) {
            Icon(
                imageVector        = if (isSelected) Icons.Filled.Check else Icons.Filled.Checklist,
                contentDescription = null,
                tint               = if (isSelected) Green else Slate.copy(alpha = 0.4f),
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        }

        // Emoji thumbnail
        Box(
            modifier         = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoftMint),
            contentAlignment = Alignment.Center,
        ) {
            Text(categoryEmoji(item.category, item.name), fontSize = 20.sp)
        }

        Spacer(Modifier.width(12.dp))

        // Name / brand / expiry
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyLarge, color = Navy)
                if (isNew) {
                    Surface(shape = CircleShape, color = Green) {
                        Text(
                            "NEW",
                            color      = Color.White,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            item.brand?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Slate)
            }
            item.expiryDate?.let { ExpiryLabel(it) }
        }

        // Stepper + menu (hidden in multi-select)
        if (!isMultiSelectMode) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepperButton(
                    label   = "−",
                    enabled = item.quantity > 1,
                    onClick = onDecrement,
                )
                Text(
                    "×${item.quantity}",
                    style              = MaterialTheme.typography.bodyMedium,
                    color              = Navy,
                    fontWeight         = FontWeight.Medium,
                    modifier           = Modifier.widthIn(min = 32.dp),
                    textAlign          = TextAlign.Center,
                )
                StepperButton(
                    label   = "+",
                    enabled = true,
                    onClick = onIncrement,
                )
                RowMenu(onEdit = onEdit, onDelete = onDelete)
            }
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier         = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            color      = if (enabled) Green else Slate.copy(alpha = 0.3f),
            fontSize   = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RowMenu(onEdit: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, "More", tint = Slate.copy(alpha = 0.7f))
        }
        DropdownMenu(
            expanded         = open,
            onDismissRequest = { open = false },
            containerColor   = Color.White,
        ) {
            DropdownMenuItem(
                text        = { Text("Edit") },
                leadingIcon = { Icon(Icons.Filled.Edit, null, tint = Navy) },
                onClick     = { open = false; onEdit() },
            )
            DropdownMenuItem(
                text        = { Text("Remove") },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFD32F2F)) },
                onClick     = { open = false; onDelete() },
            )
        }
    }
}

// ── Empty / no-results ───────────────────────────────────────────────────────

@Composable
private fun EmptyPantryState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(
                imageVector        = Icons.Outlined.Kitchen,
                contentDescription = null,
                tint               = SoftMint,
                modifier           = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Your pantry is empty",
                style      = MaterialTheme.typography.titleLarge,
                color      = Navy,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Tap \"Add\" to scan items, scan a receipt,\nor add items manually.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Slate,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun NoResultsState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(horizontal = 32.dp),
        ) {
            Icon(Icons.Filled.Search, null, tint = SoftMint, modifier = Modifier.size(60.dp))
            Spacer(Modifier.height(12.dp))
            Text("No matches", style = MaterialTheme.typography.titleMedium, color = Navy)
            Spacer(Modifier.height(6.dp))
            Text(
                "Try a different search or filter.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Slate,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Expiry label ─────────────────────────────────────────────────────────────

@Composable
private fun ExpiryLabel(epochMillis: Long) {
    val nowMs = System.currentTimeMillis()
    val days  = ((epochMillis - nowMs) / 86_400_000L).toInt()

    val (label, color) = when {
        days < 0  -> "Expired"  to Color(0xFFD32F2F)
        days == 0 -> "Today"    to Color(0xFFEF6C00)
        days <= 7 -> "$days day${if (days == 1) "" else "s"}" to Color(0xFFEF6C00)
        else      -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMillis)) to Slate
    }
    Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
}

// ── Emoji mapping ────────────────────────────────────────────────────────────

private fun categoryEmoji(category: String?, name: String): String {
    // Quick name-based overrides for common items
    val n = name.lowercase()
    when {
        "milk" in n              -> return "🥛"
        "egg"  in n              -> return "🥚"
        "bread" in n             -> return "🍞"
        "cheese" in n            -> return "🧀"
        "butter" in n            -> return "🧈"
        "apple" in n             -> return "🍎"
        "banana" in n            -> return "🍌"
        "tomato" in n            -> return "🍅"
        "potato" in n            -> return "🥔"
        "carrot" in n            -> return "🥕"
        "chicken" in n           -> return "🍗"
        "beef" in n              -> return "🥩"
        "fish" in n              -> return "🐟"
        "rice" in n              -> return "🍚"
        "pasta" in n             -> return "🍝"
        "coffee" in n            -> return "☕"
        "tea" in n               -> return "🍵"
        "wine" in n              -> return "🍷"
        "beer" in n              -> return "🍺"
        "chocolate" in n         -> return "🍫"
    }
    return when (category) {
        "Fruits"                 -> "🍎"
        "Vegetables"             -> "🥬"
        "Meat & Seafood"         -> "🥩"
        "Dairy & Eggs"           -> "🥛"
        "Bakery & Bread"         -> "🍞"
        "Pantry & Dry Goods"     -> "🌾"
        "Frozen"                 -> "🧊"
        "Snacks & Confectionery" -> "🍫"
        "Beverages"              -> "🥤"
        "Condiments & Sauces"    -> "🥫"
        "Breakfast & Cereals"    -> "🥣"
        "Baby & Toddler"         -> "🍼"
        "Health & Wellness"      -> "💊"
        "Cleaning & Household"   -> "🧴"
        else                     -> "🥫"
    }
}

// ── Add/Edit bottom sheet ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPantryItemSheet(
    initial   : PantryItem? = null,
    onDismiss : () -> Unit,
    onSave    : (PantryItem) -> Unit,
) {
    var name     by remember { mutableStateOf(initial?.name     ?: "") }
    var brand    by remember { mutableStateOf(initial?.brand    ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var qty      by remember { mutableStateOf(initial?.quantity ?: 1) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier            = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (initial == null) "Add Item" else "Edit Item", style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (optional)") }, modifier = Modifier.fillMaxWidth())

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value        = category.ifEmpty { "Select Category" },
                    onValueChange = {},
                    readOnly     = true,
                    label        = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier     = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    CATEGORY_ORDER.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { category = cat; expanded = false })
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantity", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { if (qty > 1) qty-- }) { Text("−") }
                Text("$qty", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (qty < 99) qty++ }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: PantryItem(name = name)).copy(
                                name     = name.trim(),
                                brand    = brand.trim().ifEmpty { null },
                                category = category.ifEmpty { null },
                                quantity = qty,
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
            ) {
                Text(if (initial == null) "Add to Pantry" else "Save Changes")
            }
        }
    }
}
