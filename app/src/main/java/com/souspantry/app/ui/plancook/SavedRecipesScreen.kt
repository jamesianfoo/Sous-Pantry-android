package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.souspantry.app.ui.theme.*

@Composable
fun SavedRecipesScreen(vm: SavedRecipesViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    var detailRecipe  by remember { mutableStateOf<SavedRecipe?>(null) }
    var pendingDelete by remember { mutableStateOf<SavedRecipe?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        if (state.recipes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("🔖", fontSize = 52.sp)
                Spacer(Modifier.height(12.dp))
                Text("No saved recipes yet", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ask Sous for a recipe and tap\nthe bookmark to save it here",
                    color     = Slate,
                    fontSize  = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyVerticalGrid(
                columns             = GridCells.Adaptive(minSize = 160.dp),
                contentPadding      = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
            ) {
                items(state.recipes, key = { it.id }) { recipe ->
                    SavedRecipeCard(
                        recipe   = recipe,
                        onTap    = { detailRecipe = recipe },
                        onDelete = { pendingDelete = recipe },
                    )
                }
            }
        }
    }

    // ── Detail sheet ──────────────────────────────────────────────────────────
    detailRecipe?.let { recipe ->
        SavedRecipeDetailSheet(recipe = recipe, onDismiss = { detailRecipe = null })
    }

    // ── Delete confirm (custom modal, mirrors iOS) ────────────────────────────
    pendingDelete?.let { recipe ->
        Dialog(onDismissRequest = { pendingDelete = null }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.White) {
                Column(
                    modifier            = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Cancel, null, tint = Color(0xFFC73D2E), modifier = Modifier.size(44.dp))
                    Text("Remove Recipe?", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("\"${recipe.title}\"", color = Slate, fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick  = { vm.remove(recipe.id); pendingDelete = null },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFC73D2E)),
                            shape    = RoundedCornerShape(12.dp),
                        ) { Text("Remove", color = Color.White, fontWeight = FontWeight.SemiBold) }
                        Button(
                            onClick  = { pendingDelete = null },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = Cream),
                            shape    = RoundedCornerShape(12.dp),
                        ) { Text("Cancel", color = Navy, fontWeight = FontWeight.Medium) }
                    }
                }
            }
        }
    }
}

// ── Grid card ─────────────────────────────────────────────────────────────────

@Composable
private fun SavedRecipeCard(
    recipe   : SavedRecipe,
    onTap    : () -> Unit,
    onDelete : () -> Unit,
) {
    Box {
        Surface(
            modifier        = Modifier.fillMaxWidth().clickable(onClick = onTap),
            shape           = RoundedCornerShape(14.dp),
            color           = Color.White,
            shadowElevation = 3.dp,
        ) {
            Column {
                // Image header — gradient + emoji base, loremflickr photo overlay
                Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                    Box(
                        modifier         = Modifier.fillMaxSize().background(Brush.linearGradient(gradientColors(recipe.cuisine))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(cuisineEmoji(recipe.cuisine), fontSize = 40.sp)
                    }
                    AsyncImage(
                        model              = "https://loremflickr.com/320/240/${recipe.title.replace(" ", ",")},food",
                        contentDescription = recipe.title,
                        contentScale       = ContentScale.Crop,
                        modifier           = Modifier.fillMaxSize(),
                    )
                    // Cuisine badge bottom-left
                    Surface(
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                        shape    = CircleShape,
                        color    = Color.Black.copy(alpha = 0.45f),
                    ) {
                        Text(
                            recipe.cuisine.ifBlank { "Recipe" },
                            color      = Color.White,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
                // Title + difficulty
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(recipe.title, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                    if (recipe.difficulty.isNotBlank()) {
                        Text(recipe.difficulty, color = savedDifficultyColor(recipe.difficulty), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        // X delete button top-right
        IconButton(
            onClick  = onDelete,
            modifier = Modifier.align(Alignment.TopEnd).size(32.dp).padding(4.dp),
        ) {
            Icon(Icons.Filled.Cancel, "Remove", tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(22.dp))
        }
    }
}

// ── Detail sheet ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedRecipeDetailSheet(recipe: SavedRecipe, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header card
            Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(recipe.cuisine.uppercase(), color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (recipe.difficulty.isNotBlank()) {
                            Surface(shape = CircleShape, color = savedDifficultyColor(recipe.difficulty)) {
                                Text(recipe.difficulty, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }
                    }
                    Text(recipe.title, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    if (recipe.description.isNotBlank()) Text(recipe.description, color = Slate, fontSize = 14.sp)
                    if (recipe.prepTime.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AccessTime, null, tint = Slate, modifier = Modifier.size(12.dp))
                            Text(recipe.prepTime, color = Slate, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (recipe.ingredients.isNotEmpty()) {
                Text("INGREDIENTS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        recipe.ingredients.forEachIndexed { idx, ing ->
                            Text("• $ing", color = Navy, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp))
                            if (idx < recipe.ingredients.lastIndex) HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                        }
                    }
                }
            }
            if (recipe.instructions.isNotEmpty()) {
                Text("HOW TO COOK", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        recipe.instructions.forEachIndexed { idx, step ->
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                                    Text("${idx + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(step, color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            }
                            if (idx < recipe.instructions.lastIndex) HorizontalDivider(modifier = Modifier.padding(start = 46.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

// ── Helpers (mirror iOS gradient/emoji/difficulty maps) ───────────────────────

private fun gradientColors(cuisine: String): List<Color> {
    val c = cuisine.lowercase()
    return when {
        c.contains("italian")                                                    -> listOf(Color(0xFF801A1A), Color(0xFFCC4D1A))
        c.contains("asian") || c.contains("japanese") || c.contains("korean") || c.contains("chinese")
                                                                                 -> listOf(Color(0xFF1A3366), Color(0xFF336699))
        c.contains("indian") || c.contains("thai")                               -> listOf(Color(0xFF804D00), Color(0xFFCC8019))
        c.contains("mexican")                                                    -> listOf(Color(0xFF661A00), Color(0xFFB34D00))
        c.contains("greek") || c.contains("mediterranean")                       -> listOf(Color(0xFF1A4D80), Color(0xFF3380B3))
        else                                                                     -> listOf(Color(0xFF162437), Color(0xFF225F22))
    }
}

private fun cuisineEmoji(cuisine: String): String {
    val c = cuisine.lowercase()
    return when {
        c.contains("italian")                       -> "🍝"
        c.contains("japanese")                      -> "🍜"
        c.contains("korean")                        -> "🥘"
        c.contains("chinese") || c.contains("asian")-> "🥢"
        c.contains("indian")                        -> "🍛"
        c.contains("mexican")                       -> "🌮"
        c.contains("middle")                        -> "🧆"
        c.contains("greek")                         -> "🫒"
        c.contains("thai")                          -> "🍲"
        c.contains("french")                        -> "🥐"
        else                                        -> "🍽️"
    }
}

private fun savedDifficultyColor(d: String): Color = when (d.lowercase()) {
    "easy"   -> Color(0xFF2D5A3D)
    "medium" -> Color(0xFFCC8019)
    "hard"   -> Color(0xFFC73D2E)
    else     -> Color(0xFF647080)
}
