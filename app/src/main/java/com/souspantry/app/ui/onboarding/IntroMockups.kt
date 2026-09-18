package com.souspantry.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.R
import com.souspantry.app.ui.theme.*

/**
 * Non-interactive previews of real app screens, shown on the intro carousel —
 * mirrors iOS, which shows a mockup card rather than an emoji. Built from the
 * app's own components so they stay honest when the design system changes.
 */

private val CARD_WIDTH = 300.dp

// Notification orange, matching iOS. Deliberately not the DS Terracotta token,
// which is a browner accent colour.
private val AuditOrange = Color(0xFFF5821F)

@Composable
private fun MockCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier        = Modifier.width(CARD_WIDTH),
        shape           = RoundedCornerShape(18.dp),
        color           = Paper,
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun MiniChip(text: String, bg: Color, fg: Color, bold: Boolean = false) {
    Text(
        text,
        color      = fg,
        fontSize   = 9.sp,
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Medium,
        modifier   = Modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** Screen 1 — the brand wordmark, in place of an emoji card. */
@Composable
fun SousWordmark() {
    Image(
        painter            = painterResource(R.drawable.sous_pantry_wordmark),
        contentDescription = "Sous Pantry",
        modifier           = Modifier.width(230.dp),
    )
}

/** Screen 2 — My Pantry. */
@Composable
fun PantryMockup() {
    MockCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text("My Pantry", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("14 items in stock", color = Slate, fontSize = 10.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Add", color = Green, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text("Select", color = Green, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MiniChip("All", Navy, Color.White, bold = true)
            MiniChip("Expiring (3)", Gold.copy(alpha = 0.18f), Gold, bold = true)
            MiniChip("Fruits", Cream, Slate)
        }
        PantryRow("🍇", "Red Grapes", "1.185 kg · Exp. 28 Apr", Slate)
        PantryRow("🫐", "Blackberries", "125 g · Exp. in 4d", Slate)
        PantryRow("🥬", "Baby Spinach", "200 g · Exp. today", Danger)
    }
}

@Composable
private fun PantryRow(emoji: String, name: String, detail: String, detailColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Cream),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 15.sp) }
        Column(Modifier.weight(1f)) {
            Text(name, color = Navy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = detailColor, fontSize = 10.sp)
        }
    }
}

/** Screen 3 — Plan & Cook chat with a recipe card. */
@Composable
fun PlanCookMockup() {
    MockCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                "Pasta",
                color    = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Green)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Eco, null, tint = Green, modifier = Modifier.size(14.dp))
            Text(
                "Great choice! Here are three pasta dishes you can make tonight.",
                color    = Navy,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Cream)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Column(
            modifier            = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Navy).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    MiniChip("Italian", Color.White.copy(alpha = 0.14f), Color.White)
                    MiniChip("Easy", Color.White.copy(alpha = 0.14f), Color.White)
                }
                Icon(Icons.Filled.AutoAwesome, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(9.dp))
                Spacer(Modifier.width(3.dp))
                Text("2 servings", color = Color.White.copy(alpha = 0.7f), fontSize = 9.sp)
            }
            Text("Spaghetti Bolognese", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(CircleShape).background(Green).padding(horizontal = 7.dp, vertical = 3.dp),
                ) {
                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("95% match", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
                InfoBadge("Gluten")
                InfoBadge("Beef")
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MockStat("Prep", "10 mins")
                    MockStat("Cook", "30 mins")
                }
                // Icon and label grouped so they centre on each other, not on the row's baseline.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Sage, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Sous Pantry", color = Sage, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 7.dp, vertical = 3.dp),
    ) {
        Icon(Icons.Filled.Info, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(9.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, color = Color.White.copy(alpha = 0.85f), fontSize = 9.sp)
    }
}

@Composable
private fun MockStat(label: String, value: String) {
    Column {
        Text(label, color = Color.White.copy(alpha = 0.5f), fontSize = 8.sp)
        Text(value, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Screen 4 — My Plans week view. */
@Composable
fun MyPlansMockup() {
    MockCard {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniChip("Discover", Cream, Slate)
            MiniChip("Saved", Cream, Slate)
            MiniChip("My Recipes", Cream, Slate)
            MiniChip("My Plans", Navy, Color.White, bold = true)
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("‹", color = Slate, fontSize = 12.sp)
            Text("  May 4 – May 10  ", color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("›", color = Slate, fontSize = 12.sp)
        }
        PlanDay("Monday", listOf(Triple("Crispy Fried Rice", "Asian", "Easy")))
        PlanDay("Tuesday", listOf(
            Triple("Spaghetti Bolognese", "Italian", "Medium"),
            Triple("Berry Shortcake Bites", "Mixed", "Easy"),
        ))
        PlanDay("Wednesday", listOf(Triple("Thai Red Curry", "Thai", "Medium")))
    }
}

@Composable
private fun PlanDay(day: String, meals: List<Triple<String, String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(day, color = Slate, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        meals.forEach { (name, cuisine, level) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(Cream).padding(8.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = Green, modifier = Modifier.size(9.dp))
                Text(name, color = Navy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                MiniChip(cuisine, Paper, Slate)
                MiniChip(level, Paper, Slate)
            }
        }
    }
}

/** Screen 5 — the Confirm Cooked deduction modal. */
@Composable
fun ConfirmCookedMockup() {
    MockCard {
        Text("Confirm Cooked", color = Navy, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("These ingredients will be deducted from your pantry:", color = Slate, fontSize = 10.sp)
        CookedRow("White rice", "300 g", true)
        CookedRow("Eggs", "3 whole", true)
        CookedRow("Brown onion", "1 whole", true)
        CookedRow("Garlic", "2 cloves", true)
        CookedRow("Soy sauce", "30 ml", false)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Green).padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) { Text("Yes, deduct from pantry", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Cream).padding(vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) { Text("I shopped separately", color = Slate, fontSize = 11.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun CookedRow(name: String, amount: String, checked: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .size(15.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) Green else Color.Transparent)
                .border(1.dp, if (checked) Green else Slate.copy(alpha = 0.4f), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
        }
        Text(name, color = Navy, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(amount, color = Slate, fontSize = 10.sp)
    }
}

/** Screen 6 — the Pantry Audit reminder. */
@Composable
fun PantryAuditMockup() {
    Surface(
        modifier        = Modifier.width(CARD_WIDTH),
        shape           = RoundedCornerShape(18.dp),
        color           = AuditOrange,
        shadowElevation = 10.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Warning, null, tint = Color.White, modifier = Modifier.size(15.dp))
                Text("Pantry Audit Due", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Text("Last checked today · 7 items need review", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.25f)))
            AuditRow("Mince Beef & Pork", "Expires today")
            AuditRow("Mince Beef", "Expires today")
            AuditRow("Bertocchi Soppressata", "Expires tomorrow")
            AuditRow("Jumbo Pack Baby Spinach", "Expires in 2 days")
            Text("+ 3 more items expiring", color = Color.White.copy(alpha = 0.8f), fontSize = 9.5.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = Color.White, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Expiring Items", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun AuditRow(name: String, status: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            Text(name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("Added yesterday", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
        }
        Text(status, color = Color.White, fontSize = 10.sp)
    }
}
