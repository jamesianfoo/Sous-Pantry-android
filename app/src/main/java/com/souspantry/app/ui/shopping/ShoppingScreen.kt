package com.souspantry.app.ui.shopping

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.RECIPE_REASON_PREFIX
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingScreen(vm: ShoppingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    var showAddSheet      by remember { mutableStateOf(false) }
    var itemToEdit        by remember { mutableStateOf<ShoppingItem?>(null) }
    var searchQuery       by remember { mutableStateOf("") }
    var showBasketConfirm by remember { mutableStateOf(false) }
    var pendingDelete     by remember { mutableStateOf<ShoppingItem?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { searchQuery = it }
        }
    }

    val trimmed   = searchQuery.trim()
    val matches   = { s: ShoppingItem -> trimmed.isEmpty() || s.name.contains(trimmed, ignoreCase = true) }
    val unchecked = state.items.filter { !it.checked && matches(it) }
    val checked   = state.items.filter { it.checked && matches(it) }
    val essential = unchecked.filter { it.priority == "essential" }
    val optional  = unchecked.filter { it.priority == "optional" }
    val totalUnchecked = state.items.count { !it.checked }

    val duplicateOfSearch = if (trimmed.isEmpty()) null
                            else state.items.firstOrNull { it.name.trim().equals(trimmed, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize().background(Beige)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Frozen header ───────────────────────────────────────
            ShoppingHeader(
                totalUnchecked = totalUnchecked,
                hasChecked     = checked.isNotEmpty(),
                isGenerating   = state.loading,
                onAddTap       = { showAddSheet = true },
                onAiTap        = { vm.generate() },
                onBasketTap    = { showBasketConfirm = true },
            )

            // Search bar
            ShoppingSearchBar(
                query         = searchQuery,
                onQueryChange = { searchQuery = it },
                onVoiceTap    = {
                    runCatching {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search or add an item")
                        }
                        voiceLauncher.launch(intent)
                    }
                },
            )

            // Creatable-select row — appears whenever search has text
            if (trimmed.isNotEmpty()) {
                if (duplicateOfSearch != null) {
                    DuplicateHint(name = duplicateOfSearch.name)
                } else {
                    AddFromSearchRow(text = trimmed, onAdd = {
                        vm.add(trimmed)
                        searchQuery = ""
                    })
                }
            }

            // ── Content ─────────────────────────────────────────────
            when {
                state.items.isEmpty() && !state.loading && state.error == null ->
                    EmptyStateBody(
                        pantryEmpty  = state.pantryEmpty,
                        isGenerating = state.loading,
                        onGenerate   = { vm.generate() },
                        modifier     = Modifier.weight(1f),
                    )
                else -> ShoppingListBody(
                    essential       = essential,
                    optional        = optional,
                    inBasket        = checked,
                    isGenerating    = state.loading,
                    searchActive    = trimmed.isNotEmpty(),
                    onToggle        = { vm.toggle(it) },
                    onEdit          = { itemToEdit = it },
                    onDelete        = { pendingDelete = it },
                    onClearSearch   = { searchQuery = "" },
                    error           = state.error,
                    onRetry         = { vm.generate() },
                    modifier        = Modifier.weight(1f),
                )
            }
        }
    }

    // ── Sheets / dialogs ─────────────────────────────────────────────
    if (showAddSheet) {
        AddShoppingItemSheet(
            onDismiss = { showAddSheet = false },
            onSave    = { name, qty, priority ->
                vm.add(name, qty, priority); showAddSheet = false
            },
        )
    }
    itemToEdit?.let { item ->
        AddShoppingItemSheet(
            initial   = item,
            onDismiss = { itemToEdit = null },
            onSave    = { name, qty, priority ->
                vm.update(item.id, name, qty, priority); itemToEdit = null
            },
        )
    }
    if (showBasketConfirm) {
        AlertDialog(
            onDismissRequest = { showBasketConfirm = false },
            confirmButton    = {
                TextButton(onClick = {
                    vm.moveCheckedToPantry(); showBasketConfirm = false
                }) { Text("Move to Pantry", color = Green, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton    = {
                TextButton(onClick = {
                    vm.clearChecked(); showBasketConfirm = false
                }) { Text("Delete from List", color = Color(0xFFB23A48)) }
            },
            title          = { Text("${checked.size} item${if (checked.size == 1) "" else "s"} in your basket") },
            text           = { Text("Move them to your pantry, or remove them from your shopping list?") },
            containerColor = Color.White,
        )
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton    = {
                TextButton(onClick = { vm.delete(item); pendingDelete = null }) {
                    Text("Delete", color = Color(0xFFB23A48), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title            = { Text("Delete \"${item.name}\"?") },
            text             = { Text("This will remove it from your shopping list.") },
            containerColor   = Color.White,
        )
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun ShoppingHeader(
    totalUnchecked : Int,
    hasChecked     : Boolean,
    isGenerating   : Boolean,
    onAddTap       : () -> Unit,
    onAiTap        : () -> Unit,
    onBasketTap    : () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Shopping List", style = MaterialTheme.typography.headlineLarge, color = Navy)
            Text(
                "$totalUnchecked item${if (totalUnchecked == 1) "" else "s"} to get",
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasChecked) {
                CircleIconButton(Icons.Filled.Check, "Basket actions", Green, onBasketTap)
            }
            // AI restock — green with sparkles icon (or spinner while generating)
            if (isGenerating) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(Green),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp)) }
            } else {
                CircleIconButton(Icons.Filled.AutoAwesome, "Restock with AI", Green, onAiTap)
            }
            CircleIconButton(Icons.Filled.Add, "Add", Navy, onAddTap)
        }
    }
}

@Composable
private fun CircleIconButton(
    icon          : ImageVector,
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
        Icon(icon, contentDescription = contentDesc, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShoppingSearchBar(
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
                placeholder   = { Text("Search list or type to add…", color = Slate.copy(alpha = 0.7f)) },
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

// ── Creatable-select rows ────────────────────────────────────────────────────

@Composable
private fun AddFromSearchRow(text: String, onAdd: () -> Unit) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onAdd),
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier         = Modifier.size(24.dp).clip(CircleShape).background(Green),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Add \"$text\" to list",
                color      = Navy,
                fontWeight = FontWeight.Medium,
                style      = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun DuplicateHint(name: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .background(Cream, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = Slate.copy(alpha = 0.5f))
        Spacer(Modifier.width(10.dp))
        Text(
            "\"$name\" is already in your list",
            color = Slate,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

// ── List body ────────────────────────────────────────────────────────────────

@Composable
private fun ShoppingListBody(
    essential     : List<ShoppingItem>,
    optional      : List<ShoppingItem>,
    inBasket      : List<ShoppingItem>,
    isGenerating  : Boolean,
    searchActive  : Boolean,
    onToggle      : (ShoppingItem) -> Unit,
    onEdit        : (ShoppingItem) -> Unit,
    onDelete      : (ShoppingItem) -> Unit,
    onClearSearch : () -> Unit,
    error         : String?,
    onRetry       : () -> Unit,
    modifier      : Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (error != null) {
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                color           = Color.White,
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Couldn't generate your list", style = MaterialTheme.typography.bodyMedium, color = Navy, fontWeight = FontWeight.SemiBold)
                    Text(error, style = MaterialTheme.typography.bodySmall, color = Slate)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Try Again")
                    }
                }
            }
        }

        if (isGenerating && essential.isEmpty() && optional.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Sous Pantry is building your list…", color = Slate, style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (searchActive && essential.isEmpty() && optional.isEmpty() && inBasket.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.Search, null, tint = SoftMint, modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(12.dp))
                Text("No items match", style = MaterialTheme.typography.titleMedium, color = Navy, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onClearSearch) {
                    Text("Clear Search", color = Green)
                }
            }
        } else {
            if (essential.isNotEmpty()) {
                ShoppingSection("Essential", Green, essential, onToggle, onEdit, onDelete)
            }
            if (optional.isNotEmpty()) {
                ShoppingSection("Nice to Have", Gold, optional, onToggle, onEdit, onDelete)
            }
            if (inBasket.isNotEmpty()) {
                ShoppingSection("In Basket", Slate, inBasket, onToggle, onEdit, onDelete)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ShoppingSection(
    title    : String,
    dotColor : Color,
    items    : List<ShoppingItem>,
    onToggle : (ShoppingItem) -> Unit,
    onEdit   : (ShoppingItem) -> Unit,
    onDelete : (ShoppingItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(
                title.uppercase(),
                style      = MaterialTheme.typography.labelSmall,
                color      = Slate,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "(${items.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Slate.copy(alpha = 0.6f),
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth().shadow(
                elevation     = 6.dp,
                shape         = RoundedCornerShape(16.dp),
                ambientColor  = Navy.copy(alpha = 0.06f),
                spotColor     = Navy.copy(alpha = 0.06f),
            ),
            shape    = RoundedCornerShape(16.dp),
            color    = Color.White,
        ) {
            Column {
                items.forEachIndexed { idx, item ->
                    ShoppingRowView(
                        item     = item,
                        onToggle = { onToggle(item) },
                        onEdit   = { onEdit(item) },
                        onDelete = { onDelete(item) },
                    )
                    if (idx < items.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(start = 46.dp),
                            thickness = 0.5.dp,
                            color     = Slate.copy(alpha = 0.15f),
                        )
                    }
                }
            }
        }
    }
}

// ── Row ───────────────────────────────────────────────────────────────────────

@Composable
private fun ShoppingRowView(
    item    : ShoppingItem,
    onToggle: () -> Unit,
    onEdit  : () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector        = if (item.checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint               = if (item.checked) Green else Slate.copy(alpha = 0.35f),
            modifier           = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.name,
                style      = MaterialTheme.typography.bodyLarge,
                color      = if (item.checked) Slate else Navy,
                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
            )
            item.quantity?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Slate)
            }
            // Items added from a recipe say which one, so they're identifiable
            // among everything else on the list.
            item.reason?.takeIf { it.startsWith(RECIPE_REASON_PREFIX) }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Green, maxLines = 1)
            }
        }
        // Sparkles indicator for AI-generated items (reason is non-null when AI provided context)
        if (item.reason != null) {
            Icon(
                imageVector        = Icons.Filled.AutoAwesome,
                contentDescription = "AI suggestion",
                tint               = Gold.copy(alpha = 0.7f),
                modifier           = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        // Row menu
        RowMenu(onEdit = onEdit, onDelete = onDelete)
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
                text        = { Text("Delete") },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFB23A48)) },
                onClick     = { open = false; onDelete() },
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateBody(
    pantryEmpty  : Boolean,
    isGenerating : Boolean,
    onGenerate   : () -> Unit,
    modifier     : Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (pantryEmpty) {
            EmptyPantryPrompt(isGenerating = isGenerating, onGenerate = onGenerate)
        } else {
            RestockPrompt(isGenerating = isGenerating, onGenerate = onGenerate)
        }
    }
}

@Composable
private fun RestockPrompt(isGenerating: Boolean, onGenerate: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            Icons.Outlined.ShoppingCart, null,
            tint     = SoftMint,
            modifier = Modifier.size(72.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Your list is clear!",
                style      = MaterialTheme.typography.titleLarge,
                color      = Navy,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Sous Pantry will look at what's in your pantry and suggest what to pick up on your next run.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Slate,
                textAlign = TextAlign.Center,
            )
        }
        GradientButton(
            label    = if (isGenerating) "Building your list…" else "Let's Restock",
            colors   = listOf(Green, Green.copy(alpha = 0.85f)),
            icon     = Icons.Filled.AutoAwesome,
            loading  = isGenerating,
            onClick  = onGenerate,
        )
    }
}

@Composable
private fun EmptyPantryPrompt(isGenerating: Boolean, onGenerate: () -> Unit) {
    Column(
        modifier            = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(
            modifier         = Modifier.size(90.dp).clip(CircleShape).background(SoftMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.ShoppingCart, null,
                tint     = Green,
                modifier = Modifier.size(40.dp),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Your pantry is empty — let's fix that!",
                style      = MaterialTheme.typography.titleLarge,
                color      = Navy,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
            )
            Text(
                "Sous Pantry will build your first shopping list with everyday kitchen essentials so you're ready to cook anything.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = Slate,
                textAlign = TextAlign.Center,
            )
        }
        GradientButton(
            label    = if (isGenerating) "Building your list…" else "Stock Up the Essentials",
            colors   = listOf(Navy, Navy.copy(alpha = 0.85f)),
            icon     = Icons.Filled.AutoAwesome,
            loading  = isGenerating,
            onClick  = onGenerate,
        )
    }
}

@Composable
private fun GradientButton(
    label  : String,
    colors : List<Color>,
    icon   : ImageVector,
    loading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !loading, onClick = onClick),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.Transparent,
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors))
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Add / Edit bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoppingItemSheet(
    initial   : ShoppingItem? = null,
    onDismiss : () -> Unit,
    onSave    : (name: String, qty: String?, priority: String) -> Unit,
) {
    var name     by remember { mutableStateOf(initial?.name     ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity ?: "") }
    var priority by remember { mutableStateOf(initial?.priority ?: "essential") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier            = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                if (initial == null) "Add to Shopping List" else "Edit Item",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Navy,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value           = name,
                onValueChange   = { name = it },
                label           = { Text("Item *") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )
            OutlinedTextField(
                value         = quantity,
                onValueChange = { quantity = it },
                label         = { Text("Quantity (optional, e.g. 500g)") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
            )
            // Priority segmented control
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth()) {
                PrioritySegment(
                    label = "Essential", selected = priority == "essential",
                    color = Green, modifier = Modifier.weight(1f),
                ) { priority = "essential" }
                PrioritySegment(
                    label = "Nice to Have", selected = priority == "optional",
                    color = Gold, modifier = Modifier.weight(1f),
                ) { priority = "optional" }
            }
            Button(
                onClick  = { if (name.isNotBlank()) onSave(name.trim(), quantity.takeIf { it.isNotBlank() }, priority) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                enabled  = name.isNotBlank(),
            ) {
                Text(if (initial == null) "Add to List" else "Save Changes")
            }
        }
    }
}

@Composable
private fun PrioritySegment(
    label   : String,
    selected: Boolean,
    color   : Color,
    modifier: Modifier = Modifier,
    onClick : () -> Unit,
) {
    Surface(
        modifier  = modifier.padding(horizontal = 4.dp).clickable(onClick = onClick),
        shape     = RoundedCornerShape(10.dp),
        color     = if (selected) color else Color.White,
        border    = BorderStroke(1.dp, color.copy(alpha = if (selected) 0f else 0.3f)),
    ) {
        Text(
            label,
            modifier   = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            color      = if (selected) Color.White else color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign  = TextAlign.Center,
            fontSize   = 13.sp,
        )
    }
}
