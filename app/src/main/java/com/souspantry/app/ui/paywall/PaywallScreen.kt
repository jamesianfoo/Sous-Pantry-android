package com.souspantry.app.ui.paywall

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

private val Gold1 = Color(0xFFEAC77D)
private val Gold2 = Color(0xFFC4965A)
private val PaywallNavy = Color(0xFF11192A)

private val BENEFITS = listOf(
    "Household pantry sync",
    "Unlimited AI-powered features",
    "Add my own recipes",
    "Save suggested recipes",
    "Meal planner",
    "Digital receipt sync",
    "Cloud sync & backup",
    "Family sharing",
    "Ad-free experience",
)

@Composable
fun PaywallScreen(
    onPurchased : () -> Unit,
    onDismiss   : () -> Unit,
    vm          : PaywallViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF12192A), Color(0xFF1A2A3D), Color(0xFF21333F)),
                ),
            ),
    ) {
        // ── Scrollable content ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
        ) {
            // Header row
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Gold1, modifier = Modifier.size(14.dp))
                        Text("SOUS PANTRY PREMIUM", color = Gold1, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.6.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Your pantry,", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("fully in", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("control.", color = Gold1, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("7-day free trial included.", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Close", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(18.dp))

            // Benefits
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                BENEFITS.forEach { benefit ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier         = Modifier.size(22.dp).clip(CircleShape).background(Gold1.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Check, null, tint = Gold1, modifier = Modifier.size(12.dp))
                        }
                        Text(benefit, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Plan cards
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PAYWALL_PLANS.forEach { plan ->
                    PlanCard(
                        plan       = plan,
                        isSelected = state.selectedId == plan.id,
                        onClick    = { vm.select(plan.id) },
                        modifier   = Modifier.weight(1f),
                    )
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(8.dp), color = Color.Red.copy(alpha = 0.2f)) {
                    Text(it, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
        }

        // ── Fixed bottom CTA + legal ────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val plan = state.selected
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = plan != null && !state.processing) { vm.purchase(onPurchased) },
                shape    = RoundedCornerShape(14.dp),
                color    = Color.Transparent,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (plan != null) Brush.linearGradient(listOf(Gold1, Gold2))
                            else Brush.linearGradient(listOf(Color.White.copy(0.15f), Color.White.copy(0.15f))),
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.processing) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = PaywallNavy, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            Text("Processing…", color = PaywallNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (plan != null) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Continue · ${plan.price} / ${plan.periodShort}", color = PaywallNavy, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("after 7-day free trial · cancel anytime", color = PaywallNavy.copy(alpha = 0.75f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    } else {
                        Text("Continue", color = Color.White.copy(0.5f), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Legal disclosure
            Text(
                "Subscriptions automatically renew unless cancelled at least 24 hours before the end of the current period. Manage or cancel anytime in Google Play subscriptions.",
                color     = Color.White.copy(alpha = 0.6f),
                fontSize  = 10.sp,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LegalLink("Terms of Use", "https://souspantry.com/terms.html", context)
                Text("·", color = Color.White.copy(alpha = 0.5f), fontSize = 10.sp)
                LegalLink("Privacy Policy", "https://souspantry.com/privacy.html", context)
            }

            Spacer(Modifier.height(12.dp))

            // Maybe Later + Restore
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    "Maybe Later",
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(8.dp),
                )
                Text(
                    "Restore Purchase",
                    color    = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { vm.restore(onPurchased) }.padding(8.dp),
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan       : PaywallPlan,
    isSelected : Boolean,
    onClick    : () -> Unit,
    modifier   : Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = if (isSelected) 0.14f else 0.06f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Gold1 else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    plan.period.uppercase(),
                    color      = if (isSelected) Gold1 else Color.White.copy(alpha = 0.55f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    modifier   = Modifier.weight(1f),
                )
                plan.savings?.let { pct ->
                    Surface(shape = CircleShape, color = Gold1) {
                        Text("SAVE $pct%", color = PaywallNavy, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Text(plan.price, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(plan.subtitle, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp)
            Text("7-day free trial", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LegalLink(label: String, url: String, context: android.content.Context) {
    Text(
        label,
        color      = Color.White.copy(alpha = 0.7f),
        fontSize   = 10.sp,
        fontWeight = FontWeight.Medium,
        modifier   = Modifier.clickable {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
    )
}
