package com.souspantry.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.R
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.services.AwardIcon
import com.souspantry.app.services.AwardTier
import com.souspantry.app.services.StreakReward
import com.souspantry.app.services.StreakService
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar

// ── iOS palette values the theme does not carry ─────────────────────────────
private val DeepGreen    = Color(0xFF0A2A1F)
private val MutedGray    = Color(0xFF5F5E5A)
private val SystemOrange = Color(0xFFFF9500)
private val Lime         = Color(0xFF2CFF05)
private val AuditTop     = Color(0xFFD1380D)
private val AuditBottom  = Color(0xFFF28514)
private val AuditShadow  = Color(0xFFE6591A)
private val AuditWarn    = Color(0xFFFFBF4D)

/** Donut arcs in iOS order: green, navy, gold, system orange, purple. */
private val DONUT_PALETTE = listOf(Green, Navy, Gold, SystemOrange, Color(0xFF8C40BF))

/** Nearest Material icon for each iOS SF Symbol award family. */
fun AwardIcon.vector(): ImageVector = when (this) {
    AwardIcon.BASKET -> Icons.Filled.ShoppingBasket
    AwardIcon.LEAF   -> Icons.Filled.Eco
    AwardIcon.BOLT   -> Icons.Filled.Bolt
    AwardIcon.FLAME  -> Icons.Filled.LocalFireDepartment
    AwardIcon.MEDAL  -> Icons.Filled.MilitaryTech
    AwardIcon.TROPHY -> Icons.Filled.EmojiEvents
    AwardIcon.STAR   -> Icons.Filled.Star
    AwardIcon.LAUREL -> Icons.Filled.WorkspacePremium
    AwardIcon.CROWN  -> Icons.Filled.Diamond
}

/** iOS: gold for the navy tier, green otherwise. */
fun StreakReward.accent(): Color = if (tier == AwardTier.NAVY) Gold else Green

private fun toDay(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

/** Calendar days from now to [ms] (negative = past). */
private fun daysFromNow(ms: Long): Int = ChronoUnit.DAYS.between(LocalDate.now(), toDay(ms)).toInt()

private fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

@Composable
fun HomeScreen(
    onNavigateToTab : (String) -> Unit = {},
    vm              : HomeViewModel    = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    var showAwards      by remember { mutableStateOf(false) }
    var showPriceTrends by remember { mutableStateOf(false) }
    var banner          by remember { mutableStateOf<StreakReward?>(null) }

    // iOS: on appear, take the pending banner reward, clear it, show the toast for 15 s.
    val pending = state.streak.bannerReward
    LaunchedEffect(pending) {
        if (pending != null) {
            banner = pending
            vm.clearBanner()
            delay(15_000)
            banner = null
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Cream)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            HeroSection(
                userName         = state.userName,
                latestAward      = state.streak.latestAward,
                dietaryTypes     = state.dietaryTypes,
                foodRestrictions = state.foodRestrictions,
                avoidIngredients = state.avoidIngredients,
            )

            Column(
                modifier            = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                if (state.auditItems.isNotEmpty()) {
                    PantryAuditCard(
                        items       = state.auditItems,
                        lastAuditMs = state.lastAuditMs,
                        onView      = { vm.viewExpiringItems(); onNavigateToTab("pantry") },
                    )
                }

                RecentHaulCard(
                    pantryIsEmpty      = state.pantryItems.isEmpty(),
                    estimatedMealCount = state.estimatedRecipeCount,
                    lastSyncMs         = state.lastSyncMs,
                    onAddItems         = { onNavigateToTab("pantry") },
                    onSeeWhatToCook    = { vm.seeWhatToCook(); onNavigateToTab("plancook") },
                    onViewShopping     = { onNavigateToTab("shopping") },
                    onPriceTrendsTap   = { showPriceTrends = true },
                )

                if (state.pantryItems.isNotEmpty()) {
                    PantryHealthCard(
                        totalItems        = state.pantryItems.size,
                        categoryBreakdown = state.categoryBreakdown,
                        atRiskCount       = state.auditItems.size,
                    )
                }

                CookingStreakCard(
                    mealsThisWeek    = state.streak.mealsThisWeek,
                    totalMealsCooked = state.streak.totalMealsCooked,
                    currentStreak    = state.streak.currentStreak,
                    weeklySavings    = state.streak.weeklySavings,
                    cookedWeekdays   = StreakService.cookedWeekdays(
                        lastCookedDay = state.streak.lastCookedDate?.let { StreakService.toDay(it) },
                        today         = LocalDate.now(StreakService.ZONE),
                        mealsThisWeek = state.streak.mealsThisWeek,
                    ),
                    earnedCount      = state.streak.earnedAwards.size,
                    onOpenAwards     = { showAwards = true },
                )
            }
        }

        AnimatedVisibility(
            visible  = banner != null,
            enter    = slideInVertically { -it },
            exit     = slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            banner?.let { AchievementBanner(reward = it, onDismiss = { banner = null }) }
        }
    }

    if (showAwards) {
        AwardsSheet(
            earned            = state.streak.earnedAwards,
            totalMealsCooked  = state.streak.totalMealsCooked,
            currentStreak     = state.streak.currentStreak,
            customRecipeCount = state.customRecipeCount,
            onDismiss         = { showAwards = false },
        )
    }
    if (showPriceTrends) {
        PriceTrendsSheet(
            records        = state.priceRecords,
            hauls          = state.hauls,
            spendThreshold = state.spendThreshold,
            currencyCode   = state.currencyCode,
            onDismiss      = { showPriceTrends = false },
        )
    }
}

// ── Hero (33% of screen, vegetable image + cream gradient, name + award + diet chips) ──

private val DIET_LABELS = mapOf(
    "vegan" to "Vegan", "vegetarian" to "Vegetarian", "pescatarian" to "Pescatarian", "no_beef" to "No Beef",
    "no_pork" to "No Pork", "paleo" to "Paleo", "keto" to "Keto", "halal" to "Halal",
)
private val RESTRICTION_LABELS = mapOf(
    "gluten_free" to "GF", "nut_free" to "Nut-Free", "dairy_free" to "Dairy-Free", "shellfish_free" to "Shellfish-Free",
    "egg_free" to "Egg-Free", "soy_free" to "Soy-Free", "pregnancy" to "Pregnancy Safe",
)

@Composable
private fun HeroSection(
    userName         : String,
    latestAward      : StreakReward?,
    dietaryTypes     : Set<String>,
    foodRestrictions : Set<String>,
    avoidIngredients : Set<String>,
) {
    val firstName  = userName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Chef"
    val greeting   = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }
    val heroHeight = (LocalConfiguration.current.screenHeightDp * 0.33f).dp
    val chips: List<Pair<String, Color>> =
        dietaryTypes.sorted().mapNotNull { id -> DIET_LABELS[id]?.let { it to Green } } +
        foodRestrictions.sorted().mapNotNull { id -> RESTRICTION_LABELS[id]?.let { it to Gold } } +
        avoidIngredients.sortedBy { it.lowercase() }.map { "No $it" to Slate }

    Box(modifier = Modifier.fillMaxWidth().height(heroHeight)) {
        Image(
            painter            = painterResource(id = R.drawable.hero_vegetables),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(0.0f to Cream.copy(alpha = 0.35f), 0.5f to Cream.copy(alpha = 0.65f), 1.0f to Cream),
                ),
            ),
        )
        Column(
            modifier            = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(greeting, color = MutedGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(firstName, color = DeepGreen, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                if (latestAward != null) {
                    Icon(latestAward.icon.vector(), latestAward.title, tint = latestAward.accent(), modifier = Modifier.size(22.dp))
                }
            }
            if (chips.isNotEmpty()) {
                Row(
                    modifier              = Modifier.padding(top = 4.dp).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    chips.forEach { (label, color) ->
                        Surface(shape = CircleShape, color = color) {
                            Text(
                                label, color = White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Pantry Audit Due (orange gradient card) ──────────────────────────────────

@Composable
private fun PantryAuditCard(items: List<PantryItem>, lastAuditMs: Long?, onView: () -> Unit) {
    val lastChecked = when (val d = lastAuditMs?.let { -daysFromNow(it) }) {
        null -> "Never checked"
        0    -> "Last checked today"
        1    -> "Last checked yesterday"
        else -> "Last checked $d days ago"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = AuditShadow.copy(alpha = 0.45f), spotColor = AuditShadow.copy(alpha = 0.45f))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(AuditTop, AuditBottom), start = Offset.Zero, end = Offset.Infinite))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(Icons.Filled.Warning, null, tint = AuditWarn, modifier = Modifier.size(16.dp))
            Text("Pantry Audit Due", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "$lastChecked · ${plural(items.size, "item")} need review",
            color    = White.copy(alpha = 0.75f),
            fontSize = 12.sp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.take(4).forEach { AuditRow(it) }
        }
        if (items.size > 4) {
            Text("+ ${items.size - 4} more items expiring", color = White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(White.copy(alpha = 0.2f))
                .clickable(onClick = onView)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.Filled.Checklist, null, tint = White, modifier = Modifier.size(16.dp))
            Text("View Expiring Items", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AuditRow(item: PantryItem) {
    val days     = item.expiryDate?.let(::daysFromNow) ?: 0
    val dotColor = when {
        days < 0   -> Color(0xFFF05959)
        days == 0  -> Color(0xFFFF8C33)
        days <= 3  -> Color(0xFFFFCC33)
        else       -> White.copy(alpha = 0.5f)
    }
    val (label, labelColor) = when {
        days < 0   -> "Expired"          to Color(0xFFFF8080)
        days == 0  -> "Expires today"    to Color(0xFFFFBF66)
        days == 1  -> "Expires tomorrow" to White.copy(alpha = 0.85f)
        else       -> "Expires in $days days" to White.copy(alpha = 0.85f)
    }
    val added = -daysFromNow(item.dateAdded)
    val addedText = when {
        added <= 0 -> "Added today"
        added == 1 -> "Added yesterday"
        added < 7  -> "Added $added days ago"
        else       -> "Added ${plural(added / 7, "week")} ago"
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(addedText, color = White.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Text(label, color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── Recent Haul (navy card with CTAs + Price Trends row) ─────────────────────

@Composable
private fun RecentHaulCard(
    pantryIsEmpty      : Boolean,
    estimatedMealCount : Int,
    lastSyncMs         : Long?,
    onAddItems         : () -> Unit,
    onSeeWhatToCook    : () -> Unit,
    onViewShopping     : () -> Unit,
    onPriceTrendsTap   : () -> Unit,
) {
    val mealLabel = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10  -> "TODAY'S BREAKFAST"
            in 11..14 -> "TODAY'S LUNCH"
            else      -> "TONIGHT'S DINNER"
        }
    }
    val subtitle = when (val d = lastSyncMs?.let { -daysFromNow(it) }) {
        null -> "No scan recorded yet."
        0    -> "Last recipe scan was today."
        1    -> "Last recipe scan was yesterday."
        else -> "Last recipe scan was $d days ago."
    }

    Surface(
        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(18.dp), ambientColor = Navy.copy(alpha = 0.45f), spotColor = Navy.copy(alpha = 0.45f)),
        shape    = RoundedCornerShape(18.dp),
        color    = Navy,
    ) {
        Column {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛒", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Recent Haul", color = White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (!pantryIsEmpty) {
                        Surface(shape = CircleShape, color = Lime.copy(alpha = 0.15f)) {
                            Text(
                                mealLabel, color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Text(subtitle, color = White, fontSize = 12.sp)

                if (pantryIsEmpty) {
                    Text("Let's get you started", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    CtaButton(label = "Add new items →",      primary = true,  onClick = onAddItems)
                    CtaButton(label = "View shopping list →", primary = false, onClick = onViewShopping)
                } else {
                    Text(
                        "$estimatedMealCount meals you could cook right now. Hungry?",
                        color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    )
                    CtaButton(label = "See what to cook →", primary = true, onClick = onSeeWhatToCook)
                }
            }

            if (!pantryIsEmpty) {
                HorizontalDivider(thickness = 1.dp, color = White.copy(alpha = 0.12f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPriceTrendsTap)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.TrendingUp, null, tint = White, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PRICE TRENDS", color = White.copy(alpha = 0.5f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                        Text("Tap to see spend insights", color = White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CtaButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick),
        shape    = RoundedCornerShape(12.dp),
        color    = if (primary) Green else White.copy(alpha = 0.10f),
        border   = if (primary) null else BorderStroke(1.dp, White.copy(alpha = 0.18f)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color      = if (primary) White else White.copy(alpha = 0.85f),
                fontSize   = 15.sp,
                fontWeight = if (primary) FontWeight.Bold else FontWeight.SemiBold,
            )
        }
    }
}

// ── Pantry Health (white card with donut + legend) ───────────────────────────

@Composable
private fun PantryHealthCard(
    totalItems        : Int,
    categoryBreakdown : List<Triple<String, Float, Int>>,
    atRiskCount       : Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(16.dp), ambientColor = Navy.copy(alpha = 0.08f), spotColor = Navy.copy(alpha = 0.08f)),
        shape    = RoundedCornerShape(16.dp),
        color    = White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🫙", fontSize = 20.sp)
                Text("Pantry Health", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    DonutChart(
                        segments  = categoryBreakdown.take(5).mapIndexed { idx, (_, frac, _) -> frac to DONUT_PALETTE[idx] },
                        strokeDp  = 18,
                        baseColor = SoftMint,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalItems", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("items", color = Slate, fontSize = 11.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryBreakdown.take(4).forEachIndexed { idx, (name, frac, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DONUT_PALETTE[idx]))
                            Text(name, color = Navy, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(frac * 100).toInt()}%", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.2f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (atRiskCount > 0) {
                            Icon(Icons.Filled.Warning, null, tint = Gold, modifier = Modifier.size(14.dp))
                            Text("~$${Math.round(atRiskCount * 4.50)} at risk", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(Icons.Filled.CheckCircle, null, tint = Green, modifier = Modifier.size(14.dp))
                            Text("Nothing expiring soon", color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(segments: List<Pair<Float, Color>>, strokeDp: Int, baseColor: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthPx = strokeDp.dp.toPx()
        val pad           = strokeWidthPx / 2f
        val arcSize       = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)
        drawArc(baseColor, 0f, 360f, false, Offset(pad, pad), arcSize, style = Stroke(width = strokeWidthPx))
        var start = -90f
        segments.forEach { (frac, color) ->
            val sweep = frac * 360f
            drawArc(color, start, sweep, false, Offset(pad, pad), arcSize, style = Stroke(width = strokeWidthPx))
            start += sweep
        }
    }
}

// ── Cooking streak card (meals + DAILY STREAK + Awards) ──────────────────────

@Composable
private fun CookingStreakCard(
    mealsThisWeek    : Int,
    totalMealsCooked : Int,
    currentStreak    : Int,
    weeklySavings    : Int,
    cookedWeekdays   : Set<Int>,
    earnedCount      : Int,
    onOpenAwards     : () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = Navy.copy(alpha = 0.07f), spotColor = Navy.copy(alpha = 0.07f)),
        shape    = RoundedCornerShape(14.dp),
        color    = White,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // ── Section 1: meals + savings + ladder ──
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍳", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cooking streak", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (currentStreak >= 3) {
                        Surface(shape = CircleShape, color = SystemOrange.copy(alpha = 0.12f)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = SystemOrange, modifier = Modifier.size(12.dp))
                                Text(plural(currentStreak, "day"), color = SystemOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${plural(mealsThisWeek, "meal")} cooked this week", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Surface(shape = CircleShape, color = SoftMint) {
                        Text(
                            "~ $$weeklySavings saved from eating out",
                            color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                val ladder = ladderInfo(totalMealsCooked)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.15f))) {
                        Box(modifier = Modifier.fillMaxWidth(ladder.fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Green))
                    }
                    ladder.earnedText?.let { Text(it, color = Green, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                    Text(ladder.goalText, color = Slate, fontSize = 12.sp)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))

            // ── Section 2: daily streak ──
            DailyStreakSection(cookedWeekdays = cookedWeekdays, currentStreak = currentStreak)

            // ── Section 3: Awards row (only once something is earned) ──
            if (earnedCount > 0) {
                HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SoftMint.copy(alpha = 0.45f))
                        .clickable(onClick = onOpenAwards)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Awards", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("$earnedCount earned", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = Slate, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun DailyStreakSection(cookedWeekdays: Set<Int>, currentStreak: Int) {
    val labels   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val todayIdx = LocalDate.now(StreakService.ZONE).dayOfWeek.value - 1   // Mon = 0 … Sun = 6

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DAILY STREAK", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, label ->
                val isToday  = i == todayIdx
                val isCooked = i in cookedWeekdays
                val bg = when {
                    isCooked     -> Green
                    isToday      -> Navy
                    i > todayIdx -> Color.Gray.copy(alpha = 0.15f)
                    else         -> Color.Gray.copy(alpha = 0.25f)
                }
                Box(
                    modifier         = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(bg).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = if (isCooked || isToday) White else Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(streakDayMessage(currentStreak), color = Navy, fontSize = 13.sp)
    }
}

private fun streakDayMessage(streak: Int): String = when (streak) {
    0        -> "Cook today to start a streak. Keep going!"
    1        -> "1-day streak — Great start! Keep going!"
    in 2..6  -> "$streak-day streak 🔥 Keep going!"
    in 7..13 -> "$streak-day streak 🔥 You're on fire! Keep going!"
    else     -> "$streak-day streak 🔥 You're unstoppable!"
}

private data class LadderInfo(val fraction: Float, val earnedText: String?, val goalText: String)

/** iOS badge ladder: progress toward the next tier plus the "{prev} earned!" line once a tier is reached. */
private fun ladderInfo(total: Int): LadderInfo {
    val ladder = listOf(
        5 to "Home Cook 🏅", 20 to "Super Home Cook 🏆", 40 to "Amazing Home Cook ⭐", 60 to "Stellar Chef 🌿",
        80 to "Executive Chef 🌿", 100 to "Head Chef 🌿", 120 to "Culinary Master 🌿", 150 to "Master Chef 🌿",
        200 to "Top Chef 🌿", 250 to "Grand Master Chef 👑", 500 to "Legendary Chef 👑",
        1500 to "Super Legendary Chef 👑", 3000 to "Grand Legendary Chef 👑",
    )
    val nextIdx = ladder.indexOfFirst { total < it.first }
    if (nextIdx == -1) return LadderInfo(1f, "Grand Legendary Chef unlocked! 👑", "You've reached the top — legend!")
    val next      = ladder[nextIdx]
    val prev      = ladder.getOrNull(nextIdx - 1)
    val prevNeed  = prev?.first ?: 0
    val remaining = next.first - total
    val frac      = (total - prevNeed).coerceAtLeast(0).toFloat() / (next.first - prevNeed).coerceAtLeast(1)
    return LadderInfo(frac, prev?.let { "${it.second.substringBeforeLast(' ')} earned! ${it.second.substringAfterLast(' ')}" },
        "$remaining more meal${if (remaining == 1) "" else "s"} to ${next.second}")
}

// ── 🎉 Congrats banner ───────────────────────────────────────────────────────

@Composable
private fun AchievementBanner(reward: StreakReward, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(10.dp, RoundedCornerShape(14.dp), ambientColor = Navy.copy(alpha = 0.12f), spotColor = Navy.copy(alpha = 0.12f)),
        shape    = RoundedCornerShape(14.dp),
        color    = White,
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(reward.icon.vector(), null, tint = reward.accent(), modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("🎉 Congrats!", color = Navy, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("You earned yourself a ${reward.title}!", color = Navy, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
            }
            Box(
                modifier         = Modifier.size(22.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.15f)).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "Dismiss", tint = Slate, modifier = Modifier.size(11.dp))
            }
        }
    }
}
