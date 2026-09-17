package com.souspantry.app.ui.plancook

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.souspantry.app.ui.camera.CameraPreview
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

enum class EditorMode { VIEW, EDIT }

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MyRecipesScreen(
    vm : MyRecipesViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    var editorRecipe by remember { mutableStateOf<MyRecipe?>(null) }
    var editorMode   by remember { mutableStateOf(EditorMode.VIEW) }
    var pendingDelete by remember { mutableStateOf<MyRecipe?>(null) }
    var fabExpanded  by remember { mutableStateOf(false) }
    var showCamera   by remember { mutableStateOf(false) }
    var pendingCamera by remember { mutableStateOf(false) }

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    // Photo-library picker → decode → OCR + parse
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()?.let { bmp -> vm.scanFromBitmap(bmp) }
        }
    }

    // When a scan finishes, open the editor in EDIT mode with the parsed draft
    LaunchedEffect(state.scannedDraft) {
        state.scannedDraft?.let {
            editorRecipe = it
            editorMode   = EditorMode.EDIT
            vm.consumeScannedDraft()
        }
    }

    // If the user tapped "New Scan" before granting camera access, open the
    // camera once the system permission prompt is approved.
    LaunchedEffect(cameraPermission.status.isGranted) {
        if (cameraPermission.status.isGranted && pendingCamera) {
            pendingCamera = false
            showCamera = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Beige)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Text(
                "My Recipes",
                color      = Navy,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            )

            if (state.recipes.isEmpty()) {
                EmptyRecipesState(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier            = Modifier.fillMaxSize().weight(1f),
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.recipes, key = { it.id }) { recipe ->
                        MyRecipeCard(
                            recipe      = recipe,
                            onView      = { editorRecipe = recipe; editorMode = EditorMode.VIEW },
                            onDuplicate = { vm.duplicate(recipe) },
                            onRemove    = { pendingDelete = recipe },
                        )
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }

        // ── FAB ────────────────────────────────────────────────────────────
        Column(
            modifier            = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (fabExpanded) {
                FabOption("Upload Photo", Icons.Filled.PhotoLibrary) {
                    fabExpanded = false; galleryLauncher.launch("image/*")
                }
                FabOption("New Scan", Icons.Filled.CameraAlt) {
                    fabExpanded = false
                    if (cameraPermission.status.isGranted) {
                        showCamera = true
                    } else {
                        pendingCamera = true
                        cameraPermission.launchPermissionRequest()
                    }
                }
                FabOption("Write from Scratch", Icons.Filled.Edit) {
                    fabExpanded = false
                    editorRecipe = MyRecipe(title = "", source = MyRecipeSource.MY_CREATION)
                    editorMode   = EditorMode.EDIT
                }
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Green)
                    .clickable { fabExpanded = !fabExpanded },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (fabExpanded) Icons.Filled.Close else Icons.Filled.Add,
                    contentDescription = if (fabExpanded) "Close" else "Add recipe",
                    tint               = Color.White,
                    modifier           = Modifier.size(24.dp),
                )
            }
        }

        // Scanning overlay
        if (state.scanning) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(color = Green, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Reading recipe…", color = Navy, fontSize = 14.sp)
                    }
                }
            }
        }

        // "Saved to My Recipes ✓" toast
        if (state.toastVisible) {
            Surface(
                modifier        = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                shape           = RoundedCornerShape(16.dp),
                color           = Color(0xFFE5F4ED),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(18.dp))
                    Text("Saved to My Recipes ✓", color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // ── Camera capture overlay ────────────────────────────────────────────────
    if (showCamera) {
        RecipeCameraCapture(
            onDismiss  = { showCamera = false },
            onCaptured = { bitmap ->
                showCamera = false
                vm.scanFromBitmap(bitmap)
            },
        )
    }

    // ── Editor ──────────────────────────────────────────────────────────────
    editorRecipe?.let { recipe ->
        RecipeEditorSheet(
            recipe      = recipe,
            initialMode = editorMode,
            onDismiss   = { editorRecipe = null },
            onSave      = { saved -> vm.save(saved); editorRecipe = null },
        )
    }

    // ── Delete confirm ────────────────────────────────────────────────────────
    pendingDelete?.let { recipe ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton    = {
                TextButton(onClick = { vm.remove(recipe.id); pendingDelete = null }) {
                    Text("Remove", color = Color(0xFFB23A48), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title            = { Text("Remove this recipe?") },
            text             = { Text("\"${recipe.title.ifBlank { "This recipe" }}\" will be deleted from your collection. This can't be undone.") },
            containerColor   = Color.White,
        )
    }

    // ── Scan error ────────────────────────────────────────────────────────────
    state.scanError?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.clearScanError() },
            confirmButton    = { TextButton(onClick = { vm.clearScanError() }) { Text("OK") } },
            title            = { Text("Scan error") },
            text             = { Text(msg) },
            containerColor   = Color.White,
        )
    }
}

// ── Camera capture (full-screen) ──────────────────────────────────────────────

@Composable
private fun RecipeCameraCapture(
    onDismiss  : () -> Unit,
    onCaptured : (android.graphics.Bitmap) -> Unit,
) {
    val context  = LocalContext.current
    var capture  by remember { mutableStateOf<ImageCapture?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(
            modifier      = Modifier.fillMaxSize(),
            onCameraReady = { capture = it },
        )

        // Close button
        IconButton(
            onClick  = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Icon(Icons.Filled.Close, "Close", tint = Color.White)
        }

        // Hint
        Surface(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            shape    = RoundedCornerShape(12.dp),
            color    = Color.Black.copy(alpha = 0.6f),
        ) {
            Text(
                "Point at a recipe and tap capture",
                color    = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        // Capture button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(72.dp)
                .clip(CircleShape)
                .background(Green)
                .clickable {
                    val file    = File(context.cacheDir, "recipe_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture?.takePicture(
                        options,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                BitmapFactory.decodeFile(file.absolutePath)?.let(onCaptured)
                            }
                            override fun onError(e: ImageCaptureException) { /* surfaced as scan error on retry */ }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.CameraAlt, "Capture", tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

// ── FAB option pill ───────────────────────────────────────────────────────────

@Composable
private fun FabOption(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
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
            Text(label, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(Green),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ── Recipe card ───────────────────────────────────────────────────────────────

@Composable
private fun MyRecipeCard(
    recipe      : MyRecipe,
    onView      : () -> Unit,
    onDuplicate : () -> Unit,
    onRemove    : () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onView),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    recipe.title.ifBlank { "Untitled recipe" },
                    color      = Navy,
                    fontSize   = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (recipe.cuisine.isNotBlank())    GrayChip(recipe.cuisine)
                    if (recipe.difficulty.isNotBlank()) GrayChip(recipe.difficulty)
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.MoreVert, "More", tint = Slate)
                }
                DropdownMenu(
                    expanded         = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor   = Color.White,
                ) {
                    DropdownMenuItem(
                        text        = { Text("View Recipe") },
                        leadingIcon = { Icon(Icons.Filled.Visibility, null, tint = Navy) },
                        onClick     = { menuOpen = false; onView() },
                    )
                    DropdownMenuItem(
                        text        = { Text("Duplicate") },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, null, tint = Navy) },
                        onClick     = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text        = { Text("Remove", color = Color(0xFFB23A48)) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = Color(0xFFB23A48)) },
                        onClick     = { menuOpen = false; onRemove() },
                    )
                }
            }
        }
    }
}

@Composable
private fun GrayChip(text: String) {
    Text(
        text,
        color    = Navy.copy(alpha = 0.75f),
        fontSize = 13.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(Color(0xFFEFEFEF))
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyRecipesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("📖", fontSize = 48.sp)
            Text("Your recipes will appear here", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Scan a recipe, upload a photo, or write your own — they'll all live here.",
                color     = Slate,
                fontSize  = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ── Recipe editor (view + edit) ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorSheet(
    recipe      : MyRecipe,
    initialMode : EditorMode,
    onDismiss   : () -> Unit,
    onSave      : (MyRecipe) -> Unit,
) {
    var mode by remember { mutableStateOf(initialMode) }

    // Editable field state
    var name        by remember { mutableStateOf(recipe.title) }
    var serves      by remember { mutableStateOf(recipe.servings) }
    var prep        by remember { mutableStateOf(recipe.prepTime) }
    var cook        by remember { mutableStateOf(recipe.cookTime) }
    var cuisine     by remember { mutableStateOf(recipe.cuisine) }
    var difficulty  by remember { mutableStateOf(recipe.difficulty) }
    val ingredients = remember { mutableStateListOf<String>().apply { addAll(recipe.ingredients) } }
    val steps       = remember { mutableStateListOf<String>().apply { addAll(recipe.instructions) } }
    var notes       by remember { mutableStateOf(recipe.notes) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
    ) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose {}
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            // ── Toolbar ─────────────────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close", tint = Navy) }
                Text("My Recipe", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (mode == EditorMode.VIEW) {
                    TextButton(onClick = { mode = EditorMode.EDIT }) {
                        Text("Edit", color = Green, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    recipe.copy(
                                        title        = name.trim(),
                                        servings     = serves,
                                        prepTime     = prep.trim(),
                                        cookTime     = cook.trim(),
                                        cuisine      = cuisine.trim(),
                                        difficulty   = difficulty.trim(),
                                        ingredients  = ingredients.map { it.trim() }.filter { it.isNotBlank() },
                                        instructions = steps.map { it.trim() }.filter { it.isNotBlank() },
                                        notes        = notes.trim(),
                                    )
                                )
                            }
                        },
                    ) {
                        Text("Save", color = Green, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (mode == EditorMode.VIEW) {
                    RecipeViewBody(name, serves, prep, cook, ingredients, steps, notes)
                } else {
                    RecipeEditBody(
                        name        = name,        onName       = { name = it },
                        serves      = serves,      onServes     = { serves = it },
                        prep        = prep,        onPrep       = { prep = it },
                        cook        = cook,        onCook       = { cook = it },
                        cuisine     = cuisine,     onCuisine    = { cuisine = it },
                        difficulty  = difficulty,  onDifficulty = { difficulty = it },
                        ingredients = ingredients,
                        steps       = steps,
                        notes       = notes,       onNotes      = { notes = it },
                    )
                }
            }
        }
    }
}

// ── View mode ─────────────────────────────────────────────────────────────────

@Composable
private fun RecipeViewBody(
    name        : String,
    serves      : Int,
    prep        : String,
    cook        : String,
    ingredients : List<String>,
    steps       : List<String>,
    notes       : String,
) {
    // Basics card
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(name.ifBlank { "Untitled recipe" }, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("Serves $serves")
                if (prep.isNotBlank()) MetaChip("Prep $prep")
                if (cook.isNotBlank()) MetaChip("Cook $cook")
            }
        }
    }

    if (ingredients.isNotEmpty()) {
        SectionLabel("Ingredients")
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                ingredients.forEachIndexed { idx, ing ->
                    Text(ing, color = Navy, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp))
                    if (idx < ingredients.lastIndex) HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                }
            }
        }
    }

    if (steps.isNotEmpty()) {
        SectionLabel("Cooking Instructions")
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column {
                steps.forEachIndexed { idx, step ->
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment     = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier         = Modifier.size(22.dp).clip(CircleShape).background(Navy),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${idx + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(step, color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                    if (idx < steps.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 46.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                }
            }
        }
    }

    if (notes.isNotBlank()) {
        SectionLabel("Notes")
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Text(notes, color = Navy, fontSize = 15.sp, modifier = Modifier.padding(14.dp))
        }
    }
}

// ── Edit mode ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecipeEditBody(
    name        : String, onName       : (String) -> Unit,
    serves      : Int,    onServes     : (Int) -> Unit,
    prep        : String, onPrep       : (String) -> Unit,
    cook        : String, onCook       : (String) -> Unit,
    cuisine     : String, onCuisine    : (String) -> Unit,
    difficulty  : String, onDifficulty : (String) -> Unit,
    ingredients : androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    steps       : androidx.compose.runtime.snapshots.SnapshotStateList<String>,
    notes       : String, onNotes      : (String) -> Unit,
) {
    // Basics card
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EditField("Recipe name", name, onName)
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Serves", color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { if (serves > 1) onServes(serves - 1) }) { Text("−", fontSize = 18.sp, color = Green) }
                Text("$serves", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { if (serves < 20) onServes(serves + 1) }) { Text("+", fontSize = 18.sp, color = Green) }
            }
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Prep", color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                CompactField(prep, onPrep, "15 min")
            }
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cook", color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                CompactField(cook, onCook, "30 min")
            }
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Cuisine", color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                CompactField(cuisine, onCuisine, "Italian")
            }
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Difficulty", color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                CompactField(difficulty, onDifficulty, "Medium")
            }
        }
    }

    // Ingredients editor
    SectionLabel("Ingredients")
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column {
            ingredients.forEachIndexed { idx, _ ->
                EditableListRow(
                    leadingNumber = null,
                    value         = ingredients.getOrElse(idx) { "" },
                    placeholder   = "e.g. 200g chicken breast",
                    onValueChange = { if (idx < ingredients.size) ingredients[idx] = it },
                    onDelete      = { if (idx < ingredients.size) ingredients.removeAt(idx) },
                )
                if (idx < ingredients.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 40.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            }
            AddRowButton("Add ingredient") { ingredients.add("") }
        }
    }

    // Steps editor
    SectionLabel("Cooking Instructions")
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column {
            steps.forEachIndexed { idx, _ ->
                EditableListRow(
                    leadingNumber = idx + 1,
                    value         = steps.getOrElse(idx) { "" },
                    placeholder   = "Describe this step…",
                    onValueChange = { if (idx < steps.size) steps[idx] = it },
                    onDelete      = { if (idx < steps.size) steps.removeAt(idx) },
                )
                if (idx < steps.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 70.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
            }
            AddRowButton("Add step") { steps.add("") }
        }
    }

    // Notes
    SectionLabel("Notes")
    Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        EditField("Anything to remember…", notes, onNotes, modifier = Modifier.padding(4.dp))
    }
}

// ── Small editor building blocks ──────────────────────────────────────────────

@Composable
private fun MetaChip(text: String) {
    Text(
        text,
        color    = Navy.copy(alpha = 0.75f),
        fontSize = 13.sp,
        modifier = Modifier.clip(CircleShape).background(Color(0xFFEFEFEF)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditField(placeholder: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder, color = Slate.copy(alpha = 0.5f)) },
        modifier      = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        colors        = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor   = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder, color = Slate.copy(alpha = 0.5f), fontSize = 14.sp) },
        singleLine    = true,
        modifier      = Modifier.width(120.dp),
        textStyle     = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.End, fontSize = 14.sp, color = Navy),
        colors        = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.Transparent,
            focusedContainerColor   = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor   = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditableListRow(
    leadingNumber : Int?,
    value         : String,
    placeholder   : String,
    onValueChange : (String) -> Unit,
    onDelete      : () -> Unit,
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingNumber != null) {
            Box(
                modifier         = Modifier.size(22.dp).clip(CircleShape).background(Navy),
                contentAlignment = Alignment.Center,
            ) {
                Text("$leadingNumber", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(Icons.Filled.DragHandle, null, tint = Slate.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
        TextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text(placeholder, color = Slate.copy(alpha = 0.5f), fontSize = 15.sp) },
            modifier      = Modifier.weight(1f),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            colors        = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor   = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor   = Color.Transparent,
            ),
        )
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, "Delete", tint = Slate.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddRowButton(label: String, onClick: () -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.Filled.Add, null, tint = Green, modifier = Modifier.size(18.dp))
        Text(label, color = Green, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
