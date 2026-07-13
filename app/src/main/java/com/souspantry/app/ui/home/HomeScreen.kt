package com.souspantry.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.souspantry.app.R
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.ui.theme.*
import java.util.Calendar

// ── Color palette for donut chart (mirrors iOS) ──────────────────────────────
private val DONUT_PALETTE = listOf(
    Color(0xFF2D5A3D), // Green
    Color(0xFF162437), // Navy
    Color(0xFFC4965A), // Gold
    Color(0xFFFF9800), // Orange
    Color(0xFF8C3FBF), // Purple (iOS rgb(0.55, 0.25, 0.75))
)

private val LIME_HIGHLIGHT = Color(0xFF2CFF05)

@Composable
fun HomeScreen(
    onNavigateToTab : (String) -> Unit = {},
    vm              : HomeViewModel    = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Hero ─────────────────────────────────────────────────
        HeroSection(userName = state.userName)

        // ── Stacked content cards ────────────────────────────────
        Column(
            modifier            = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            if (state.auditItems.isNotEmpty()) {
                PantryAuditCard(items = state.auditItems)
            }

            RecentHaulCard(
                pantryIsEmpty       = state.pantryItems.isEmpty(),
                estimatedMealCount  = state.estimatedRecipeCount,
                onAddItems          = { onNavigateToTab("pantry") },
                onSeeWhatToCook     = { onNavigateToTab("plancook") },
                onViewShopping      = { onNavigateToTab("shopping") },
                onPriceTrendsTap    = { /* Price Trends sheet — future */ },
            )

            if (state.pantryItems.isNotEmpty()) {
                PantryHealthCard(
                    totalItems        = state.pantryItems.size,
                    categoryBreakdown = state.categoryBreakdown,
                    atRiskCount       = state.auditItems.size,
                )
            }

            CookingStreakCard(
                mealsThisWeek    = state.mealsThisWeek,
                totalMealsCooked = state.totalMealsCooked,
                currentStreak    = state.currentStreak,
                weeklySavings    = state.weeklySavings,
                cookedWeekdays   = state.cookedWeekdays,
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Hero (full-bleed vegetable image + cream gradient overlay) ───────────────

@Composable
private fun HeroSection(userName: String) {
    val firstName = userName.split(" ").firstOrNull()?.takeIf { it.isNotBlank() } ?: "Chef"
    val greeting  = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            else      -> "Good evening,"
        }
    }
    val deepGreen = Color(0xFF0A2A1F)
    val mutedGray = Color(0xFF5F5E5A)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
    ) {
        // 1. Vegetable backdrop (full-bleed, top edge to bottom)
        androidx.compose.foundation.Image(
            painter            = painterResource(id = R.drawable.hero_vegetables),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier.fillMaxSize(),
        )

        // 2. Cream gradient overlay — image fades into the cream background.
        //    Inverse of iOS overlay stops: 35% at top → 65% middle → 100% bottom.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Cream.copy(alpha = 0.35f),
                            0.5f to Cream.copy(alpha = 0.65f),
                            1.0f to Cream,
                        ),
                    ),
                ),
        )

        // 3. Greeting block — bottom-left of hero
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(greeting, color = mutedGray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(firstName, color = deepGreen, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Pantry Audit Due (Navy card) ─────────────────────────────────────────────

@Composable
private fun PantryAuditCard(items: List<PantryItem>) {
    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(18.dp),
        color           = Navy,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier            = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Filled.Warning, null, tint = Gold)
                Text("Pantry Audit Due", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                "${items.size} item${if (items.size == 1) "" else "s"} need review",
                color    = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.take(4).forEach { AuditRow(it) }
            }
        }
    }
}

@Composable
private fun AuditRow(item: PantryItem) {
    val nowMs = System.currentTimeMillis()
    val days  = item.expiryDate?.let { ((it - nowMs) / 86_400_000L).toInt() } ?: 0
    val (label, dotColor) = when {
        days < 0  -> "Expired"     to Color(0xFFE53935)
        days == 0 -> "Today"       to Color(0xFFFFB74D)
        days <= 3 -> "$days days"  to Color(0xFFFFB74D)
        else      -> "$days days"  to Color(0xFFFFD54F)
    }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Text(item.name, color = Color.White, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── Recent Haul (Navy card with CTAs + Price Trends row) ─────────────────────

@Composable
private fun RecentHaulCard(
    pantryIsEmpty       : Boolean,
    estimatedMealCount  : Int,
    onAddItems          : () -> Unit,
    onSeeWhatToCook     : () -> Unit,
    onViewShopping      : () -> Unit,
    onPriceTrendsTap    : () -> Unit,
) {
    val mealLabel = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10  -> "TODAY'S BREAKFAST"
            in 11..14 -> "TODAY'S LUNCH"
            else      -> "TONIGHT'S DINNER"
        }
    }

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(18.dp),
        color           = Navy,
        shadowElevation = 12.dp,
    ) {
        Column {
            // ── Top section ──────────────────────────────────────
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Title row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🛒", fontSize = 22.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("Recent Haul", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    if (!pantryIsEmpty) {
                        Surface(
                            shape    = CircleShape,
                            color    = LIME_HIGHLIGHT.copy(alpha = 0.15f),
                        ) {
                            Text(
                                mealLabel,
                                color      = LIME_HIGHLIGHT,
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                // Subtitle
                Text(
                    if (pantryIsEmpty) "No scan recorded yet." else "Last recipe scan was yesterday.",
                    color    = Color.White,
                    fontSize = 12.sp,
                )

                if (pantryIsEmpty) {
                    Text(
                        "Let's get you started",
                        color      = Color.White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    CtaButton(label = "Add new items →",      primary = true,  onClick = onAddItems)
                    CtaButton(label = "View shopping list →", primary = false, onClick = onViewShopping)
                } else {
                    Text(
                        "$estimatedMealCount meals you could cook right now. Hungry?",
                        color      = Color.White,
                        fontSize   = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    CtaButton(label = "See what to cook →", primary = true, onClick = onSeeWhatToCook)
                }
            }

            // ── Price Trends row (hidden when empty) ─────────────
            if (!pantryIsEmpty) {
                HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.12f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onPriceTrendsTap)
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "PRICE TRENDS",
                            color      = Color.White.copy(alpha = 0.5f),
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Tap to see spend insights",
                            color    = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = 0.4f),
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CtaButton(label: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape  = RoundedCornerShape(12.dp),
        color  = if (primary) Green else Color.White.copy(alpha = 0.10f),
    ) {
        Box(
            modifier         = Modifier
                .fillMaxWidth()
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                color      = if (primary) Color.White else Color.White.copy(alpha = 0.85f),
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
        modifier        = Modifier.fillMaxWidth().shadow(
            elevation    = 6.dp,
            shape        = RoundedCornerShape(16.dp),
            ambientColor = Navy.copy(alpha = 0.08f),
            spotColor    = Navy.copy(alpha = 0.08f),
        ),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🫙", fontSize = 20.sp)
                Text("Pantry Health", color = Navy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                verticalAlignment       = Alignment.CenterVertically,
                horizontalArrangement   = Arrangement.spacedBy(20.dp),
            ) {
                // Donut
                Box(
                    modifier         = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    DonutChart(
                        segments  = categoryBreakdown.take(5).mapIndexed { idx, (_, frac, _) -> frac to DONUT_PALETTE[idx % DONUT_PALETTE.size] },
                        strokeDp  = 18,
                        baseColor = SoftMint,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$totalItems", color = Navy, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("items", color = Slate, fontSize = 10.sp)
                    }
                }
                // Legend + at risk
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categoryBreakdown.take(4).forEachIndexed { idx, (name, frac, _) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(DONUT_PALETTE[idx % DONUT_PALETTE.size]))
                            Text(name, color = Navy, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("${(frac * 100).toInt()}%", color = Slate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.2f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (atRiskCount > 0) {
                            Icon(Icons.Filled.Warning, null, tint = Gold, modifier = Modifier.size(14.dp))
                            Text(
                                "~$${"%.0f".format(atRiskCount * 4.50)} at risk",
                                color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            )
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
private fun DonutChart(
    segments  : List<Pair<Float, Color>>,
    strokeDp  : Int,
    baseColor : Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidthPx = strokeDp.dp.toPx()
        val pad           = strokeWidthPx / 2f
        val arcSize       = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

        // Base ring
        drawArc(
            color      = baseColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter  = false,
            topLeft    = Offset(pad, pad),
            size       = arcSize,
            style      = Stroke(width = strokeWidthPx),
        )

        // Coloured arcs (start at -90° = top)
        var start = -90f
        segments.forEach { (frac, color) ->
            val sweep = frac * 360f
            drawArc(
                color      = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter  = false,
                topLeft    = Offset(pad, pad),
                size       = arcSize,
                style      = Stroke(width = strokeWidthPx),
            )
            start += sweep
        }
    }
}

// ── Cooking Streak card ──────────────────────────────────────────────────────

@Composable
private fun CookingStreakCard(
    mealsThisWeek    : Int,
    totalMealsCooked : Int,
    currentStreak    : Int,
    weeklySavings    : Double,
    cookedWeekdays   : Set<Int>,
) {
    Surface(
        modifier        = Modifier.fillMaxWidth().shadow(
            elevation    = 6.dp,
            shape        = RoundedCornerShape(16.dp),
            ambientColor = Navy.copy(alpha = 0.08f),
            spotColor    = Navy.copy(alpha = 0.08f),
        ),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
    ) {
        Column(
            modifier            = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Section 1: meals + savings + progress ─────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Title row + streak flame pill (≥3 days)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🍳", fontSize = 16.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Cooking streak", color = Navy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    if (currentStreak >= 3) {
                        Surface(shape = CircleShape, color = Color(0xFFFF9800).copy(alpha = 0.12f)) {
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier              = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            ) {
                                Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF9800), modifier = Modifier.size(12.dp))
                                Text(
                                    "$currentStreak day${if (currentStreak == 1) "" else "s"}",
                                    color = Color(0xFFFF9800), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }

                // Meals + savings
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "$mealsThisWeek meal${if (mealsThisWeek == 1) "" else "s"} cooked this week",
                        color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    )
                    Surface(shape = CircleShape, color = SoftMint) {
                        Text(
                            "~ $${"%.2f".format(weeklySavings)} saved from eating out",
                            color      = Green,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                // Progress toward next badge
                val (fraction, goalText) = nextBadgeInfo(totalMealsCooked)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Slate.copy(alpha = 0.15f)),
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Green))
                    }
                    Text(goalText, color = Slate, fontSize = 12.sp)
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))

            // ── Section 2: daily streak dots ──────────────────────────────
            DailyStreakSection(cookedWeekdays = cookedWeekdays, currentStreak = currentStreak)
        }
    }
}

@Composable
private fun DailyStreakSection(cookedWeekdays: Set<Int>, currentStreak: Int) {
    val labels   = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    // Mon = 0 … Sun = 6
    val todayIdx = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DAILY STREAK", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.forEachIndexed { i, label ->
                val isToday  = i == todayIdx
                val isCooked = cookedWeekdays.contains(i)
                val isFuture = i > todayIdx
                val bg = when {
                    isCooked -> Green
                    isToday  -> Navy
                    isFuture -> Color.Gray.copy(alpha = 0.15f)
                    else     -> Color.Gray.copy(alpha = 0.25f)
                }
                val fg = if (isCooked || isToday) Color.White else Slate
                Box(
                    modifier         = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(bg).padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Text(streakDayMessage(currentStreak), color = Navy, fontSize = 13.sp)
    }
}

private fun streakDayMessage(streak: Int): String = when (streak) {
    0       -> "Cook today to start a streak. Keep going!"
    1       -> "1-day streak — Great start! Keep going!"
    in 2..6 -> "$streak-day streak 🔥 Keep going!"
    in 7..13-> "$streak-day streak 🔥 You're on fire! Keep going!"
    else    -> "$streak-day streak 🔥 You're unstoppable!"
}

/** Mirrors iOS badge ladder. Returns (progressFraction, goalText). */
private fun nextBadgeInfo(total: Int): Pair<Float, String> {
    val ladder = listOf(
        5    to "Home Cook 🏅",
        20   to "Super Home Cook 🏆",
        40   to "Amazing Home Cook ⭐",
        60   to "Stellar Chef 🌿",
        80   to "Executive Chef 🌿",
        100  to "Head Chef 🌿",
        120  to "Culinary Master 🌿",
        150  to "Master Chef 🌿",
        200  to "Top Chef 🌿",
        250  to "Grand Master Chef 👑",
        500  to "Legendary Chef 👑",
        1500 to "Super Legendary Chef 👑",
        3000 to "Grand Legendary Chef 👑",
    )
    val nextIdx = ladder.indexOfFirst { total < it.first }
    if (nextIdx == -1) {
        return 1.0f to "You've reached the top — legend!"
    }
    val next      = ladder[nextIdx]
    val prevNeed  = if (nextIdx == 0) 0 else ladder[nextIdx - 1].first
    val remaining = next.first - total
    val range     = (next.first - prevNeed).coerceAtLeast(1).toFloat()
    val frac      = ((total - prevNeed).coerceAtLeast(0).toFloat()) / range
    return frac to "$remaining more meal${if (remaining == 1) "" else "s"} to ${next.second}"
}

