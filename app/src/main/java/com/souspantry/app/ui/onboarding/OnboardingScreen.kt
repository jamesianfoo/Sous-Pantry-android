package com.souspantry.app.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.launch

// Intro only. Goals, dietary needs and allergies are asked by the funnel, which
// also ends with the paywall — this carousel must not duplicate or pre-empt it.
private const val TOTAL_PAGES = 6

private data class FeaturePage(val tag: String?, val headline: String, val subtitle: String, val cta: String)

private val FEATURE_PAGES = listOf(
    FeaturePage(null,            "Your kitchen,\nalways in sync.",            "Sous Pantry uses AI to track what you have, plan what to cook, and shop smarter — all in one place.", "Get Started"),
    FeaturePage("Pantry",        "Know exactly what\nyou have.",              "Scan a receipt from your preferred local supermarket, take a photo of your pantry or items, or add them manually.", "Next"),
    FeaturePage("Plan & Cook",   "AI meals built\naround your pantry.",       "Sous Pantry suggests recipes using what's already in your kitchen. Cook one and your pantry updates automatically.", "Next"),
    FeaturePage("My Plans",      "Plan your week.\nCook with confidence.",    "Schedule meals for the week ahead and see exactly what you need in your pantry.", "Next"),
    FeaturePage("Smart Kitchen", "Your pantry updates\nitself after cooking.","After you cook a meal, Sous Pantry deducts the used ingredients and helps restock what you've run out of.", "Next"),
    FeaturePage("Reminders",     "Keep your pantry\nhonest.",                 "Get a nudge to audit your pantry on your schedule. Sous Pantry will surface the oldest items first.", "Continue"),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete : () -> Unit,
    vm         : OnboardingFlowViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { TOTAL_PAGES })
    val scope      = rememberCoroutineScope()

    HorizontalPager(
        state    = pagerState,
        modifier = Modifier.fillMaxSize().background(Cream),
    ) { page ->
        FeatureScreen(
            page      = FEATURE_PAGES[page],
            pageIndex = page,
            onNext    = {
                if (page == TOTAL_PAGES - 1) vm.complete(onComplete)
                else scope.launch { pagerState.animateScrollToPage(page + 1) }
            },
        )
    }
}

// ── Feature screen (pages 0–5) ────────────────────────────────────────────────

@Composable
private fun FeatureScreen(page: FeaturePage, pageIndex: Int, onNext: () -> Unit) {
    // No Skip anywhere: the intro can't be skipped, same rule as the funnel.
    Column(modifier = Modifier.fillMaxSize().background(Cream).padding(horizontal = 24.dp)) {
        Spacer(Modifier.weight(1f))

        Box(Modifier.align(Alignment.CenterHorizontally)) {
            when (pageIndex) {
                0    -> SousWordmark()
                1    -> PantryMockup()
                2    -> PlanCookMockup()
                3    -> MyPlansMockup()
                4    -> ConfirmCookedMockup()
                else -> PantryAuditMockup()
            }
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            page.tag?.let { TagPill(it) }
            Text(page.headline, color = Navy, fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp)
            Text(page.subtitle, color = Slate, fontSize = 14.sp, lineHeight = 20.sp)
        }

        Spacer(Modifier.height(28.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            DotIndicators(current = pageIndex, total = 6)
        }
        Spacer(Modifier.height(14.dp))
        CtaButton(page.cta, onClick = onNext)
        Spacer(Modifier.height(32.dp))
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
