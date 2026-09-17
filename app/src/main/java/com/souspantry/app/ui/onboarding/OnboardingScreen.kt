package com.souspantry.app.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Egg
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.launch

private const val TOTAL_PAGES = 10
private const val PRO_PAGE    = 9

private data class FeaturePage(val emoji: String, val tag: String?, val headline: String, val subtitle: String, val cta: String)

private val FEATURE_PAGES = listOf(
    FeaturePage("🍳", null,            "Your kitchen,\nalways in sync.",            "Sous Pantry uses AI to track what you have, plan what to cook, and shop smarter — all in one place.", "Get Started"),
    FeaturePage("🧾", "Pantry",        "Know exactly what\nyou have.",              "Scan a receipt from your preferred local supermarket, take a photo of your pantry or items, or add them manually.", "Next"),
    FeaturePage("🍽️", "Plan & Cook",   "AI meals built\naround your pantry.",       "Sous Pantry suggests recipes using what's already in your kitchen. Cook one and your pantry updates automatically.", "Next"),
    FeaturePage("📅", "My Plans",      "Plan your week.\nCook with confidence.",    "Schedule meals for the week ahead and see exactly what you need in your pantry.", "Next"),
    FeaturePage("♻️", "Smart Kitchen", "Your pantry updates\nitself after cooking.","After you cook a meal, Sous Pantry deducts the used ingredients and helps restock what you've run out of.", "Next"),
    FeaturePage("🔔", "Reminders",     "Keep your pantry\nhonest.",                 "Get a nudge to audit your pantry on your schedule. Sous Pantry will surface the oldest items first.", "Next"),
)

private data class Opt(val id: String, val icon: ImageVector, val title: String, val detail: String)

private val GOALS = listOf(
    Opt("pantry",  Icons.Outlined.Kitchen,            "Manage my pantry well",        "Track ingredients and reduce food waste"),
    Opt("recipes", Icons.AutoMirrored.Filled.MenuBook,"Recommend recipes I can make", "AI meal ideas using what you already have"),
    Opt("staples", Icons.Filled.Checklist,            "Start with staple ingredients","We'll pre-fill your pantry with essentials"),
)
private val DIETARY = listOf(
    Opt("vegan",       Icons.Outlined.Eco,       "Vegan",         "No meat, fish, dairy, or eggs"),
    Opt("vegetarian",  Icons.Outlined.Spa,       "Vegetarian",    "No meat or fish"),
    Opt("pescatarian", Icons.Filled.SetMeal,     "Pescatarian",   "Meat-free, fish and seafood allowed"),
    Opt("no_beef",     Icons.Filled.Warning,     "No Beef",       "No beef or veal in any recipe"),
    Opt("no_pork",     Icons.Filled.Warning,     "No Pork",       "No pork or pork-derived products"),
    Opt("none",        Icons.Filled.CheckCircle, "No preference", "I eat everything"),
)
private val RESTRICTIONS = listOf(
    Opt("gluten_free",    Icons.Outlined.BakeryDining, "Gluten Free",              "No wheat, barley, or rye"),
    Opt("nut_free",       Icons.Outlined.Eco,          "Nut Free",                 "No tree nuts or peanuts"),
    Opt("dairy_free",     Icons.Filled.WaterDrop,      "Dairy Free",               "No dairy or lactose products"),
    Opt("shellfish_free", Icons.Filled.SetMeal,        "Shellfish / Seafood Free", "No shellfish or seafood"),
    Opt("egg_free",       Icons.Filled.Egg,            "Egg Free",                 "No eggs or egg-based products"),
    Opt("pregnancy",      Icons.Filled.Favorite,       "Pregnancy Friendly",       "Safe for expectant mothers"),
    Opt("none",           Icons.Filled.NoMeals,        "No restrictions",          "No food restrictions"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete : () -> Unit,
    vm         : OnboardingFlowViewModel = hiltViewModel(),
) {
    val state      by vm.state.collectAsState()
    val pagerState  = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope       = rememberCoroutineScope()

    fun goTo(page: Int) { scope.launch { pagerState.animateScrollToPage(page) } }
    fun next()          = goTo((pagerState.currentPage + 1).coerceAtMost(PRO_PAGE))
    fun skipToPro()     = goTo(PRO_PAGE)

    HorizontalPager(
        state    = pagerState,
        modifier = Modifier.fillMaxSize().background(Beige),
    ) { page ->
        when (page) {
            in 0..5 -> FeatureScreen(
                page      = FEATURE_PAGES[page],
                pageIndex = page,
                onNext    = { next() },
                onSkip    = { skipToPro() },
            )
            6 -> WizardScreen(
                step       = 1,
                headline   = "What do you\nwant to do?",
                subtitle   = "Select all that apply — you can change this later in Account.",
                options    = GOALS,
                isSelected = { state.selectedGoals.contains(it) },
                onTap      = { vm.toggleGoal(it) },
                ctaLabel   = if (state.selectedGoals.isEmpty()) "Skip for now" else "Continue",
                onCta      = { next() },
                onSkip     = { skipToPro() },
            )
            7 -> WizardScreen(
                step       = 2,
                headline   = "Are you…?",
                subtitle   = "Choose the option that best describes your diet.",
                options    = DIETARY,
                isSelected = { state.selectedDietary == it },
                onTap      = { vm.selectDietary(it) },
                ctaLabel   = "Continue",
                onCta      = { next() },
                onSkip     = { skipToPro() },
            )
            8 -> WizardScreen(
                step       = 3,
                headline   = "Any food\nrestrictions?",
                subtitle   = "Select all that apply. We'll keep every recipe safe for you.",
                options    = RESTRICTIONS,
                isSelected = { state.selectedRestrictions.contains(it) },
                onTap      = { vm.toggleRestriction(it) },
                ctaLabel   = "Continue",
                onCta      = { next() },
                onSkip     = { skipToPro() },
            )
            else -> ProScreen(
                onStartTrial   = { vm.complete(onComplete) },
                onContinueFree = { vm.complete(onComplete) },
            )
        }
    }
}

// ── Feature screen (pages 0–5) ────────────────────────────────────────────────

@Composable
private fun FeatureScreen(page: FeaturePage, pageIndex: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
            Text("Skip", color = Slate.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.clickable(onClick = onSkip).padding(8.dp))
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 300.dp, height = 200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(page.emoji, fontSize = 88.sp)
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            page.tag?.let { TagPill(it) }
            Text(page.headline, color = Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Text(page.subtitle, color = Slate, fontSize = 14.sp, lineHeight = 20.sp)
        }

        Spacer(Modifier.height(28.dp))

        DotIndicators(current = pageIndex, total = 6)
        Spacer(Modifier.height(14.dp))
        CtaButton(page.cta, onClick = onNext)
        Spacer(Modifier.height(32.dp))
    }
}

// ── Wizard screen (pages 6–8) ─────────────────────────────────────────────────

@Composable
private fun WizardScreen(
    step       : Int,
    headline   : String,
    subtitle   : String,
    options    : List<Opt>,
    isSelected : (String) -> Boolean,
    onTap      : (String) -> Unit,
    ctaLabel   : String,
    onCta      : () -> Unit,
    onSkip     : () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Beige)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                repeat(3) { i ->
                    val w by animateDpAsState(if (i + 1 == step) 20.dp else 6.dp, label = "stepdot")
                    Box(Modifier.width(w).height(6.dp).clip(CircleShape).background(if (i + 1 <= step) Green else Navy.copy(alpha = 0.15f)))
                }
            }
            Text("Skip", color = Slate.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.clickable(onClick = onSkip).padding(8.dp))
        }

        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(headline, color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
            Text(subtitle, color = Slate, fontSize = 14.sp, lineHeight = 20.sp)
        }

        LazyColumn(
            modifier            = Modifier.weight(1f).fillMaxWidth(),
            contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            optionItems(options.size) { idx ->
                val opt = options[idx]
                WizardOptionCard(opt, isSelected(opt.id)) { onTap(opt.id) }
            }
        }

        HorizontalDivider(color = Navy.copy(alpha = 0.08f))
        Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            CtaButton(ctaLabel, onClick = onCta)
        }
    }
}

// ── Pro screen (page 9) ───────────────────────────────────────────────────────

@Composable
private fun ProScreen(onStartTrial: () -> Unit, onContinueFree: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Beige).padding(horizontal = 24.dp)) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 300.dp, height = 200.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.verticalGradient(listOf(Navy, Color(0xFF1A3829)))),
            contentAlignment = Alignment.Center,
        ) {
            Text("👑", fontSize = 88.sp)
        }
        Spacer(Modifier.weight(1f))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TagPill("Sous Pantry Premium")
            Text("Your kitchen,\nfully in control.", color = Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Text(
                "Go Pro for unlimited AI recipes, meal planning, receipt imports, and an ad-free experience. 7-day free trial included.",
                color = Slate, fontSize = 14.sp, lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(28.dp))
        CtaButton("Start 7-Day Free Trial", onClick = onStartTrial)
        Spacer(Modifier.height(8.dp))
        Text(
            "Continue with Free",
            color      = Slate.copy(alpha = 0.7f),
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier   = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onContinueFree).padding(8.dp),
        )
        Spacer(Modifier.height(28.dp))
    }
}

// ── Shared bits ───────────────────────────────────────────────────────────────

@Composable
private fun TagPill(text: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(Green.copy(alpha = 0.12f))
            .border(1.dp, Green.copy(alpha = 0.25f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text("✦", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(text, color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DotIndicators(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            val w by animateDpAsState(if (i == current) 18.dp else 6.dp, label = "dot")
            Box(Modifier.width(w).height(6.dp).clip(CircleShape).background(if (i == current) Green else Navy.copy(alpha = 0.18f)))
        }
    }
}

@Composable
private fun CtaButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(Green, Color(0xFF6B7A40))))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WizardOptionCard(opt: Opt, isSelected: Boolean, onTap: () -> Unit) {
    Surface(
        modifier        = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onTap),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        border          = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Green else Navy.copy(alpha = 0.08f)),
        shadowElevation = if (isSelected) 6.dp else 2.dp,
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (isSelected) Green.copy(alpha = 0.15f) else Navy.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(opt.icon, null, tint = if (isSelected) Green else Slate, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(opt.title, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(opt.detail, color = Slate, fontSize = 12.sp)
            }
            Box(
                modifier         = Modifier.size(22.dp).clip(CircleShape)
                    .background(if (isSelected) Green else Color.Transparent)
                    .border(2.dp, if (isSelected) Green else Navy.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
    }
}

private fun LazyListScope.optionItems(count: Int, item: @Composable (Int) -> Unit) =
    items(count = count, itemContent = { item(it) })
