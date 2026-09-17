package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.theme.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Past Discover generations — mirrors iOS MealHistorySheet. Each session is
 * collapsible; "View Recipe" opens the same CookingSessionSheet used from
 * Discover, since the full recipe (ingredients + instructions) is already
 * cached in the session, no re-fetch needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealHistorySheet(
    sessions      : List<MealHistorySession>,
    onDismiss     : () -> Unit,
    onClearAll    : () -> Unit,
    onViewRecipe  : (SuggestedMeal) -> Unit,
) {
    var expandedId by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = Cream,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("History", color = Navy, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (sessions.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear All", color = Color(0xFFB23A48), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "Close", tint = Slate.copy(alpha = 0.5f))
                }
            }

            if (sessions.isEmpty()) {
                Column(
                    modifier             = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    horizontalAlignment  = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Filled.History, null, tint = Slate.copy(alpha = 0.4f), modifier = Modifier.size(44.dp))
                    Text("No history yet", color = Navy, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Your Discover sessions will appear here after your first generation.",
                        color     = Slate,
                        fontSize  = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 32.dp),
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        HistorySessionCard(
                            session      = session,
                            isExpanded   = expandedId == session.id,
                            onToggle     = { expandedId = if (expandedId == session.id) null else session.id },
                            onViewRecipe = onViewRecipe,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistorySessionCard(
    session      : MealHistorySession,
    isExpanded   : Boolean,
    onToggle     : () -> Unit,
    onViewRecipe : (SuggestedMeal) -> Unit,
) {
    val dateTime = remember(session.createdAt) {
        Instant.ofEpochMilli(session.createdAt).atZone(ZoneId.systemDefault())
    }
    val dayLabel  = remember(dateTime) { DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault()).format(dateTime) }
    val timeLabel = remember(dateTime) { DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()).format(dateTime) }

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        shadowElevation = 3.dp,
    ) {
        Column {
            Row(
                modifier          = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(dayLabel, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("·", color = Slate, fontSize = 14.sp)
                        Text(timeLabel, color = Slate, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            "${session.meals.size} recipe${if (session.meals.size == 1) "" else "s"}",
                            color    = Slate,
                            fontSize = 12.sp,
                        )
                        if (session.cuisines.isNotEmpty()) {
                            Text("·", color = Slate, fontSize = 12.sp)
                            Text(
                                session.cuisines.joinToString(", "),
                                color    = Green,
                                fontSize = 12.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
                Icon(
                    if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null,
                    tint = Slate,
                )
            }

            if (isExpanded) {
                HorizontalDivider(thickness = 0.5.dp, color = Slate.copy(alpha = 0.15f))
                Column {
                    session.meals.forEachIndexed { idx, meal ->
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(meal.title, color = Navy, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (meal.difficulty.isNotBlank()) {
                                    Surface(shape = CircleShape, color = historyDifficultyColor(meal.difficulty)) {
                                        Text(
                                            meal.difficulty,
                                            color      = Color.White,
                                            fontSize   = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                if (meal.cuisine.isNotBlank()) {
                                    Text(
                                        meal.cuisine,
                                        color    = Slate,
                                        fontSize = 11.sp,
                                        modifier = Modifier.clip(CircleShape).background(Cream).padding(horizontal = 8.dp, vertical = 3.dp),
                                    )
                                }
                                if (meal.prepTime.isNotBlank()) {
                                    Icon(Icons.Filled.AccessTime, null, tint = Slate, modifier = Modifier.size(11.dp))
                                    Text(meal.prepTime, color = Slate, fontSize = 11.sp)
                                }
                            }
                            Button(
                                onClick  = { onViewRecipe(meal) },
                                modifier = Modifier.fillMaxWidth().height(38.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                                shape    = RoundedCornerShape(10.dp),
                            ) {
                                Text("View Recipe", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (idx < session.meals.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), thickness = 0.5.dp, color = Slate.copy(alpha = 0.12f))
                        }
                    }
                }
            }
        }
    }
}

private fun historyDifficultyColor(difficulty: String): Color = when (difficulty.lowercase()) {
    "easy"   -> Color(0xFF2D5A3D)
    "medium" -> Color(0xFFC4965A)
    "hard"   -> Color(0xFFB23A48)
    else     -> Color(0xFF647080)
}
