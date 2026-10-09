package com.souspantry.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.data.models.PriceRecord
import com.souspantry.app.services.AwardTier
import com.souspantry.app.services.PriceInsights
import com.souspantry.app.services.StreakReward
import com.souspantry.app.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale

// ── Awards sheet ─────────────────────────────────────────────────────────────

/** Earned-card tints per tier (iOS AwardsSheet). */
private fun AwardTier.tint(): Color = when (this) {
    AwardTier.AMBER -> Color(0xFFF7F5E6)
    AwardTier.TEAL  -> SoftMint.copy(alpha = 0.6f)
    AwardTier.NAVY  -> Color(0xFFEBF2FC)
    AwardTier.GOLD  -> Color(0xFFFCF7E0)
    AwardTier.GRAY  -> White
}

private data class AwardProgress(val fraction: Float, val status: String)

/** Port of iOS AwardsSheet.progress(for:counters:). Scan / rescue / speed counters are 0 on iOS too. */
private fun awardProgress(a: StreakReward, earned: Boolean, meals: Int, streak: Int, recipes: Int): AwardProgress {
    fun pct(have: Int, need: Int) = if (need > 0) (have.toFloat() / need).coerceAtMost(1f) else 0f
    fun toGo(have: Int, need: Int) = (need - have).coerceAtLeast(0)
    return when (a) {
        StreakReward.KITCHEN_EXPLORER, StreakReward.PANTRY_EXPERT, StreakReward.ULTIMATE_PANTRY_EXPERT, StreakReward.PANTRY_SPECIALIST -> {
            val need = when (a) { StreakReward.KITCHEN_EXPLORER -> 20; StreakReward.PANTRY_EXPERT -> 150; StreakReward.ULTIMATE_PANTRY_EXPERT -> 500; else -> 1000 }
            if (earned) AwardProgress(0f, "Unlocked — $need items scanned") else AwardProgress(0f, "0 scanned — $need to go")
        }
        StreakReward.IMPACT_MAKER, StreakReward.IMPACT_CATALYST -> {
            val need = if (a == StreakReward.IMPACT_MAKER) 50 else 100
            if (earned) AwardProgress(0f, "Unlocked — $need expiring items rescued") else AwardProgress(0f, "0 rescued — $need to go")
        }
        StreakReward.SPEED_COOK       -> if (earned) AwardProgress(0f, "Unlocked — 5 meals under 15 min")  else AwardProgress(0f, "0 / 5 quick meals")
        StreakReward.SUPER_SPEED_COOK -> if (earned) AwardProgress(0f, "Unlocked — 20 meals under 15 min") else AwardProgress(0f, "0 / 20 quick meals")
        StreakReward.SONIC_SPEED_COOK -> if (earned) AwardProgress(0f, "Unlocked — 20 meals under 5 min")  else AwardProgress(0f, "0 / 20 super-quick meals")
        StreakReward.PERFECT_WEEK, StreakReward.PERFECT_STREAK -> {
            val need = if (a == StreakReward.PERFECT_WEEK) 7 else 30
            if (earned) AwardProgress(0f, "Unlocked — $need-day streak")
            else AwardProgress(pct(streak, need), "$streak-day streak — ${toGo(streak, need)} to go")
        }
        StreakReward.GRAND_MASTER_CHEF, StreakReward.LEGENDARY_CHEF, StreakReward.SUPER_LEGENDARY_CHEF -> {
            val (m, r) = when (a) { StreakReward.GRAND_MASTER_CHEF -> 250 to 20; StreakReward.LEGENDARY_CHEF -> 500 to 50; else -> 1500 to 150 }
            if (earned) AwardProgress(0f, "Unlocked — $m meals + $r recipes")
            else AwardProgress(minOf(pct(meals, m), pct(recipes, r)), "$meals/$m meals · $recipes/$r own recipes")
        }
        else -> {
            val need = when (a) {
                StreakReward.HOME_COOK -> 5; StreakReward.SUPER_HOME_COOK -> 20; StreakReward.AMAZING_HOME_COOK -> 40
                StreakReward.STELLAR_CHEF -> 60; StreakReward.EXECUTIVE_CHEF -> 80; StreakReward.HEAD_CHEF -> 100
                StreakReward.CULINARY_MASTER -> 120; StreakReward.MASTER_CHEF -> 150; StreakReward.TOP_CHEF -> 200
                else -> 3000
            }
            if (earned) AwardProgress(0f, "Unlocked — $need meals cooked")
            else AwardProgress(pct(meals, need), "$meals meals cooked — ${toGo(meals, need)} to go")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AwardsSheet(
    earned            : List<StreakReward>,
    totalMealsCooked  : Int,
    currentStreak     : Int,
    customRecipeCount : Int,
    onDismiss         : () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Cream) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Awards", color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("${earned.size}/${StreakReward.entries.size} awards completed", color = Slate, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                Box(
                    modifier         = Modifier.size(28.dp).clip(CircleShape).background(Slate.copy(alpha = 0.12f)).clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                contentPadding        = PaddingValues(bottom = 40.dp),
            ) {
                items(StreakReward.entries) { award ->
                    AwardCell(award, award in earned, awardProgress(award, award in earned, totalMealsCooked, currentStreak, customRecipeCount))
                }
                item(span = { GridItemSpan(2) }) { MoreAwardsCell() }
            }
        }
    }
}

@Composable
private fun AwardCell(award: StreakReward, isEarned: Boolean, progress: AwardProgress) {
    val accent     = award.accent()
    val inProgress = !isEarned && progress.fraction > 0f
    val titleColor = when {
        isEarned   -> Green
        inProgress -> Navy
        else       -> Slate.copy(alpha = 0.45f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(14.dp), ambientColor = Navy.copy(alpha = 0.06f), spotColor = Navy.copy(alpha = 0.06f)),
        shape    = RoundedCornerShape(14.dp),
        color    = if (isEarned) award.tier.tint() else White,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(if (isEarned) accent.copy(alpha = 0.15f) else Slate.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(award.icon.vector(), null, tint = if (isEarned) accent else Slate.copy(alpha = 0.45f), modifier = Modifier.size(24.dp))
            }
            Text(award.title, color = titleColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            if (inProgress) {
                Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Slate.copy(alpha = 0.15f))) {
                    Box(modifier = Modifier.fillMaxWidth(progress.fraction).fillMaxHeight().clip(CircleShape).background(Green))
                }
            }
            Text(progress.status, color = if (isEarned) Green else Slate.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun MoreAwardsCell() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = White) {
        Column(
            modifier            = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Filled.Lock, null, tint = Slate.copy(alpha = 0.5f), modifier = Modifier.size(24.dp))
            Text("More awards coming", color = Slate.copy(alpha = 0.5f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
                "New badges will appear here as we add more ways to celebrate your cooking.",
                color = Slate, fontSize = 12.sp, textAlign = TextAlign.Center,
            )
            Text("Stay tuned", color = Slate.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

// ── Price Trends sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTrendsSheet(
    records        : List<PriceRecord>,
    hauls          : List<PriceInsights.HaulEntry>,
    spendThreshold : Int,
    currencyCode   : String?,
    onDismiss      : () -> Unit,
) {
    fun money(amount: Double)      = formatMoney(amount, currencyCode, 2)
    fun moneyWhole(amount: Double) = formatMoney(amount, currencyCode, 0)

    val movers    = PriceInsights.topMovers(records)
    val monthly   = PriceInsights.monthlySpend(hauls)
    val bestStore = PriceInsights.bestStores(records)
    val threshold = spendThreshold.toDouble()
    val streak    = PriceInsights.spendingStreak(hauls, threshold)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Cream) {
        Column(
            modifier            = Modifier.fillMaxWidth().fillMaxHeight(0.92f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Price Trends", color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Cancel, "Close", tint = Navy.copy(alpha = 0.25f),
                    modifier = Modifier.size(28.dp).clip(CircleShape).clickable(onClick = onDismiss),
                )
            }

            TrendSection("TOP MOVERS") {
                if (movers.isEmpty()) EmptyCard(PriceInsights.EMPTY_TOP_MOVERS)
                else movers.forEach { m ->
                    WhiteCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(m.itemName, color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${money(m.oldPrice)} → ${money(m.newPrice)} / ${m.unitLabel}", color = Slate, fontSize = 12.sp)
                            }
                            Capsule(m.newStore, Green, 11.sp, FontWeight.SemiBold, vertical = 3.dp)
                            Capsule(m.pctText, if (m.isUp) Color.Red else Green, 12.sp, FontWeight.Bold, vertical = 4.dp)
                        }
                    }
                }
            }

            TrendSection("MONTHLY SPEND") {
                if (monthly.isEmpty()) EmptyCard(PriceInsights.EMPTY_MONTHLY_SPEND)
                else WhiteCard(padding = 14.dp) {
                    val maxValue = maxOf(monthly.maxOf { it.total }, 1.0)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        monthly.forEach { row ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    row.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(44.dp),
                                )
                                Box(modifier = Modifier.weight(1f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(SoftMint.copy(alpha = 0.4f))) {
                                    Box(modifier = Modifier.fillMaxWidth((row.total / maxValue).toFloat().coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(Green))
                                }
                                Text(moneyWhole(row.total), color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(64.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            if (bestStore.isNotEmpty()) {
                TrendSection("BEST STORE FOR YOU") {
                    bestStore.forEach { c ->
                        WhiteCard {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("🛒", fontSize = 18.sp)
                                Text(PriceInsights.bestStoreLine(c, money(c.savings)), color = Navy, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (streak >= PriceInsights.MIN_STREAK_VISITS) {
                TrendSection("SPENDING STREAK") {
                    val t = moneyWhole(threshold)
                    Row(
                        modifier              = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(SoftMint).padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Gold, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(PriceInsights.streakTitle(t), color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(PriceInsights.streakBody(t, streak), color = Navy.copy(alpha = 0.75f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 4.dp))
        content()
    }
}

@Composable
private fun WhiteCard(padding: androidx.compose.ui.unit.Dp = 12.dp, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp), ambientColor = Navy.copy(alpha = 0.06f), spotColor = Navy.copy(alpha = 0.06f)),
        shape    = RoundedCornerShape(12.dp),
        color    = White,
    ) {
        Box(modifier = Modifier.padding(padding)) { content() }
    }
}

@Composable
private fun EmptyCard(text: String) {
    WhiteCard(padding = 16.dp) {
        Text(text, color = Slate, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Capsule(text: String, color: Color, size: androidx.compose.ui.unit.TextUnit, weight: FontWeight, vertical: androidx.compose.ui.unit.Dp) {
    Surface(shape = CircleShape, color = color) {
        Text(text, color = White, fontSize = size, fontWeight = weight, modifier = Modifier.padding(horizontal = 8.dp, vertical = vertical))
    }
}
