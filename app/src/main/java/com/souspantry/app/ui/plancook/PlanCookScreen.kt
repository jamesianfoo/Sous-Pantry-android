package com.souspantry.app.ui.plancook

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.repository.PantryRepository
import com.souspantry.app.ui.theme.*

// ── Constants ────────────────────────────────────────────────────────────────

private val MOOD_OPTIONS = listOf(
    "Quick & Easy", "Comfort Food", "Light & Healthy",
    "Post-workout 💪", "Fancy", "Impress Someone", "Surprise Me 🎲",
)

@Composable
fun PlanCookScreen(
    vm        : PlanCookViewModel = hiltViewModel(),
    plansVm   : MyPlansViewModel  = viewModel(),
) {
    val state by vm.state.collectAsState()
    var addToWeekMeal by remember { mutableStateOf<SuggestedMeal?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        Header(
            showHistoryActions = state.selectedTab == PlanTab.DISCOVER,
            onHistoryTap       = { /* TODO: history sheet */ },
            onStartOver        = { vm.startOver() },
        )
        PillTabBar(selected = state.selectedTab, isPremium = state.isPremium, onSelect = vm::selectTab)

        when (state.selectedTab) {
            PlanTab.DISCOVER      -> DiscoverTab(state = state, vm = vm, onAddToWeek = { addToWeekMeal = it })
            PlanTab.SAVED_RECIPES -> if (state.isPremium) ProTabPlaceholder("Saved Recipes", "Tap the bookmark on any recipe to save it here.")
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
private fun DiscoverTab(state: PlanCookState, vm: PlanCookViewModel, onAddToWeek: (SuggestedMeal) -> Unit) {
    val listState = rememberLazyListState()

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
                MessageRow(message = state.messages[idx], onAddToWeek = onAddToWeek)
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
}

// ── Message rendering ────────────────────────────────────────────────────────

@Composable
private fun MessageRow(message: ChatMessage, onAddToWeek: (SuggestedMeal) -> Unit) {
    when (message) {
        is ChatMessage.Text -> when (message.role) {
            ChatRole.ASSISTANT -> AssistantTextBubble(text = message.content)
            ChatRole.USER      -> UserBubble(text = message.content)
        }
        is ChatMessage.RecipeList -> AssistantRecipeListBubble(
            intro       = message.intro,
            recipes     = message.recipes,
            onAddToWeek = onAddToWeek,
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

@Composable
private fun AssistantRecipeListBubble(
    intro       : String,
    recipes     : List<SuggestedMeal>,
    onAddToWeek : (SuggestedMeal) -> Unit,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistantAvatar()
        Surface(
            modifier        = Modifier.weight(1f),
            shape           = RoundedCornerShape(20.dp),
            color           = Color.White,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(intro, color = Navy, fontSize = 15.sp)
                recipes.forEach { recipe ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(12.dp),
                        color    = Cream,
                    ) {
                        Row(
                            modifier          = Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text       = recipe.title,
                                color      = Navy,
                                fontSize   = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier   = Modifier.weight(1f),
                            )
                            // Add to my week
                            IconButton(onClick = { onAddToWeek(recipe) }) {
                                Icon(Icons.Filled.CalendarMonth, "Add to my week", tint = Green)
                            }
                        }
                    }
                }
            }
        }
    }
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
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
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
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                IconButton(onClick = { /* TODO: voice */ }) {
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
                        singleLine    = true,
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
                shape    = RoundedCornerShape(14.dp),
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
