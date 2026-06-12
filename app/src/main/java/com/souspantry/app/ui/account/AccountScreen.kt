package com.souspantry.app.ui.account

import android.content.Intent
import android.net.Uri
import com.souspantry.app.BuildConfig
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(vm: AccountViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    var showDietaryEditor by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {

        // ── Header ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Text("Account", style = MaterialTheme.typography.headlineLarge, color = Navy)
            Text(
                if (state.userName.isBlank()) "Settings & preferences" else state.userName,
                style = MaterialTheme.typography.bodyMedium,
                color = Slate,
            )
        }

        Column(
            modifier              = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement   = Arrangement.spacedBy(12.dp),
        ) {

            // ── Premium ───────────────────────────────────────
            SectionCard {
                Column {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(Gold.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.WorkspacePremium, null, tint = Gold, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sous Pantry Premium", color = Navy, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (state.isPremium) "Active" else "Free", color = Slate, fontSize = 12.sp)
                                if (!state.isPremium) {
                                    Surface(
                                        shape    = RoundedCornerShape(4.dp),
                                        color    = Green,
                                        modifier = Modifier.clickable { /* TODO: paywall — Task 8 */ },
                                    ) {
                                        Text(
                                            "Upgrade",
                                            color      = Color.White,
                                            fontSize   = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // Debug-only Force Premium toggle (compiled out of release builds)
                    if (BuildConfig.DEBUG) {
                        Divider()
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Debug: Force Premium", color = Color(0xFFE65100), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Bypasses billing — unlocks every Pro tab locally.",
                                    color    = Slate,
                                    fontSize = 11.sp,
                                )
                            }
                            Switch(
                                checked         = state.forcePremium,
                                onCheckedChange = { vm.setForcePremium(it) },
                                colors          = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFFE65100),
                                ),
                            )
                        }
                    }
                }
            }

            // ── Details ───────────────────────────────────────
            SectionLabel("DETAILS")
            SectionCard {
                Column {
                    DetailRow("Email", state.userEmail.ifBlank { "Not signed in" })
                    Divider()
                    EditableNameRow(
                        name        = state.userName,
                        onNameChange = { vm.setUserName(it) },
                    )
                    Divider()
                    GenderRow(
                        currentValue = state.gender,
                        onChange     = { vm.setGender(it) },
                    )
                }
            }

            // ── Cooking for ───────────────────────────────────
            SectionLabel("COOKING FOR")
            SectionCard {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("People in your household", color = Navy, modifier = Modifier.weight(1f))
                    StepperControl(
                        value    = state.cookingFor,
                        min      = 1,
                        max      = 12,
                        onChange = { vm.setCookingFor(it) },
                    )
                }
            }

            // ── Dietary Preferences ───────────────────────────
            SectionLabel("DIETARY PREFERENCES")
            SectionCard {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text     = dietarySummary(state.dietaryTypes, state.foodRestrictions, state.avoidIngredients),
                        color    = if (dietaryCount(state) == 0) Slate else Navy,
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        shape    = RoundedCornerShape(8.dp),
                        color    = Green,
                        modifier = Modifier.clickable { showDietaryEditor = true },
                    ) {
                        Text(
                            "Add/Edit",
                            color      = Color.White,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            // ── Measurement ───────────────────────────────────
            SectionLabel("MEASUREMENT")
            SectionCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    SegmentedPickerRow(
                        label        = "System",
                        options      = listOf("metric" to "Metric (g, ml)", "imperial" to "Imperial (oz, cups)"),
                        currentValue = state.measurementSystem,
                        onChange     = { vm.setMeasurementSystem(it) },
                    )
                    SegmentedPickerRow(
                        label        = "Temperature",
                        options      = listOf("celsius" to "°C", "fahrenheit" to "°F"),
                        currentValue = state.temperatureUnit,
                        onChange     = { vm.setTemperatureUnit(it) },
                    )
                }
            }

            // ── Notifications ─────────────────────────────────
            SectionLabel("NOTIFICATIONS")
            SectionCard {
                Column {
                    ToggleRow("Expiring items", state.alertExpiring) { vm.setAlertExpiring(it) }
                    Divider()
                    ToggleRow("Receipt import status", state.alertReceipt) { vm.setAlertReceipt(it) }
                    Divider()
                    ToggleRow("Pantry out of date", state.alertPantryStale) { vm.setAlertPantryStale(it) }
                }
            }

            // ── Customisation ─────────────────────────────────
            SectionLabel("CUSTOMISATION")
            SectionCard {
                Column {
                    ToggleRow("Recommended substitutions", state.recommendedSubs) { vm.setRecommendedSubs(it) }
                    Divider()
                    ToggleRow("Sous Pantry suggestions", state.sousAIEnabled) { vm.setSousAIEnabled(it) }
                }
            }

            // ── Rate ──────────────────────────────────────────
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // Open Play Store listing for this app
                            val pkg = context.packageName
                            val playUrl = "https://play.google.com/store/apps/details?id=$pkg"
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }.onFailure {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(playUrl))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Star, null, tint = Navy)
                    Spacer(Modifier.width(12.dp))
                    Text("Rate Sous Pantry", color = Navy, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, null, tint = Slate.copy(alpha = 0.4f))
                }
            }

            // ── Account actions ───────────────────────────────
            SectionCard {
                Column {
                    DestructiveActionRow(
                        label   = "Log Out",
                        icon    = Icons.Filled.ExitToApp,
                        onClick = { showLogoutConfirm = true },
                    )
                    Divider()
                    DestructiveActionRow(
                        label   = "Delete Account",
                        icon    = Icons.Filled.Delete,
                        onClick = { showDeleteConfirm = true },
                    )
                }
            }

            // ── Customer ID footer ────────────────────────────
            Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 32.dp, start = 4.dp, end = 4.dp)) {
                Text("Customer ID", color = Slate.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    state.deviceId.ifBlank { "—" },
                    color      = Slate,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Share this with support if you need help with your subscription.",
                    color    = Slate.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                )
            }
        }
    }

    // ── Sheets / dialogs ─────────────────────────────────────────────

    if (showDietaryEditor) {
        DietaryEditorSheet(
            initialTypes        = state.dietaryTypes,
            initialRestrictions = state.foodRestrictions,
            initialAvoid        = state.avoidIngredients,
            onDismiss           = { showDietaryEditor = false },
            onSave              = { types, restrictions, avoid ->
                vm.setDietaryTypes(types)
                vm.setFoodRestrictions(restrictions)
                vm.setAvoidIngredients(avoid)
                showDietaryEditor = false
            },
        )
    }
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            confirmButton    = {
                TextButton(onClick = { vm.logOut(); showLogoutConfirm = false }) {
                    Text("Log Out", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = { TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") } },
            title            = { Text("Log out of Sous Pantry?") },
            text             = { Text("You'll need to sign in again to access your account.") },
            containerColor   = Color.White,
        )
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton    = {
                TextButton(onClick = { vm.deleteAccount(); showDeleteConfirm = false }) {
                    Text("Delete", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } },
            title            = { Text("Delete account?") },
            text             = { Text("This will permanently erase your pantry items and all preferences. This can't be undone.") },
            containerColor   = Color.White,
        )
    }
}

// ── Reusable building blocks ─────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text       = text,
        color      = Slate,
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier.padding(start = 8.dp, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(
            elevation    = 6.dp,
            shape        = RoundedCornerShape(14.dp),
            ambientColor = Navy.copy(alpha = 0.06f),
            spotColor    = Navy.copy(alpha = 0.06f),
        ),
        shape   = RoundedCornerShape(14.dp),
        color   = Color.White,
    ) {
        content()
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier  = Modifier.padding(start = 14.dp),
        thickness = 0.5.dp,
        color     = Slate.copy(alpha = 0.15f),
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Navy, modifier = Modifier.weight(1f))
        Text(value, color = Slate, fontSize = 13.sp)
    }
}

@Composable
private fun EditableNameRow(name: String, onNameChange: (String) -> Unit) {
    var local by remember(name) { mutableStateOf(name) }
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Name", color = Navy, modifier = Modifier.weight(1f))
        BasicNameField(
            value         = local,
            onValueChange = {
                local = it
                onNameChange(it.trim())
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicNameField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text("Add your name", color = Slate.copy(alpha = 0.6f), fontSize = 13.sp) },
        singleLine    = true,
        textStyle     = androidx.compose.ui.text.TextStyle(
            color    = Slate,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
        ),
        modifier      = Modifier.widthIn(min = 140.dp),
        colors        = TextFieldDefaults.colors(
            unfocusedContainerColor   = Color.Transparent,
            focusedContainerColor     = Color.Transparent,
            unfocusedIndicatorColor   = Color.Transparent,
            focusedIndicatorColor     = Color.Transparent,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenderRow(currentValue: String, onChange: (String) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val options = listOf(
        ""                 to "Not specified",
        "male"             to "Male",
        "female"           to "Female",
        "non_binary"       to "Non-binary",
        "prefer_not"       to "Prefer not to say",
    )
    val label = options.firstOrNull { it.first == currentValue }?.second ?: "Not specified"

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { menuOpen = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Gender", color = Navy, modifier = Modifier.weight(1f))
            Text(label, color = Slate, fontSize = 13.sp)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = Slate.copy(alpha = 0.4f))
        }
        DropdownMenu(
            expanded         = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor   = Color.White,
        ) {
            options.forEach { (value, opLabel) ->
                DropdownMenuItem(
                    text    = { Text(opLabel) },
                    onClick = { onChange(value); menuOpen = false },
                )
            }
        }
    }
}

@Composable
private fun StepperControl(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        StepperBtn("−", enabled = value > min) { onChange(value - 1) }
        Text("$value", color = Navy, fontWeight = FontWeight.SemiBold, modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center)
        StepperBtn("+", enabled = value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepperBtn(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier         = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (enabled) SoftMint else Slate.copy(alpha = 0.1f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) Green else Slate.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SegmentedPickerRow(
    label        : String,
    options      : List<Pair<String, String>>,
    currentValue : String,
    onChange     : (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Navy, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Cream),
        ) {
            options.forEach { (value, optionLabel) ->
                val selected = value == currentValue
                Box(
                    modifier         = Modifier
                        .weight(1f)
                        .padding(3.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Green else Color.Transparent)
                        .clickable { onChange(value) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        optionLabel,
                        color      = if (selected) Color.White else Navy,
                        fontSize   = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Navy, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Green,
            ),
        )
    }
}

@Composable
private fun DestructiveActionRow(
    label  : String,
    icon   : androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color(0xFFD32F2F))
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color(0xFFD32F2F), fontWeight = FontWeight.Medium)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun dietaryCount(s: AccountState): Int {
    val d = s.dietaryTypes.filter { it.isNotBlank() && it.lowercase() != "none" }.size
    val r = s.foodRestrictions.filter { it.isNotBlank() && it.lowercase() != "none" }.size
    return d + r + s.avoidIngredients.size
}

private fun dietarySummary(types: Set<String>, restrictions: Set<String>, avoid: Set<String>): String {
    val total = types.filter { it.isNotBlank() && it.lowercase() != "none" }.size +
                restrictions.filter { it.isNotBlank() && it.lowercase() != "none" }.size +
                avoid.size
    return when (total) {
        0    -> "No preferences set"
        1    -> "1 preference set"
        else -> "$total preferences set"
    }
}

// ── Dietary editor sheet ─────────────────────────────────────────────────────

private val DIETARY_TYPES = listOf(
    "vegetarian"   to "Vegetarian",
    "vegan"        to "Vegan",
    "pescatarian"  to "Pescatarian",
    "keto"         to "Keto",
    "paleo"        to "Paleo",
    "mediterranean" to "Mediterranean",
)

private val FOOD_RESTRICTIONS = listOf(
    "gluten_free"     to "Gluten-Free",
    "nut_free"        to "Nut-Free",
    "dairy_free"      to "Dairy-Free",
    "shellfish_free"  to "Shellfish-Free",
    "egg_free"        to "Egg-Free",
    "soy_free"        to "Soy-Free",
    "pregnancy"       to "Pregnancy-Safe",
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun DietaryEditorSheet(
    initialTypes        : Set<String>,
    initialRestrictions : Set<String>,
    initialAvoid        : Set<String>,
    onDismiss           : () -> Unit,
    onSave              : (Set<String>, Set<String>, Set<String>) -> Unit,
) {
    val types        = remember { mutableStateListOf<String>().apply { addAll(initialTypes) } }
    val restrictions = remember { mutableStateListOf<String>().apply { addAll(initialRestrictions) } }
    val avoidList    = remember { mutableStateListOf<String>().apply { addAll(initialAvoid) } }
    var avoidInput   by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Cream) {
        Column(
            modifier            = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                "Dietary Preferences",
                style      = MaterialTheme.typography.headlineMedium,
                color      = Navy,
                fontWeight = FontWeight.SemiBold,
            )

            Text("DIETARY TYPE", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowChipGroup(
                options   = DIETARY_TYPES,
                selected  = types,
                onToggle  = { v -> if (types.contains(v)) types.remove(v) else types.add(v) },
            )

            Text("RESTRICTIONS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            FlowChipGroup(
                options   = FOOD_RESTRICTIONS,
                selected  = restrictions,
                onToggle  = { v -> if (restrictions.contains(v)) restrictions.remove(v) else restrictions.add(v) },
            )

            Text("AVOID INGREDIENTS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value         = avoidInput,
                    onValueChange = { avoidInput = it },
                    placeholder   = { Text("e.g. peanuts") },
                    singleLine    = true,
                    modifier      = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val v = avoidInput.trim()
                        if (v.isNotBlank() && !avoidList.any { it.equals(v, ignoreCase = true) }) {
                            avoidList.add(v); avoidInput = ""
                        }
                    },
                    colors  = ButtonDefaults.buttonColors(containerColor = Green),
                    enabled = avoidInput.isNotBlank(),
                ) { Text("Add") }
            }
            if (avoidList.isNotEmpty()) {
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    avoidList.toList().forEach { item ->
                        Surface(
                            shape    = RoundedCornerShape(16.dp),
                            color    = Color.White,
                            border   = BorderStroke(1.dp, Slate.copy(alpha = 0.3f)),
                            modifier = Modifier.clickable { avoidList.remove(item) },
                        ) {
                            Row(
                                modifier          = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item, color = Navy, fontSize = 13.sp)
                                Spacer(Modifier.width(6.dp))
                                Text("×", color = Slate, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Button(
                onClick  = { onSave(types.toSet(), restrictions.toSet(), avoidList.toSet()) },
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
            ) {
                Text("Save Preferences")
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowChipGroup(
    options  : List<Pair<String, String>>,
    selected : List<String>,
    onToggle : (String) -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (value, label) ->
            val on = selected.contains(value)
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = if (on) Green else Color.White,
                border   = BorderStroke(1.dp, if (on) Color.Transparent else Slate.copy(alpha = 0.3f)),
                modifier = Modifier.clickable { onToggle(value) },
            ) {
                Text(
                    label,
                    color      = if (on) Color.White else Navy,
                    fontSize   = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
