package com.souspantry.app.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.Canvas
import coil.compose.AsyncImage
import com.souspantry.app.data.models.PantryItem
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.theme.*
import java.util.Calendar

@Composable
fun HomeScreen(
    onNavigateToTab : (String) -> Unit = {},
    vm              : HomeViewModel    = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(
        modifier            = Modifier
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
            )

            if (state.pantryItems.isNotEmpty()) {
                PantryHealthCard(
                    totalItems        = state.pantryItems.size,
                    categoryBreakdown = state.categoryBreakdown,
                    atRiskCount       = state.auditItems.size,
                )
            }

            // Suggested recipes — keeps the existing API wiring
            SuggestedSection(
                state    = state,
                onRetry  = { vm.loadSuggested() },
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ── Hero ─────────────────────────────────────────────────────────────────────

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
            .height(280.dp)
            .background(
                Brush.verticalGradient(
                    listOf(SoftMint.copy(alpha = 0.6f), Cream),
                ),
            ),
    ) {
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

// ── Recent Haul (Navy card with CTAs) ────────────────────────────────────────

@Composable
private fun RecentHaulCard(
    pantryIsEmpty       : Boolean,
    estimatedMealCount  : Int,
    onAddItems          : () -> Unit,
    onSeeWhatToCook     : () -> Unit,
    onViewShopping      : () -> Unit,
) {
    val mealLabel = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..10  -> "TODAY'S BREAKFAST"
            in 11..14 -> "TODAY'S LUNCH"
            else      -> "TONIGHT'S DINNER"
        }
    }
    val lime = Color(0xFF2CFF04)

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(18.dp),
        color           = Navy,
        shadowElevation = 12.dp,
    ) {
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
                        color    = lime.copy(alpha = 0.15f),
                        modifier = Modifier,
                    ) {
                        Text(
                            mealLabel,
                            color      = lime,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            // Subtitle
            Text(
                "No scan recorded yet.",
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
                CtaButton(label = "Add new items →", primary = true,  onClick = onAddItems)
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
                        segments  = categoryBreakdown.take(4).mapIndexed { idx, (_, frac, _) -> frac to donutColor(idx) },
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
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(donutColor(idx)))
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

private fun donutColor(idx: Int): Color = when (idx) {
    0 -> Color(0xFF2D5A3D) // Green
    1 -> Color(0xFFC4965A) // Gold
    2 -> Color(0xFF647080) // Slate
    else -> Color(0xFFE3EDE6) // SoftMint
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
            color     = baseColor,
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

// ── Suggested recipes carousel ───────────────────────────────────────────────

@Composable
private fun SuggestedSection(state: HomeState, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("✨", fontSize = 18.sp)
            Text(
                "Suggested for You",
                color      = Navy,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        when {
            state.suggestedLoading -> {
                Box(
                    modifier         = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = Green) }
            }
            state.suggestedError -> {
                Surface(
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(14.dp),
                    color           = Color.White,
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier            = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Couldn't load recipes", color = Navy, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Check your connection and try again.", color = Slate, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.suggestedRecipes) { meal ->
                        RecipeCard(meal)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeCard(meal: SuggestedMeal) {
    val imageUrl = "https://loremflickr.com/400/300/${meal.imageQuery.replace(" ", ",")},food"
    Surface(
        modifier        = Modifier.width(200.dp).shadow(
            elevation = 4.dp,
            shape     = RoundedCornerShape(16.dp),
        ),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                AsyncImage(
                    model              = imageUrl,
                    contentDescription = meal.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
                // Subtle bottom scrim for text contrast over the image edge
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))),
                    ),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(meal.title, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(meal.description, color = Slate, fontSize = 12.sp, maxLines = 2)
            }
        }
    }
}
