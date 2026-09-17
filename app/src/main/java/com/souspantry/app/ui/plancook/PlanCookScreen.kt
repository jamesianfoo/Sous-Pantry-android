package com.souspantry.app.ui.plancook

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.services.BuyList
import com.souspantry.app.services.IngredientScaler
import com.souspantry.app.services.IngredientStaples
import com.souspantry.app.services.RecipeLinkResolver
import com.souspantry.app.ui.theme.*

// ── Constants ────────────────────────────────────────────────────────────────

private val MOOD_OPTIONS = listOf(
    "Quick & Easy", "Comfort Food", "Light & Healthy",
    "Post-workout 💪", "Fancy", "Impress Someone", "Surprise Me 🎲",
)

@Composable
fun PlanCookScreen(
    vm        : PlanCookViewModel     = hiltViewModel(),
    plansVm   : MyPlansViewModel      = hiltViewModel(),
    savedVm   : SavedRecipesViewModel = hiltViewModel(),
) {
    val state    by vm.state.collectAsState()
    val savedState by savedVm.state.collectAsState()
    val historySessions by vm.historySessions.collectAsState()
    var addToWeekMeal by remember { mutableStateOf<SuggestedMeal?>(null) }
    var cookingMeal    by remember { mutableStateOf<SuggestedMeal?>(null) }
    var showHistory    by remember { mutableStateOf(false) }
    // External recipe page (pasted URL / AI card with a source) opened in-app.
    var webRecipe      by remember { mutableStateOf<Triple<String, String, List<String>>?>(null) }
    val uriHandler     = androidx.compose.ui.platform.LocalUriHandler.current

    /** Cards with a link open the page; purely-AI cards open the cooking sheet. */
    fun openRecipe(meal: SuggestedMeal) {
        if (!vm.isExternal(meal)) { cookingMeal = meal; return }
        vm.resolveLink(meal) { link ->
            when (link) {
                is com.souspantry.app.services.RecipeLink.Direct ->
                    webRecipe = Triple(link.url, meal.title, link.ingredients)
                is com.souspantry.app.services.RecipeLink.Search ->
                    runCatching { uriHandler.openUri(link.url) }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Beige)) {
        Header(
            showHistoryActions = state.selectedTab == PlanTab.DISCOVER,
            onHistoryTap       = { showHistory = true },
            onStartOver        = { vm.startOver() },
        )
        PillTabBar(selected = state.selectedTab, isPremium = state.isPremium, onSelect = vm::selectTab)

        when (state.selectedTab) {
            PlanTab.DISCOVER      -> DiscoverTab(
                state          = state,
                vm             = vm,
                onAddToWeek    = { addToWeekMeal = it },
                onCook         = { openRecipe(it) },
                isSaved        = { savedState.recipes.any { r -> r.title.equals(it.title.trim(), ignoreCase = true) } },
                onToggleSaved  = { savedVm.toggle(it) },
            )
            PlanTab.SAVED_RECIPES -> if (state.isPremium) SavedRecipesScreen(vm = savedVm)
                                    else PremiumLockState(feature = "Saved Recipes",
                                        description = "Bookmark your favourite recipes and access them anytime.")
            PlanTab.MY_RECIPES    -> if (state.isPremium) MyRecipesScreen()
                                    else PremiumLockState(feature = "My Recipes",
                                        description = "Edit, personalise, and save your own versions of any recipe.")
            PlanTab.MY_PLANS      -> if (state.isPremium) MyPlansScreen(pantryItems = state.pantryItems, vm = plansVm)
                                    else PremiumLockState(feature = "My Plans",
                                        description = "Schedule meals for the whole week with AI-powered planning.")
        }
    }

    // Add-to-week date picker, reachable from any Discover recipe card.
    addToWeekMeal?.let { meal ->
        AddToWeekSheet(
            meal      = meal,
            onDismiss = { addToWeekMeal = null },
            onAdd     = { date ->
                plansVm.addFromMeal(meal, date)
                addToWeekMeal = null
                vm.selectTab(PlanTab.MY_PLANS)
            },
        )
    }

    // Cooking session, reachable by tapping a Discover recipe card or a History entry.
    cookingMeal?.let { meal ->
        CookingSessionSheet(
            meal         = meal,
            pantryItems  = state.pantryItems,
            isSaved      = savedState.recipes.any { it.title.equals(meal.title.trim(), ignoreCase = true) },
            onDismiss    = { cookingMeal = null },
            onSave       = { savedVm.toggle(meal) },
            onCooked     = { checked -> vm.markCooked(checked); cookingMeal = null },
            onAddMissing = { missing -> vm.addMissingToShopping(missing, meal.title) },
        )
    }

    webRecipe?.let { (url, title, ingredients) ->
        RecipeWebSheet(
            url         = url,
            title       = title,
            ingredients = ingredients,
            onDismiss   = { webRecipe = null },
            onCooked    = { used -> vm.markCooked(used) },
        )
    }

    // Past Discover generations.
    if (showHistory) {
        MealHistorySheet(
            sessions     = historySessions,
            onDismiss    = { showHistory = false },
            onClearAll   = { vm.clearHistory() },
            onViewRecipe = { meal -> showHistory = false; openRecipe(meal) },
        )
    }
}

// ── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun Header(
    showHistoryActions : Boolean,
    onHistoryTap       : () -> Unit,
    onStartOver        : () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Plan & Cook",
                style      = MaterialTheme.typography.headlineLarge,
                color      = Navy,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f),
            )
            if (showHistoryActions) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier              = Modifier.clickable(onClick = onHistoryTap),
                    ) {
                        Icon(Icons.Filled.History, null, tint = Green, modifier = Modifier.size(14.dp))
                        Text("History", color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        "Start over",
                        color      = Navy,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier.clickable(onClick = onStartOver),
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Sous is AI-powered and can make mistakes. Always verify recipes before cooking.",
            color    = Slate.copy(alpha = 0.7f),
            fontSize = 11.sp,
        )
    }
}

// ── Pill tab bar (4 tabs) ────────────────────────────────────────────────────

@Composable
private fun PillTabBar(selected: PlanTab, isPremium: Boolean, onSelect: (PlanTab) -> Unit) {
    val tabs = listOf(
        PlanTab.DISCOVER      to "Discover",
        PlanTab.SAVED_RECIPES to "Saved Recipes",
        PlanTab.MY_RECIPES    to "My Recipes",
        PlanTab.MY_PLANS      to "My Plans",
    )
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { (tab, label) ->
            val isOn   = tab == selected
            val locked = tab != PlanTab.DISCOVER && !isPremium
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isOn) Green else Color.Transparent)
                    .clickable { onSelect(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (locked) {
                        Icon(
                            Icons.Filled.Lock, null,
                            tint     = if (isOn) Color.White else Slate,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                    Text(
                        label,
                        color      = if (isOn) Color.White else Navy,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                    )
                }
            }
        }
    }
}

// ── Discover tab (chat) ──────────────────────────────────────────────────────

@Composable
private fun DiscoverTab(
    state         : PlanCookState,
    vm            : PlanCookViewModel,
    onAddToWeek   : (SuggestedMeal) -> Unit,
    onCook        : (SuggestedMeal) -> Unit,
    isSaved       : (SuggestedMeal) -> Boolean,
    onToggleSaved : (SuggestedMeal) -> Unit,
) {
    val listState = rememberLazyListState()
    var toBuyMeal by remember { mutableStateOf<SuggestedMeal?>(null) }

    // Auto-scroll to bottom whenever a new message arrives
    LaunchedEffect(state.messages.size, state.isLoading) {
        val target = (state.messages.size - 1 + if (state.isLoading) 1 else 0).coerceAtLeast(0)
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(target)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier            = Modifier.fillMaxWidth().weight(1f),
            state               = listState,
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(state.messages.size, key = { state.messages[it].id }) { idx ->
                MessageRow(
                    message       = state.messages[idx],
                    onAddToWeek   = onAddToWeek,
                    onCook        = onCook,
                    isSaved       = isSaved,
                    onToggleSaved = onToggleSaved,
                    onToBuy       = { toBuyMeal = it },
                )
            }
            if (state.isLoading) {
                item(key = "loading") { LoadingBubble() }
            }
        }
        Composer(
            inputText      = state.inputText,
            selectedMoods  = state.selectedMoods,
            canSend        = state.inputText.isNotBlank() && !state.isLoading,
            onInputChange  = { vm.setInputText(it) },
            onMoodToggle   = { vm.toggleMood(it) },
            onSend         = { vm.sendMessage() },
        )
    }

    // "What to buy" diffs the page's real scraped ingredients against the pantry;
    // the AI's guessed list is only the fallback for pages without structured data.
    toBuyMeal?.let { meal ->
        val scraped by produceState<List<String>?>(null, meal) { value = vm.scrapedIngredients(meal) }
        val real = scraped?.takeIf { it.isNotEmpty() }
            ?.let { BuyList.fromScraped(it, state.pantryItems.map { item -> item.name }) }
        val aiList = remember(meal) { aiMissing(meal) }
        WhatToBuySheet(
            recipeTitle  = meal.title,
            matchPercent = real?.matchPercent ?: aiMatchPercent(meal, aiList),
            missing      = real?.missing ?: aiList,
            loading      = scraped == null,
            onDismiss    = { toBuyMeal = null },
            onAdd        = { vm.addMissingToShopping(it, meal.title) },
        )
    }
}

// ── Message rendering ────────────────────────────────────────────────────────

@Composable
private fun MessageRow(
    message       : ChatMessage,
    onAddToWeek   : (SuggestedMeal) -> Unit,
    onCook        : (SuggestedMeal) -> Unit,
    isSaved       : (SuggestedMeal) -> Boolean,
    onToggleSaved : (SuggestedMeal) -> Unit,
    onToBuy       : (SuggestedMeal) -> Unit,
) {
    when (message) {
        is ChatMessage.Text -> when (message.role) {
            ChatRole.ASSISTANT -> AssistantTextBubble(text = message.content)
            ChatRole.USER      -> UserBubble(text = message.content)
        }
        is ChatMessage.RecipeList -> AssistantRecipeListBubble(
            recipes       = message.recipes,
            onAddToWeek   = onAddToWeek,
            onCook        = onCook,
            isSaved       = isSaved,
            onToggleSaved = onToggleSaved,
            onToBuy       = onToBuy,
        )
    }
}

@Composable
private fun AssistantAvatar() {
    Box(
        modifier         = Modifier.size(34.dp).clip(CircleShape).background(Green.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Eco, null, tint = Green, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AssistantTextBubble(text: String) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistantAvatar()
        Surface(
            modifier = Modifier.weight(1f),
            shape    = RoundedCornerShape(20.dp),
            color    = Color.White,
            shadowElevation = 1.dp,
        ) {
            Text(
                text,
                color    = Navy,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape           = RoundedCornerShape(20.dp),
            color           = Navy,
            shadowElevation = 1.dp,
            modifier        = Modifier.padding(start = 48.dp),
        ) {
            Text(
                text,
                color    = Color.White,
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}

/** No avatar, no bubble — cards float directly on the chat background, mirroring iOS. */
@Composable
private fun AssistantRecipeListBubble(
    recipes       : List<SuggestedMeal>,
    onAddToWeek   : (SuggestedMeal) -> Unit,
    onCook        : (SuggestedMeal) -> Unit,
    isSaved       : (SuggestedMeal) -> Boolean,
    onToggleSaved : (SuggestedMeal) -> Unit,
    onToBuy       : (SuggestedMeal) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(recipes) { recipe ->
            AiRecipeCard(
                recipe        = recipe,
                isSaved       = isSaved(recipe),
                onToggleSaved = { onToggleSaved(recipe) },
                onAddToWeek   = { onAddToWeek(recipe) },
                onViewRecipe  = { onCook(recipe) },
                onToBuy       = { onToBuy(recipe) },
            )
        }
    }
}

/**
 * The AI's estimate of what a recipe still needs. Staples are always assumed on
 * hand: never "needed", and the match reaches 100% once every non-staple is in
 * the pantry.
 */
private fun aiMissing(recipe: SuggestedMeal): List<String> =
    recipe.ingredients.filter { ing ->
        !IngredientStaples.isPantryStaple(ing) &&
            recipe.usedPantryItems.none { used -> ing.contains(used, ignoreCase = true) }
    }

private fun aiMatchPercent(recipe: SuggestedMeal, missing: List<String>): Int =
    if (recipe.ingredients.isEmpty()) 0
    else ((recipe.ingredients.size - missing.size) * 100) / recipe.ingredients.size

/** Mirrors iOS's dark recipe card: pantry match, missing ingredients, and a View Recipe action. */
@Composable
private fun AiRecipeCard(
    recipe        : SuggestedMeal,
    isSaved       : Boolean,
    onToggleSaved : () -> Unit,
    onAddToWeek   : () -> Unit,
    onViewRecipe  : () -> Unit,
    onToBuy       : () -> Unit,
) {
    val missing      = remember(recipe) { aiMissing(recipe) }
    val matchPercent = remember(recipe) { aiMatchPercent(recipe, missing) }
    val matchColor = if (matchPercent >= 85) Green else Gold

    Surface(
        modifier = Modifier.width(260.dp),
        shape    = RoundedCornerShape(24.dp), // DS r-xl — recipe cards
        color    = Navy,
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (recipe.cuisine.isNotBlank()) CardPill(recipe.cuisine)
                if (recipe.difficulty.isNotBlank()) { Spacer(Modifier.width(6.dp)); CardPill(recipe.difficulty) }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleSaved, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector        = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isSaved) "Saved" else "Save recipe",
                        tint               = if (isSaved) Gold else Color.White.copy(alpha = 0.7f),
                        modifier           = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onAddToWeek, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.CalendarMonth, "Add to my week", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }

            Text(
                recipe.title,
                color      = Color.White,
                fontSize   = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines   = 2,
            )

            // Filled chip, not just colored text — matches the iOS reference.
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(matchColor.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = if (matchPercent >= 85) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint     = matchColor,
                    modifier = Modifier.size(13.dp),
                )
                Text("$matchPercent% pantry match", color = matchColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (missing.isNotEmpty()) {
                Text(
                    "Need: ${missing.take(2).joinToString(", ") { IngredientScaler.cleanName(it) }}${if (missing.size > 2) " +${missing.size - 2} more" else ""}",
                    color    = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }

            if (recipe.description.isNotBlank()) {
                Text(
                    recipe.description,
                    color    = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    maxLines = 2,
                )
            }

            Spacer(Modifier.weight(1f))

            // Serves / Prep / Cook — stacked label-over-value, matching iOS.
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CardStat("Serves", "${recipe.servings}")
                if (recipe.prepTime.isNotBlank()) CardStat("Prep", recipe.prepTime)
                if (recipe.cookTime.isNotBlank()) CardStat("Cook", recipe.cookTime)
            }

            // Credit the real source when the recipe came from a website; only
            // recipes Sous actually composed are badged "Sous AI".
            val sourceSite = recipe.sourceSite.takeIf { it.isNotBlank() }
                ?: RecipeLinkResolver.host(recipe.sourceURL).takeIf { it.isNotBlank() }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    if (sourceSite != null) Icons.Filled.Language else Icons.Filled.AutoAwesome,
                    null, tint = Green, modifier = Modifier.size(12.dp),
                )
                Text(
                    sourceSite ?: "Sous AI",
                    color = Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                )
            }

            // External recipes open the page and offer a to-buy shortcut (iOS parity);
            // Sous-composed ones open the in-app cooking session instead.
            if (sourceSite != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick  = onViewRecipe,
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = Green),
                        shape    = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text("Open link", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.OpenInNew, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                    Button(
                        onClick  = onToBuy,
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor         = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.5f),
                        ),
                        shape    = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, null, tint = Green, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "To buy",
                            color = Green, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1,
                        )
                    }
                }
            } else {
                Button(
                    onClick  = onViewRecipe,
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Green),
                    shape    = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Filled.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View Recipe", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun CardStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
        Text(value, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CardPill(text: String) {
    Text(
        text,
        color      = Color.White.copy(alpha = 0.85f),
        fontSize   = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier   = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun LoadingBubble() {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistantAvatar()
        Surface(
            shape           = RoundedCornerShape(20.dp),
            color           = Color.White,
            shadowElevation = 1.dp,
        ) {
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(color = Green, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                Text("Sous is thinking…", color = Slate, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
        }
    }
}

// ── Composer (mood chips + input bar) ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Composer(
    inputText      : String,
    selectedMoods  : Set<String>,
    canSend        : Boolean,
    onInputChange  : (String) -> Unit,
    onMoodToggle   : (String) -> Unit,
    onSend         : () -> Unit,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = Cream,
        shadowElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().imePadding().padding(vertical = 8.dp)) {
            // Hairline separator at top
            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))
            Spacer(Modifier.height(8.dp))

            // Mood label + chips row
            Text(
                "What are you in the mood for?",
                color      = Navy,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(start = 16.dp, bottom = 6.dp),
            )
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MOOD_OPTIONS.forEach { mood ->
                    MoodPill(
                        label      = mood,
                        isSelected = selectedMoods.contains(mood),
                        onClick    = { onMoodToggle(mood) },
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Input bar
            val voiceLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                        ?.firstOrNull()
                        ?.let { onInputChange(it) }
                }
            }
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = {
                    runCatching {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ask Sous Anything")
                        }
                        voiceLauncher.launch(intent)
                    }
                }) {
                    Icon(Icons.Filled.Mic, "Voice input", tint = Slate, modifier = Modifier.size(22.dp))
                }
                Surface(
                    modifier        = Modifier.weight(1f),
                    shape           = RoundedCornerShape(25.dp),
                    color           = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    TextField(
                        value         = inputText,
                        onValueChange = onInputChange,
                        placeholder   = { Text("Ask Sous Anything…", color = Slate.copy(alpha = 0.5f), fontSize = 15.sp) },
                        // Grows with the text up to 4 lines, then scrolls (iOS lineLimit(1...4)).
                        // Multi-line, so Enter inserts a newline; sending is the button only.
                        singleLine    = false,
                        maxLines      = 4,
                        modifier      = Modifier.fillMaxWidth(),
                        colors        = TextFieldDefaults.colors(
                            unfocusedContainerColor   = Color.Transparent,
                            focusedContainerColor     = Color.Transparent,
                            unfocusedIndicatorColor   = Color.Transparent,
                            focusedIndicatorColor     = Color.Transparent,
                            disabledIndicatorColor    = Color.Transparent,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (canSend) Green else Slate.copy(alpha = 0.3f))
                        .clickable(enabled = canSend, onClick = onSend),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.ArrowUpward, "Send", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MoodPill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = CircleShape,
        color    = if (isSelected) Navy else Color.Transparent,
        border   = BorderStroke(1.dp, if (isSelected) Color.Transparent else Slate.copy(alpha = 0.4f)),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            label,
            color      = if (isSelected) Color.White else Navy,
            fontSize   = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

// ── Pro tab placeholder (unlocked, but empty until persistence ships) ────────

@Composable
private fun ProTabPlaceholder(title: String, body: String) {
    Box(
        modifier         = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(body, color = Slate, fontSize = 13.sp, textAlign = TextAlign.Center)
            Text("Nothing here yet", color = Slate.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}

// ── Premium lock state for Saved Recipes / My Recipes / My Plans ─────────────

@Composable
private fun PremiumLockState(feature: String, description: String) {
    Box(
        modifier         = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Lock, null, tint = Slate, modifier = Modifier.size(40.dp))
            Text(feature, color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(description, color = Slate, fontSize = 13.sp, textAlign = TextAlign.Center)
            Button(
                onClick  = { /* TODO: paywall */ },
                modifier = Modifier.padding(top = 8.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                shape    = RoundedCornerShape(16.dp),
            ) {
                Text("👑 Upgrade to Premium", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── LazyColumn helper ─────────────────────────────────────────────────────────

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    count : Int,
    key   : (Int) -> Any,
    item  : @Composable (Int) -> Unit,
) = items(count = count, key = key, itemContent = { item(it) })
