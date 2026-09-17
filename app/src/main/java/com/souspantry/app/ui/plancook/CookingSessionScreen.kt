package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.services.IngredientScaler
import com.souspantry.app.services.IngredientStaples
import com.souspantry.app.ui.theme.*

/**
 * Cooking session sheet — reachable by tapping a suggested recipe in Discover.
 * Mirrors iOS CookingSessionView: adjustable serving size (scales ingredient
 * quantities live), pantry-aware ingredient checklist, and Mark as Cooked.
 *
 * No `servings` field comes back from the meals API, so the base serving size
 * is assumed at 2 (matching MyRecipe's default) purely for the scale factor.
 */
private const val BASE_SERVINGS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingSessionSheet(
    meal        : SuggestedMeal,
    pantryItems : List<PantryItem>,
    isSaved     : Boolean,
    onDismiss   : () -> Unit,
    onSave      : () -> Unit,
    onCooked    : (checkedIngredients: List<String>) -> Unit,
    onAddMissing: (missing: List<String>) -> Unit,
) {
    val ingredientRows = remember(meal, pantryItems) {
        meal.ingredients.map { ing ->
            // Staples (salt, water, oil…) always count as on hand, so they tick
            // automatically and never reach the to-buy list.
            val inPantry = IngredientStaples.isPantryStaple(ing) || pantryItems.any {
                it.name.contains(ing, ignoreCase = true) || ing.contains(it.name, ignoreCase = true)
            }
            ing to inPantry
        }
    }
    val pantryCount  = ingredientRows.count { it.second }
    val missingItems = ingredientRows.filter { !it.second }.map { it.first }
    val checkedItems = remember(meal) {
        mutableStateListOf<String>().also { list ->
            ingredientRows.filter { it.second }.forEach { list.add(it.first) }
        }
    }
    var servings        by remember(meal) { mutableStateOf(BASE_SERVINGS) }
    var addedToShopping by remember(meal) { mutableStateOf(false) }
    val scaleFactor = servings.toDouble() / BASE_SERVINGS.toDouble()

    fun scaled(ingredient: String) = IngredientScaler.scale(ingredient, scaleFactor)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header row: title + save + close ──────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    meal.title,
                    color      = Navy,
                    fontSize   = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.weight(1f).padding(top = 4.dp),
                )
                IconButton(onClick = onSave, enabled = !isSaved) {
                    Icon(
                        imageVector        = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save recipe",
                        tint               = if (isSaved) Gold else Navy,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }

            // ── Header card ────────────────────────────────────────────────
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(16.dp),
                color           = Color.White,
                shadowElevation = 4.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            meal.cuisine.uppercase(),
                            color      = Slate,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.weight(1f),
                        )
                        if (meal.difficulty.isNotBlank()) {
                            Surface(shape = CircleShape, color = sessionDifficultyColor(meal.difficulty)) {
                                Text(
                                    meal.difficulty,
                                    color      = Color.White,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    if (meal.description.isNotBlank()) {
                        Text(meal.description, color = Slate, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (meal.prepTime.isNotBlank()) {
                            Text("Prep ${meal.prepTime}", color = Slate, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        Icon(Icons.Filled.CheckCircle, null, tint = Green.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("$pantryCount of ${meal.ingredients.size} in pantry", color = Slate, fontSize = 12.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Green.copy(alpha = 0.8f), modifier = Modifier.size(12.dp))
                        Text("Suggested by Sous Pantry", color = Green.copy(alpha = 0.8f), fontSize = 11.sp)
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))

                    // ── Serving size stepper ──────────────────────────────
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Cooking for", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "$servings ${if (servings == 1) "person" else "people"}",
                                color      = Navy,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StepperButton(
                                icon    = Icons.Filled.Remove,
                                enabled = servings > 1,
                                onClick = { if (servings > 1) servings -= 1 },
                            )
                            Text(
                                "$servings",
                                color      = Navy,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.width(36.dp),
                                textAlign  = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                            StepperButton(
                                icon    = Icons.Filled.Add,
                                enabled = servings < 20,
                                onClick = { if (servings < 20) servings += 1 },
                            )
                        }
                    }
                    if (scaleFactor != 1.0) {
                        Text(
                            "Quantities scaled for $servings (base recipe serves $BASE_SERVINGS)",
                            color    = Gold,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            // ── Ingredients ────────────────────────────────────────────────
            if (ingredientRows.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "INGREDIENTS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (missingItems.isNotEmpty() && !addedToShopping) {
                            TextButton(
                                onClick = {
                                    onAddMissing(missingItems.map { IngredientScaler.cleanName(scaled(it)) })
                                    addedToShopping = true
                                },
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Icon(Icons.Filled.ShoppingCartCheckout, null, tint = Navy, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add ${missingItems.size} missing", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else if (addedToShopping) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(12.dp))
                                Text("Added to Shopping", color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Surface(
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(16.dp),
                        color           = Color.White,
                        shadowElevation = 3.dp,
                    ) {
                        Column {
                            ingredientRows.forEachIndexed { idx, (ing, inPantry) ->
                                val isChecked = inPantry && checkedItems.contains(ing)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = inPantry) {
                                            if (isChecked) checkedItems.remove(ing) else checkedItems.add(ing)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment     = Alignment.CenterVertically,
                                    horizontalArrangement  = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector        = if (isChecked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint               = if (isChecked) Green else Slate.copy(alpha = 0.35f),
                                        modifier           = Modifier.size(22.dp),
                                    )
                                    Text(
                                        scaled(ing),
                                        color    = if (inPantry && isChecked) Slate else Navy,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        when {
                                            inPantry && isChecked -> "In pantry"
                                            inPantry               -> "Excluded"
                                            else                    -> "Needed"
                                        },
                                        color      = when {
                                            inPantry && isChecked -> Green
                                            inPantry               -> Slate.copy(alpha = 0.5f)
                                            else                    -> Gold
                                        },
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (idx < ingredientRows.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 46.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Instructions ───────────────────────────────────────────────
            if (meal.instructions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HOW TO COOK", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(16.dp),
                        color           = Color.White,
                        shadowElevation = 3.dp,
                    ) {
                        Column {
                            meal.instructions.forEachIndexed { idx, step ->
                                Row(
                                    modifier              = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment     = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Box(
                                        modifier         = Modifier.size(22.dp).clip(CircleShape).background(Green),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("${idx + 1}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Text(step, color = Navy, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                }
                                if (idx < meal.instructions.lastIndex) {
                                    HorizontalDivider(modifier = Modifier.padding(start = 48.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                                }
                            }
                        }
                    }
                }
            }

            // ── Cook button ────────────────────────────────────────────────
            val hasMissing = missingItems.isNotEmpty()
            Button(
                onClick  = { onCooked(checkedItems.toList()) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled  = !hasMissing,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Green,
                    disabledContainerColor = Slate.copy(alpha = 0.25f),
                ),
                shape    = RoundedCornerShape(16.dp),
            ) {
                Icon(
                    imageVector = if (hasMissing) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (hasMissing) Slate else Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasMissing) "Missing ${missingItems.size} ingredient${if (missingItems.size == 1) "" else "s"}"
                    else "Mark as Cooked",
                    color      = if (hasMissing) Slate else Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) SoftMint else Slate.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon, null,
            tint     = if (enabled) Green else Slate.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp),
        )
    }
}

private fun sessionDifficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "easy"   -> Color(0xFF2D5A3D)
    "medium" -> Color(0xFFC4965A)
    "hard"   -> Color(0xFFB23A48)
    else     -> Color(0xFF647080)
}
