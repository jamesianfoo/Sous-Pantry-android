package com.souspantry.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.ui.theme.Cream
import com.souspantry.app.ui.theme.Danger
import com.souspantry.app.ui.theme.Navy
import com.souspantry.app.ui.theme.Slate

/** iOS accent green for this editor (rgb 0.176/0.416/0.176 — differs from theme Forest). */
private val EditorGreen = Color(0xFF2D6A2D)

private data class Chip(val id: String, val label: String, val emoji: String)

// Mirrors iOS DietaryEditorSheet.swift — do not invent options.
private val DIETARY_TYPES = listOf(
    Chip("vegan", "Vegan", "🌱"), Chip("vegetarian", "Vegetarian", "🥕"),
    Chip("pescatarian", "Pescatarian", "🐟"), Chip("no_beef", "No Beef", "🥩"),
    Chip("no_pork", "No Pork", "🥓"), Chip("paleo", "Paleo", "🍖"),
    Chip("keto", "Keto", "🥑"), Chip("halal", "Halal", "🌙"),
)

private val RESTRICTIONS = listOf(
    Chip("gluten_free", "Gluten-Free", "🌾"), Chip("nut_free", "Nut-Free", "🥜"),
    Chip("dairy_free", "Dairy-Free", "🥛"), Chip("shellfish_free", "Shellfish-Free", "🦐"),
    Chip("egg_free", "Egg-Free", "🥚"), Chip("soy_free", "Soy-Free", "🫘"),
    Chip("pregnancy", "Pregnancy-Safe", "🤰"),
)

/** Pending chip-tap confirmation (dietary prefs shape every AI suggestion). */
private data class PendingToggle(val chip: Chip, val adding: Boolean, val isType: Boolean)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DietaryEditorSheet(
    types                : Set<String>,
    restrictions         : Set<String>,
    avoid                : Set<String>,
    onTypesChange        : (Set<String>) -> Unit,
    onRestrictionsChange : (Set<String>) -> Unit,
    onAvoidChange        : (Set<String>) -> Unit,
    onDismiss            : () -> Unit,
) {
    // Housekeeping: silently strip stale ""/"none" values on open.
    LaunchedEffect(Unit) {
        if (types.any { it.isBlank() || it == "none" })        onTypesChange(types.filterNot { it.isBlank() || it == "none" }.toSet())
        if (restrictions.any { it.isBlank() || it == "none" }) onRestrictionsChange(restrictions.filterNot { it.isBlank() || it == "none" }.toSet())
    }

    var pending      by remember { mutableStateOf<PendingToggle?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var newAvoid     by remember { mutableStateOf("") }

    val isEmpty = types.isEmpty() && restrictions.isEmpty() && avoid.isEmpty()

    fun addAvoid() {
        val clean = newAvoid.trim()
        if (clean.isNotEmpty() && avoid.none { it.equals(clean, ignoreCase = true) }) {
            onAvoidChange(avoid + clean)
        }
        newAvoid = ""
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Toolbar: Clear All (red, disabled when empty) · title · Save
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Clear All",
                    color      = if (isEmpty) Slate.copy(alpha = 0.4f) else Danger,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.clickable(enabled = !isEmpty) { confirmClear = true },
                )
                Text(
                    "Dietary Preferences",
                    color = Navy, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Text(
                    "Save",
                    color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onDismiss),   // changes persist live; Save just closes
                )
            }

            // Section A — Dietary Type
            EditorSection("Dietary Type", "Tap to toggle. These shape every suggestion.") {
                ChipGrid(DIETARY_TYPES, types) { chip, selected ->
                    pending = PendingToggle(chip, adding = !selected, isType = true)
                }
            }

            // Section B — Food Restrictions
            EditorSection("Food Restrictions", "Allergies and must-avoids.") {
                ChipGrid(RESTRICTIONS, restrictions) { chip, selected ->
                    pending = PendingToggle(chip, adding = !selected, isType = false)
                }
            }

            // Section C — Ingredients to Avoid
            EditorSection("Ingredients to Avoid", null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape  = RoundedCornerShape(10.dp),
                        color  = Color.White,
                        border = BorderStroke(1.dp, Navy.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f),
                    ) {
                        TextField(
                            value = newAvoid, onValueChange = { newAvoid = it }, singleLine = true,
                            placeholder = { Text("Add ingredient...", color = Slate.copy(alpha = 0.5f), fontSize = 14.sp) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addAvoid() }),
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                            ),
                        )
                    }
                    Surface(
                        shape    = RoundedCornerShape(10.dp),
                        color    = EditorGreen.copy(alpha = if (newAvoid.isBlank()) 0.4f else 1f),
                        modifier = Modifier.clickable(enabled = newAvoid.isNotBlank()) { addAvoid() },
                    ) {
                        Text("Add", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                    }
                }
                if (avoid.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        avoid.sortedBy { it.lowercase() }.forEach { ing ->
                            Surface(
                                shape  = CircleShape,
                                color  = EditorGreen.copy(alpha = 0.15f),
                                border = BorderStroke(1.5.dp, EditorGreen),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                ) {
                                    Text(ing, color = EditorGreen, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(
                                        Icons.Filled.Cancel, "Remove $ing", tint = EditorGreen,
                                        modifier = Modifier.size(16.dp).clickable { onAvoidChange(avoid - ing) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Per-tap confirmation — dietary prefs shape every AI suggestion.
    pending?.let { p ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(if (p.adding) "Add ${p.chip.label}?" else "Remove ${p.chip.label}?") },
            text  = { Text(if (p.adding) "All future recipe suggestions will follow this."
                           else "Future suggestions will no longer account for this.") },
            confirmButton = {
                TextButton(onClick = {
                    val apply = { set: Set<String> -> if (p.adding) set + p.chip.id else set - p.chip.id }
                    if (p.isType) onTypesChange(apply(types)) else onRestrictionsChange(apply(restrictions))
                    pending = null
                }) { Text(if (p.adding) "Add" else "Remove", fontWeight = FontWeight.SemiBold) }
            },
            dismissButton  = { TextButton(onClick = { pending = null }) { Text("Cancel") } },
            containerColor = Color.White,
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all dietary preferences?") },
            text  = { Text("Removes every dietary type, restriction, and avoided ingredient. Suggestions will no longer be filtered.") },
            confirmButton = {
                TextButton(onClick = {
                    onTypesChange(emptySet()); onRestrictionsChange(emptySet()); onAvoidChange(emptySet())
                    confirmClear = false
                }) { Text("Clear All", color = Danger, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton  = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun EditorSection(title: String, subtitle: String?, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        if (subtitle != null) Text(subtitle, color = Slate, fontSize = 12.sp)
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChipGrid(chips: List<Chip>, selected: Set<String>, onTap: (Chip, Boolean) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        chips.forEach { chip ->
            val isOn = chip.id in selected
            Surface(
                shape  = CircleShape,
                color  = if (isOn) EditorGreen.copy(alpha = 0.15f) else Slate.copy(alpha = 0.10f),
                border = if (isOn) BorderStroke(1.5.dp, EditorGreen) else null,
                modifier = Modifier.clickable { onTap(chip, isOn) },
            ) {
                Text(
                    "${chip.emoji} ${chip.label}",
                    color      = if (isOn) EditorGreen else Slate,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

/**
 * Meal Moods editor — Customisation section. Shares the funnel's mood options
 * (single source of truth) so Account and onboarding never drift. Instant
 * toggle, hard cap 3 (4th tap is a no-op), no confirmation dialogs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealMoodsEditor(
    moods    : Set<String>,
    onToggle : (String) -> Unit,
) {
    val options = remember {
        com.souspantry.app.ui.funnel.FUNNEL_STEPS.first { it.id == "moods" }.options
    }
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Meal moods", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Text("Pick up to 3. Plan & Cook focuses on these.", color = Slate, fontSize = 12.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val isOn = opt.id in moods
                Surface(
                    shape  = CircleShape,
                    color  = if (isOn) EditorGreen.copy(alpha = 0.15f) else Slate.copy(alpha = 0.10f),
                    border = if (isOn) BorderStroke(1.5.dp, EditorGreen) else null,
                    modifier = Modifier.clickable { onToggle(opt.id) },
                ) {
                    Text(
                        "${opt.emoji} ${opt.label}",
                        color      = if (isOn) EditorGreen else Slate,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
