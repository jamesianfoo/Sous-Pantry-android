package com.souspantry.app.ui.pantry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.material3.SwipeToDismissBoxValue.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PantryScreen(
    onBarcodeScan : () -> Unit = {},
    onReceiptScan : () -> Unit = {},
    vm            : PantryViewModel = hiltViewModel(),
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    LaunchedEffect(Unit) { if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest() }

    val items    by vm.items.collectAsState(initial = emptyList())
    var showAdd  by remember { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<PantryItem?>(null) }

    val grouped = items.groupBy { it.category ?: "Other" }

    Scaffold(
        containerColor = Cream,
        floatingActionButton = {
            var menuOpen by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (menuOpen) {
                    SmallFloatingActionButton(onClick = { showAdd = true; menuOpen = false }, containerColor = Navy, contentColor = Color.White) {
                        Icon(Icons.Filled.Edit, "Manual add")
                    }
                    SmallFloatingActionButton(onClick = { onReceiptScan(); menuOpen = false }, containerColor = Navy, contentColor = Color.White) {
                        Icon(Icons.Filled.Receipt, "Receipt")
                    }
                    SmallFloatingActionButton(onClick = { onBarcodeScan(); menuOpen = false }, containerColor = Navy, contentColor = Color.White) {
                        Icon(Icons.Filled.QrCodeScanner, "Barcode")
                    }
                }
                FloatingActionButton(
                    onClick = { menuOpen = !menuOpen },
                    containerColor = Green, contentColor = Color.White, shape = CircleShape,
                ) { Icon(if (menuOpen) Icons.Filled.Close else Icons.Filled.Add, if (menuOpen) "Close" else "Menu") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                    Text("My Pantry", style = MaterialTheme.typography.headlineLarge)
                    Text("${items.size} items", style = MaterialTheme.typography.bodyMedium)
                }
            }

            grouped.forEach { (category, catItems) ->
                item {
                    Text(
                        text     = category.uppercase(),
                        style    = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                        color    = Slate,
                    )
                }
                items(catItems, key = { it.id }) { item ->
                    SwipeablePantryRow(
                        item     = item,
                        onEdit   = { editItem = item },
                        onDelete = { vm.delete(item) },
                    )
                }
            }
        }
    }

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
}

// ── Swipeable row ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePantryRow(
    item    : PantryItem,
    onEdit  : () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                StartToEnd -> { onEdit();   false }  // swipe right = edit (don't dismiss)
                EndToStart -> { onDelete(); true  }  // swipe left  = delete
                Settled    -> false
            }
        }
    )

    SwipeToDismissBox(
        state            = dismissState,
        backgroundContent = {
            val (color, icon, alignment) = when (dismissState.dismissDirection) {
                StartToEnd -> Triple(Navy,             Icons.Filled.Edit,   Alignment.CenterStart)
                EndToStart -> Triple(Color(0xFFD32F2F), Icons.Filled.Delete, Alignment.CenterEnd)
                Settled    -> Triple(Color.Transparent, Icons.Filled.Edit,  Alignment.CenterStart)
            }
            Box(
                modifier          = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                contentAlignment  = alignment,
            ) {
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        },
        content = { PantryRow(item) },
    )
}

// ── Pantry row ────────────────────────────────────────────────────────────────

@Composable
private fun PantryRow(item: PantryItem) {
    val isNew = System.currentTimeMillis() - item.dateAdded < 30 * 60 * 1_000L

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier            = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (isNew) {
                        Surface(shape = RoundedCornerShape(50), color = Green) {
                            Text("NEW", style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                item.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                item.expiryDate?.let { ExpiryLabel(it) }
            }
            Text("×${item.quantity}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ExpiryLabel(epochMillis: Long) {
    val now  = System.currentTimeMillis()
    val diff = epochMillis - now
    val days = (diff / (1000 * 60 * 60 * 24)).toInt()

    val (label, color) = when {
        days < 0  -> "Expired"  to Color(0xFFD32F2F)
        days <= 7 -> "$days d"  to Color(0xFFE65100)
        else      -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(epochMillis)) to Slate
    }
    Text(label, style = MaterialTheme.typography.labelSmall.copy(color = color))
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

    val categories = listOf(
        "Fruits","Vegetables","Meat & Seafood","Dairy & Eggs","Bakery & Bread",
        "Pantry & Dry Goods","Frozen","Snacks & Confectionery","Beverages",
        "Condiments & Sauces","Breakfast & Cereals","Baby & Toddler",
        "Health & Wellness","Cleaning & Household",
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (initial == null) "Add Item" else "Edit Item", style = MaterialTheme.typography.headlineMedium)

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = brand, onValueChange = { brand = it }, label = { Text("Brand (optional)") }, modifier = Modifier.fillMaxWidth())

            // Category dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value            = category.ifEmpty { "Select Category" },
                    onValueChange    = {},
                    readOnly         = true,
                    label            = { Text("Category") },
                    trailingIcon     = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier         = Modifier.menuAnchor().fillMaxWidth(),
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text    = { Text(cat) },
                            onClick = { category = cat; expanded = false },
                        )
                    }
                }
            }

            // Quantity stepper
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Quantity", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { if (qty > 1) qty-- }) { Text("−") }
                Text("$qty", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { if (qty < 99) qty++ }) { Text("+") }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = {
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
