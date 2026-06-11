package com.souspantry.app.ui.plancook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.delay

// ── Constants ────────────────────────────────────────────────────────────────

private data class Mood(val label: String, val icon: ImageVector)

private val MOODS = listOf(
    Mood("Quick Meal",          Icons.Filled.Bolt),
    Mood("Dinner in 30 Mins",   Icons.Filled.AccessTime),
    Mood("Healthy Snacks",      Icons.Filled.Eco),
    Mood("Salads",              Icons.Filled.RestaurantMenu),
)

private val CUISINES = listOf(
    "American", "Australian", "British", "Chinese",
    "French", "Indian", "Indonesian", "Italian", "Japanese",
    "Korean", "Malaysian", "Mexican", "Middle Eastern",
    "South American", "Spanish", "Thai", "Turkish",
)

private data class LoadingFrame(val icon: ImageVector, val label: String)

private val LOADING_FRAMES = listOf(
    LoadingFrame(Icons.Filled.LocalFireDepartment, "Preheating the oven…"),
    LoadingFrame(Icons.Filled.Restaurant,          "Consulting the chef…"),
    LoadingFrame(Icons.Filled.ContentCut,          "Chopping the onions…"),
    LoadingFrame(Icons.Filled.Thermostat,          "Simmering suggestions…"),
    LoadingFrame(Icons.Filled.AutoAwesome,         "Sprinkling a pinch of magic…"),
    LoadingFrame(Icons.Filled.ShoppingCart,        "Raiding the pantry…"),
    LoadingFrame(Icons.Filled.MenuBook,            "Reading recipe books…"),
    LoadingFrame(Icons.Filled.AutoFixHigh,         "Whisking up something special…"),
)

// ── Screen ───────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PlanCookScreen(vm: PlanCookViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        PageHeader()
        PlanTabBar(
            selected = state.selectedTab,
            onSelect = { vm.selectTab(it) },
        )

        when (state.selectedTab) {
            PlanTab.RECIPES -> RecipesTab(state = state, vm = vm)
            PlanTab.MY_WEEK -> EmptyTabPlaceholder(
                emoji = "📅",
                title = "Plan your week",
                body  = "Schedule meals across the week — save and reuse plans, generate shopping lists from them.",
            )
            PlanTab.SAVED   -> EmptyTabPlaceholder(
                emoji = "🔖",
                title = "Save your favourites",
                body  = "Tap the bookmark on any meal to save it here for later.",
            )
        }
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun PageHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Plan & Cook", style = MaterialTheme.typography.headlineLarge, color = Navy)
        Text(
            "Sous Pantry suggested meals from your pantry",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            "Sous Pantry can make mistakes. Always verify before cooking.",
            color      = Slate.copy(alpha = 0.6f),
            fontSize   = 11.sp,
            fontStyle  = FontStyle.Italic,
            modifier   = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
    }
}

// ── Tab bar ──────────────────────────────────────────────────────────────────

@Composable
private fun PlanTabBar(selected: PlanTab, onSelect: (PlanTab) -> Unit) {
    val tabs = listOf(
        Triple(PlanTab.RECIPES, "Recipes", Icons.Filled.AutoAwesome),
        Triple(PlanTab.MY_WEEK, "My Week", Icons.Filled.CalendarMonth),
        Triple(PlanTab.SAVED,   "Saved",   Icons.Filled.Bookmark),
    )
    Column {
        Row(modifier = Modifier.fillMaxWidth().background(Cream)) {
            tabs.forEach { (tab, label, icon) ->
                val isOn = selected == tab
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelect(tab) }
                        .padding(vertical = 10.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (isOn) Navy else Slate.copy(alpha = 0.6f))
                        Text(
                            label,
                            color      = if (isOn) Navy else Slate.copy(alpha = 0.6f),
                            fontSize   = 14.sp,
                            fontWeight = if (isOn) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isOn) Green else Color.Transparent),
                    )
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))
    }
}

// ── Recipes tab ──────────────────────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RecipesTab(state: PlanCookState, vm: PlanCookViewModel) {
    when {
        state.loading -> GeneratingView(
            mood     = state.selectedMood,
            cuisines = state.selectedCuisines,
            onCancel = { vm.cancelGeneration() },
        )
        state.meals.isEmpty() -> LandingView(
            state          = state,
            onMoodSelect   = { vm.setMood(if (state.selectedMood == it) null else it) },
            onCuisineToggle = { vm.toggleCuisine(it) },
            onClearCuisines = { vm.clearCuisines() },
            onGenerate     = { vm.generate() },
        )
        else -> ResultsView(
            state            = state,
            onGenerateMore   = { vm.generate() },
            onToggleExpanded = { vm.toggleExpanded(it.title) },
            onReset          = { vm.reset() },
        )
    }
}

// ── Landing view (filter picker) ─────────────────────────────────────────────

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LandingView(
    state           : PlanCookState,
    onMoodSelect    : (String) -> Unit,
    onCuisineToggle : (String) -> Unit,
    onClearCuisines : () -> Unit,
    onGenerate      : () -> Unit,
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {

        // Filter card (mood + cuisines)
        Surface(
            modifier        = Modifier.fillMaxWidth().shadow(
                elevation     = 6.dp,
                shape         = RoundedCornerShape(16.dp),
                ambientColor  = Navy.copy(alpha = 0.07f),
                spotColor     = Navy.copy(alpha = 0.07f),
            ),
            shape           = RoundedCornerShape(16.dp),
            color           = Color.White,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                Text(
                    "WHAT ARE YOU IN THE MOOD FOR?",
                    color      = Slate,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    MOODS.forEach { mood ->
                        MoodChip(
                            label      = mood.label,
                            icon       = mood.icon,
                            isSelected = state.selectedMood == mood.label,
                            onClick    = { onMoodSelect(mood.label) },
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "FILTER BY CUISINE",
                        color      = Slate,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.weight(1f),
                    )
                    if (state.selectedCuisines.isNotEmpty()) {
                        TextButton(onClick = onClearCuisines, contentPadding = PaddingValues(0.dp)) {
                            Text("Clear", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    CUISINES.forEach { c ->
                        CuisineChip(
                            label      = c,
                            isSelected = state.selectedCuisines.contains(c),
                            onClick    = { onCuisineToggle(c) },
                        )
                    }
                }
                if (state.selectedCuisines.isNotEmpty()) {
                    Text(
                        "${state.selectedCuisines.size} cuisine${if (state.selectedCuisines.size == 1) "" else "s"} selected",
                        color    = Slate,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        // Error banner (if previous generate failed)
        state.error?.let { msg ->
            Surface(
                modifier        = Modifier.fillMaxWidth(),
                shape           = RoundedCornerShape(12.dp),
                color           = Color.White,
                shadowElevation = 2.dp,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(msg, color = Navy, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Check your connection and try again.", color = Slate, fontSize = 12.sp)
                }
            }
        }

        // CTA
        Button(
            onClick  = onGenerate,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Green),
            shape    = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate Meal Plan", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MoodChip(label: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = CircleShape,
        color    = if (isSelected) Navy else Cream,
        border   = BorderStroke(1.dp, if (isSelected) Color.Transparent else Slate.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, null, modifier = Modifier.size(14.dp), tint = if (isSelected) Color.White else Navy)
            Text(
                label,
                color      = if (isSelected) Color.White else Navy,
                fontSize   = 14.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun CuisineChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = CircleShape,
        color    = if (isSelected) Green else Cream,
        border   = BorderStroke(1.dp, if (isSelected) Color.Transparent else Slate.copy(alpha = 0.15f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            color      = if (isSelected) Color.White else Navy,
            fontSize   = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier   = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

// ── Generating view ──────────────────────────────────────────────────────────

@Composable
private fun GeneratingView(
    mood     : String?,
    cuisines : Set<String>,
    onCancel : () -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2500)
            step++
        }
    }
    val frame = LOADING_FRAMES[step % LOADING_FRAMES.size]

    Column(
        modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier         = Modifier.size(110.dp).clip(CircleShape).background(SoftMint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(frame.icon, null, tint = Green, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "Sous Pantry is cooking…",
            color      = Navy,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            frame.label,
            color     = Slate,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
        )
        if (mood != null || cuisines.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (mood != null) {
                    Surface(shape = CircleShape, color = Navy) {
                        Text(
                            mood,
                            color      = Color.White,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                cuisines.take(3).forEach { c ->
                    Surface(shape = CircleShape, color = Green) {
                        Text(
                            c,
                            color      = Color.White,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
                if (cuisines.size > 3) {
                    Text("+${cuisines.size - 3}", color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Surface(
            shape           = CircleShape,
            color           = Color.White,
            shadowElevation = 4.dp,
            modifier        = Modifier.clickable(onClick = onCancel),
        ) {
            Text(
                "Stop",
                color      = Slate,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(horizontal = 28.dp, vertical = 10.dp),
            )
        }
    }
}

// ── Results view (meal list) ─────────────────────────────────────────────────

@Composable
private fun ResultsView(
    state            : PlanCookState,
    onGenerateMore   : () -> Unit,
    onToggleExpanded : (SuggestedMeal) -> Unit,
    onReset          : () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Active filter chips summary row
        if (state.selectedMood != null || state.selectedCuisines.isNotEmpty()) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier              = Modifier.padding(horizontal = 4.dp),
            ) {
                state.selectedMood?.let {
                    Surface(shape = CircleShape, color = Navy) {
                        Text(it, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                state.selectedCuisines.take(3).forEach { c ->
                    Surface(shape = CircleShape, color = Green) {
                        Text(c, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                if (state.selectedCuisines.size > 3) {
                    Text("+${state.selectedCuisines.size - 3}", color = Green, fontSize = 11.sp)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onReset, contentPadding = PaddingValues(0.dp)) {
                    Text("Reset", color = Slate, fontSize = 12.sp)
                }
            }
        }

        state.meals.forEach { meal ->
            MealCard(
                meal       = meal,
                isExpanded = state.expandedMealId == meal.title,
                onTap      = { onToggleExpanded(meal) },
            )
        }

        Button(
            onClick  = onGenerateMore,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = Green),
            shape    = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate More", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun MealCard(meal: SuggestedMeal, isExpanded: Boolean, onTap: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation     = 8.dp,
                shape         = RoundedCornerShape(14.dp),
                ambientColor  = Navy.copy(alpha = 0.08f),
                spotColor     = Navy.copy(alpha = 0.08f),
            )
            .clickable(onClick = onTap),
        shape  = RoundedCornerShape(14.dp),
        color  = Color.White,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: cuisine + difficulty
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    meal.cuisine.uppercase(),
                    color      = Slate,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                )
                DifficultyBadge(meal.difficulty)
            }
            // Title
            Text(meal.title, color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            // Description
            Text(meal.description, color = Slate, fontSize = 13.sp, maxLines = if (isExpanded) Int.MAX_VALUE else 2)

            // Timing row
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Filled.AccessTime, null, tint = Slate, modifier = Modifier.size(12.dp))
                Text(meal.prepTime, color = Slate, fontSize = 11.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.AutoAwesome, null, tint = Green.copy(alpha = 0.75f), modifier = Modifier.size(11.dp))
                Text("Sous Pantry", color = Green.copy(alpha = 0.75f), fontSize = 11.sp)
            }

            // Expanded body
            if (isExpanded) {
                HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))
                if (meal.ingredients.isNotEmpty()) {
                    Text("INGREDIENTS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    meal.ingredients.forEach { Text("• $it", color = Navy, fontSize = 13.sp) }
                }
                if (meal.instructions.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text("INSTRUCTIONS", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    meal.instructions.forEachIndexed { i, step ->
                        Text("${i + 1}. $step", color = Navy, fontSize = 13.sp)
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))

            // Action buttons
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape    = CircleShape,
                    color    = Green,
                    modifier = Modifier.clickable { onTap() },
                ) {
                    Text(
                        if (isExpanded) "Cooking…" else "Cook This",
                        color      = Color.White,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
                Surface(
                    shape    = CircleShape,
                    color    = Cream,
                    border   = BorderStroke(1.dp, Slate.copy(alpha = 0.2f)),
                    modifier = Modifier.clickable { /* TODO: Add to my week — premium */ },
                ) {
                    Text(
                        "Add to my week",
                        color      = Navy,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: String) {
    val color = when (difficulty.lowercase()) {
        "easy"   -> Green
        "medium" -> Gold
        "hard"   -> Color(0xFFD32F2F)
        else     -> Slate
    }
    Surface(shape = CircleShape, color = color) {
        Text(
            difficulty,
            color      = Color.White,
            fontSize   = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ── Empty placeholder for My Week / Saved ────────────────────────────────────

@Composable
private fun EmptyTabPlaceholder(emoji: String, title: String, body: String) {
    Box(
        modifier         = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(emoji, fontSize = 56.sp)
            Text(title, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                color     = Slate,
                fontSize  = 13.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                "Coming soon",
                color      = Green,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(top = 8.dp),
            )
        }
    }
}
