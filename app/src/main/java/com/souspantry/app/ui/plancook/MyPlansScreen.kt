package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MyPlansScreen(
    pantryItems : List<PantryItem> = emptyList(),
    vm          : MyPlansViewModel = hiltViewModel(),
) {
    val state      by vm.state.collectAsState()
    val weekDates   = vm.weekDates()

    var showAddSheet         by remember { mutableStateOf<LocalDate?>(null) }
    var showMoveSheet        by remember { mutableStateOf<WeekMealEntry?>(null) }
    var showDetailSheet      by remember { mutableStateOf<WeekMealEntry?>(null) }
    var pendingDelete        by remember { mutableStateOf<WeekMealEntry?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    if (dragAmount < -50) vm.nextWeek()
                    else if (dragAmount > 50) vm.prevWeek()
                }
            },
    ) {
        // ── Week navigation header ─────────────────────────────────────────
        WeekNavHeader(
            weekDates  = weekDates,
            onPrevWeek = { vm.prevWeek() },
            onNextWeek = { vm.nextWeek() },
        )

        if (state.entries.isEmpty()) {
            // ── Empty state ────────────────────────────────────────────────
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.padding(horizontal = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("📅", fontSize = 56.sp)
                    Text(
                        "No meals planned yet",
                        color      = Navy,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Plan your week by adding meals from recipes. Tap the calendar icon to schedule them.",
                        color     = Slate,
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // ── 7-day calendar list ────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekDates.forEach { date ->
                    val dayEntries = state.entries
                        .filter { it.scheduledDate == date }
                        .sortedBy { it.title }

                    if (dayEntries.isEmpty()) {
                        EmptyDayRow(
                            date    = date,
                            onClick = { showAddSheet = date },
                        )
                    } else {
                        dayEntries.forEach { entry ->
                            MealEntryRow(
                                entry     = entry,
                                onView    = { showDetailSheet = entry },
                                onMove    = { showMoveSheet   = entry },
                                onRemove  = { pendingDelete   = entry },
                            )
                        }
                        AddMoreRow(
                            date    = date,
                            onClick = { showAddSheet = date },
                        )
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // ── Sheets ─────────────────────────────────────────────────────────────

    showAddSheet?.let { date ->
        AddCustomMealSheet(
            date      = date,
            onDismiss = { showAddSheet = null },
            onSave    = { name -> vm.addCustomMeal(name, date); showAddSheet = null },
        )
    }

    showMoveSheet?.let { entry ->
        MoveToOtherDaySheet(
            entry     = entry,
            onDismiss = { showMoveSheet = null },
            onMove    = { newDate -> vm.moveEntry(entry.id, newDate); showMoveSheet = null },
        )
    }

    showDetailSheet?.let { entry ->
        MealDetailSheet(
            entry        = entry,
            pantryItems  = pantryItems,
            onDismiss    = { showDetailSheet = null },
            onRemove     = { pendingDelete = entry; showDetailSheet = null },
            onMarkCooked = { checked -> vm.markCooked(entry, checked); showDetailSheet = null },
            onAddMissing = { missing -> vm.addMissingToShopping(missing) },
        )
    }

    pendingDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            confirmButton    = {
                TextButton(onClick = { vm.removeEntry(entry.id); pendingDelete = null }) {
                    Text("Remove", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton    = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
            title            = { Text("Remove \"${entry.title}\"?") },
            text             = { Text("This meal will be removed from your week.") },
            containerColor   = Color.White,
        )
    }
}

// ── Week navigation header ────────────────────────────────────────────────────

@Composable
private fun WeekNavHeader(
    weekDates  : List<LocalDate>,
    onPrevWeek : () -> Unit,
    onNextWeek : () -> Unit,
) {
    val fmt     = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val fmtFull = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    val first   = weekDates.firstOrNull()
    val last    = weekDates.lastOrNull()
    val title   = if (first != null && last != null)
        "${fmt.format(first)} – ${fmtFull.format(last)}" else ""

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevWeek) {
                Icon(Icons.Filled.ChevronLeft, "Previous week", tint = Navy)
            }
            Text(
                title,
                color      = Navy,
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.weight(1f),
            )
            IconButton(onClick = onNextWeek) {
                Icon(Icons.Filled.ChevronRight, "Next week", tint = Navy)
            }
        }
    }
}

// ── Date column (shared between row types) ────────────────────────────────────

@Composable
private fun DateColumn(date: LocalDate, dimmed: Boolean = false) {
    val isToday = date == LocalDate.now()
    val alpha   = if (dimmed) 0.5f else 1.0f

    Column(
        modifier          = Modifier.width(60.dp).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            color      = Slate.copy(alpha = alpha),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
        )
        Text(
            "${date.dayOfMonth}",
            color      = if (isToday) Green else Navy.copy(alpha = alpha),
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
            color      = Slate.copy(alpha = alpha),
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun VerticalDivider(dimmed: Boolean = false) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(48.dp)
            .background(Color(0xFFE6E1DC).copy(alpha = if (dimmed) 0.5f else 1.0f)),
    )
}

// ── Empty day row ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyDayRow(date: LocalDate, onClick: () -> Unit) {
    val isPast = date.isBefore(LocalDate.now())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = !isPast, onClick = onClick),
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White.copy(alpha = if (isPast) 0.5f else 1.0f),
        shadowElevation = if (isPast) 0.dp else 2.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            DateColumn(date, dimmed = isPast)
            VerticalDivider(dimmed = isPast)
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.AddCircleOutline, null,
                    tint     = Green.copy(alpha = if (isPast) 0.3f else 0.6f),
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "Add meal",
                    color      = Slate.copy(alpha = if (isPast) 0.3f else 0.5f),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Meal entry row ────────────────────────────────────────────────────────────

@Composable
private fun MealEntryRow(
    entry   : WeekMealEntry,
    onView  : () -> Unit,
    onMove  : () -> Unit,
    onRemove: () -> Unit,
) {
    val isPast  = entry.scheduledDate.isBefore(LocalDate.now())
    val alpha   = if (isPast) 0.6f else 1.0f
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White,
        shadowElevation = 3.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            DateColumn(entry.scheduledDate, dimmed = isPast)
            VerticalDivider(dimmed = isPast)

            // ── Recipe details ─────────────────────────────────────────────
            Column(
                modifier            = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.title,
                            color      = Navy.copy(alpha = alpha),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines   = 2,
                        )
                        if (!entry.isUserAdded) {
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (entry.cuisine.isNotBlank())    TagPill(entry.cuisine,    alpha)
                                if (entry.difficulty.isNotBlank()) TagPill(entry.difficulty, alpha)
                            }
                        }
                    }

                    // ⋯ menu
                    Box {
                        IconButton(
                            onClick  = { menuOpen = true },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(Icons.Filled.MoreVert, "More", tint = Slate.copy(alpha = 0.6f))
                        }
                        DropdownMenu(
                            expanded         = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor   = Color.White,
                        ) {
                            if (!entry.isUserAdded) {
                                DropdownMenuItem(
                                    text        = { Text("View Recipe") },
                                    leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = Navy) },
                                    onClick     = { menuOpen = false; onView() },
                                )
                            }
                            DropdownMenuItem(
                                text        = { Text("Move to Other Day") },
                                leadingIcon = { Icon(Icons.Filled.CalendarMonth, null, tint = Navy) },
                                onClick     = { menuOpen = false; onMove() },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text        = { Text("Remove", color = Color(0xFFD32F2F)) },
                                leadingIcon = { Icon(Icons.Filled.Remove, null, tint = Color(0xFFD32F2F)) },
                                onClick     = { menuOpen = false; onRemove() },
                            )
                        }
                    }
                }

                // Timing row
                if (entry.prepTime.isNotBlank() || entry.cookTime.isNotBlank()) {
                    TimingRow(
                        prepTime = entry.prepTime,
                        cookTime = entry.cookTime,
                        alpha    = alpha,
                    )
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, alpha: Float = 1f) {
    Text(
        text,
        color      = Navy.copy(alpha = alpha),
        fontSize   = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier
            .clip(CircleShape)
            .background(Cream)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun TimingRow(prepTime: String, cookTime: String, alpha: Float = 1f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            prepTime.isNotBlank() && cookTime.isNotBlank() -> {
                Text("Prep $prepTime", color = Slate.copy(alpha = alpha), fontSize = 11.sp)
                Text("  ·  ", color = Slate.copy(alpha = alpha * 0.5f), fontSize = 11.sp)
                Text("Cook $cookTime", color = Slate.copy(alpha = alpha), fontSize = 11.sp)
            }
            prepTime.isNotBlank() -> Text("Time $prepTime", color = Slate.copy(alpha = alpha), fontSize = 11.sp)
            cookTime.isNotBlank() -> Text("Time $cookTime", color = Slate.copy(alpha = alpha), fontSize = 11.sp)
        }
    }
}

// ── Add-more row ──────────────────────────────────────────────────────────────

@Composable
private fun AddMoreRow(date: LocalDate, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape           = RoundedCornerShape(12.dp),
        color           = Color.White.copy(alpha = 0.45f),
        shadowElevation = 0.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            // Dimmed date column
            Column(
                modifier          = Modifier.width(60.dp).padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    color = Slate.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Text("${date.dayOfMonth}",
                    color = Navy.copy(alpha = 0.25f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    color = Slate.copy(alpha = 0.3f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFE6E1DC).copy(alpha = 0.5f)))
            Row(
                modifier              = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.AddCircleOutline, null, tint = Green.copy(alpha = 0.55f), modifier = Modifier.size(14.dp))
                Text("Add another meal", color = Green.copy(alpha = 0.55f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Meal detail bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailSheet(
    entry         : WeekMealEntry,
    pantryItems   : List<PantryItem>,
    onDismiss     : () -> Unit,
    onRemove      : () -> Unit,
    onMarkCooked  : (checkedIngredients: List<String>) -> Unit,
    onAddMissing  : (missing: List<String>) -> Unit,
) {
    val ingredientRows = remember(entry, pantryItems) {
        entry.ingredients.map { ing ->
            val inPantry = pantryItems.any {
                it.name.contains(ing, ignoreCase = true) || ing.contains(it.name, ignoreCase = true)
            }
            ing to inPantry
        }
    }
    val pantryCount     = ingredientRows.count { it.second }
    val missingItems    = ingredientRows.filter { !it.second }.map { it.first }
    val checkedItems    = remember { mutableStateListOf<String>().also { list ->
        ingredientRows.filter { it.second }.forEach { list.add(it.first) }
    }}
    var addedToShopping by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom     = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Header card ─────────────────────────────────────────────────
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(14.dp),
                color           = Color.White,
                shadowElevation = 4.dp,
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            entry.cuisine.uppercase(),
                            color      = Slate,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.weight(1f),
                        )
                        if (entry.difficulty.isNotBlank()) {
                            Surface(shape = CircleShape, color = difficultyColor(entry.difficulty)) {
                                Text(
                                    entry.difficulty,
                                    color      = Color.White,
                                    fontSize   = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Column(
                        modifier            = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (entry.description.isNotBlank()) {
                            Text(entry.description, color = Slate, fontSize = 14.sp)
                        }
                        TimingRow(entry.prepTime, entry.cookTime)
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Green.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                            Text(
                                "$pantryCount of ${entry.ingredients.size} ingredients in pantry",
                                color    = Slate,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }

            // ── Ingredients ─────────────────────────────────────────────────
            if (entry.ingredients.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("INGREDIENTS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                        if (missingItems.isNotEmpty() && !addedToShopping) {
                            TextButton(
                                onClick        = { onAddMissing(missingItems); addedToShopping = true },
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
                        shape           = RoundedCornerShape(14.dp),
                        color           = Color.White,
                        shadowElevation = 3.dp,
                    ) {
                        Column {
                            ingredientRows.forEachIndexed { idx, (ing, inPantry) ->
                                val isChecked = checkedItems.contains(ing)
                                Row(
                                    modifier          = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = inPantry) {
                                            if (isChecked) checkedItems.remove(ing)
                                            else checkedItems.add(ing)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = if (inPantry && isChecked) Icons.Filled.CheckCircle
                                                      else if (inPantry)         Icons.Filled.RadioButtonUnchecked
                                                      else                       Icons.Filled.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint               = when {
                                            inPantry && isChecked -> Green
                                            inPantry              -> Slate.copy(alpha = 0.35f)
                                            else                  -> Slate.copy(alpha = 0.35f)
                                        },
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Text(
                                        ing,
                                        color    = if (inPantry && isChecked) Slate else Navy,
                                        fontSize = 15.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        when {
                                            inPantry && isChecked -> "In pantry"
                                            inPantry             -> "Excluded"
                                            else                 -> "Needed"
                                        },
                                        color      = when {
                                            inPantry && isChecked -> Green
                                            inPantry             -> Slate.copy(alpha = 0.5f)
                                            else                 -> Gold
                                        },
                                        fontSize   = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                if (idx < ingredientRows.lastIndex) {
                                    HorizontalDivider(
                                        modifier  = Modifier.padding(start = 46.dp),
                                        thickness = 0.5.dp,
                                        color     = Slate.copy(alpha = 0.12f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Instructions ─────────────────────────────────────────────────
            if (entry.instructions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HOW TO COOK", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Surface(
                        modifier        = Modifier.fillMaxWidth(),
                        shape           = RoundedCornerShape(14.dp),
                        color           = Color.White,
                        shadowElevation = 3.dp,
                    ) {
                        Column {
                            entry.instructions.forEachIndexed { idx, step ->
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
                                if (idx < entry.instructions.lastIndex) {
                                    HorizontalDivider(
                                        modifier  = Modifier.padding(start = 48.dp),
                                        thickness = 0.5.dp,
                                        color     = Slate.copy(alpha = 0.12f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Action buttons ────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Mark as Cooked — deducts the checked in-pantry ingredients and
                // removes the meal from the week.
                Button(
                    onClick  = { onMarkCooked(checkedItems.toList()); onDismiss() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Green),
                    shape    = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as Cooked", color = Color.White, fontWeight = FontWeight.SemiBold)
                }

                // Remove
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onRemove),
                    shape  = RoundedCornerShape(12.dp),
                    color  = Color(0xFFD32F2F).copy(alpha = 0.07f),
                ) {
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Remove, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove from My Plans", color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Add Custom Meal bottom sheet ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomMealSheet(
    date      : LocalDate,
    onDismiss : () -> Unit,
    onSave    : (String) -> Unit,
) {
    val fmt       = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())
    val dateLabel = fmt.format(date)
    var mealName  by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        val dialogView = LocalView.current
        DisposableEffect(dialogView) {
            val window = (dialogView.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
            window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            onDispose {}
        }
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add Meal", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(dateLabel, color = Slate, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }
            Surface(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(12.dp),
                color     = Cream,
            ) {
                TextField(
                    value           = mealName,
                    onValueChange   = { mealName = it },
                    placeholder     = { Text("e.g. Frozen Japanese Curry", color = Slate.copy(alpha = 0.5f)) },
                    singleLine      = true,
                    modifier        = Modifier.fillMaxWidth(),
                    colors          = TextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor   = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor   = Color.Transparent,
                    ),
                )
            }
            Button(
                onClick  = { if (mealName.isNotBlank()) onSave(mealName) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (mealName.isNotBlank()) Green else Slate.copy(alpha = 0.3f),
                ),
                enabled  = mealName.isNotBlank(),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Move to Other Day bottom sheet ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveToOtherDaySheet(
    entry     : WeekMealEntry,
    onDismiss : () -> Unit,
    onMove    : (LocalDate) -> Unit,
) {
    var selectedDate by remember { mutableStateOf(entry.scheduledDate) }
    val state        = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli(),
    )
    val fmt       = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())

    // Sync DatePicker selection back to LocalDate
    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { ms ->
            selectedDate = java.time.Instant.ofEpochMilli(ms)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Move to Another Day", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(entry.title, color = Slate, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(8.dp))
            DatePicker(
                state  = state,
                colors = DatePickerDefaults.colors(selectedDayContainerColor = Green),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { onMove(selectedDate) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Text("Move to ${fmt.format(selectedDate)}", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Add to My Week bottom sheet (from a Discover recipe) ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToWeekSheet(
    meal      : com.souspantry.app.data.models.SuggestedMeal,
    onDismiss : () -> Unit,
    onAdd     : (LocalDate) -> Unit,
) {
    val todayMs = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = todayMs,
        // Only allow today and future dates.
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMs - 86_400_000L
        },
    )
    val fmt = DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())

    LaunchedEffect(state.selectedDateMillis) {
        state.selectedDateMillis?.let { ms ->
            selectedDate = java.time.Instant.ofEpochMilli(ms)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val isToday    = selectedDate == LocalDate.now()
    val saveLabel  = if (isToday) "Add for Today" else "Add for ${fmt.format(selectedDate)}"

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Add to My Plans", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(meal.title, color = Slate, fontSize = 12.sp, maxLines = 1)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }
            Spacer(Modifier.height(8.dp))
            DatePicker(
                state  = state,
                colors = DatePickerDefaults.colors(selectedDayContainerColor = Green),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { onAdd(selectedDate) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                shape    = RoundedCornerShape(14.dp),
            ) {
                Text(saveLabel, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun difficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "easy"   -> Color(0xFF2D5A3D)
    "medium" -> Color(0xFFC4965A)
    "hard"   -> Color(0xFFD32F2F)
    else     -> Color(0xFF647080)
}
