package com.souspantry.app.ui.setup

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NoMeals
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

private data class WizardOption(
    val id     : String,
    val icon   : ImageVector,
    val title  : String,
    val detail : String,
)

private val GOAL_OPTIONS = listOf(
    WizardOption("pantry",  Icons.Outlined.Kitchen,           "Manage my pantry well",       "Track ingredients and reduce food waste"),
    WizardOption("recipes", Icons.AutoMirrored.Filled.MenuBook,"Recommend recipes I can make","AI meal ideas using what you already have"),
    WizardOption("staples", Icons.Filled.Checklist,           "Start with staple ingredients","We'll pre-fill your pantry with essentials"),
)

private val DIETARY_OPTIONS = listOf(
    WizardOption("vegan",       Icons.Outlined.Eco,   "Vegan",         "No meat, fish, dairy, or eggs"),
    WizardOption("vegetarian",  Icons.Outlined.Spa,   "Vegetarian",    "No meat or fish"),
    WizardOption("pescatarian", Icons.Filled.SetMeal, "Pescatarian",   "Meat-free, fish and seafood allowed"),
    WizardOption("no_beef",     Icons.Filled.Warning, "No Beef",       "No beef or veal in any recipe"),
    WizardOption("no_pork",     Icons.Filled.Warning, "No Pork",       "No pork or pork-derived products"),
    WizardOption("none",        Icons.Filled.CheckCircle, "No preference", "I eat everything"),
)

private val RESTRICTION_OPTIONS = listOf(
    WizardOption("gluten_free",    Icons.Outlined.BakeryDining, "Gluten Free",              "No wheat, barley, or rye"),
    WizardOption("nuts_free",      Icons.Outlined.Grass,        "Nut Free",                 "No tree nuts or peanuts"),
    WizardOption("lactose_free",   Icons.Filled.WaterDrop,      "Lactose Free",             "No dairy or lactose products"),
    WizardOption("shellfish_free", Icons.Filled.SetMeal,        "Shellfish / Seafood Free", "No shellfish or seafood"),
    WizardOption("pregnancy",      Icons.Filled.Favorite,       "Pregnancy Friendly",       "Safe for expectant mothers"),
    WizardOption("none",           Icons.Filled.NoMeals,        "No restrictions",          "No food restrictions"),
)

@Composable
fun SetupWizardScreen(
    onComplete : () -> Unit,
    vm         : SetupWizardViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        // ── Top bar (navy, progress dots) ───────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth().background(Navy).padding(top = 48.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val w by animateDpAsState(if (i == state.step) 24.dp else 8.dp, label = "dotW")
                    Box(
                        modifier = Modifier
                            .width(w)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (i <= state.step) Green else Color.White.copy(alpha = 0.2f)),
                    )
                }
            }
            Text("Step ${state.step + 1} of 3", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }

        // ── Step content ─────────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            when (state.step) {
                0 -> {
                    StepHeader("What do you want to do?", "Select all that apply — you can always change this later.")
                    GOAL_OPTIONS.forEach { opt ->
                        WizardCard(opt, state.selectedGoals.contains(opt.id)) { vm.toggleGoal(opt.id) }
                    }
                }
                1 -> {
                    StepHeader("Are you…?", "Choose the option that best describes your diet.")
                    DIETARY_OPTIONS.forEach { opt ->
                        WizardCard(opt, state.selectedDietary == opt.id) { vm.selectDietary(opt.id) }
                    }
                }
                2 -> {
                    StepHeader("Any food restrictions?", "Select all that apply. We'll keep every recipe safe for you.")
                    RESTRICTION_OPTIONS.forEach { opt ->
                        WizardCard(opt, state.selectedRestrictions.contains(opt.id)) { vm.toggleRestriction(opt.id) }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Bottom navigation ────────────────────────────────────────────
        Surface(color = Color.White, shadowElevation = 8.dp) {
            Row(
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.step > 0) {
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable { vm.back() },
                        shape    = RoundedCornerShape(14.dp),
                        color    = SoftMint,
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment     = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.ChevronLeft, null, tint = Navy, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back", color = Navy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                val isLast = state.step == 2
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(enabled = state.canContinue && !state.finishing) {
                            if (isLast) vm.finish(onComplete) else vm.next()
                        },
                    shape    = RoundedCornerShape(14.dp),
                    color    = if (state.canContinue) Green else Slate.copy(alpha = 0.3f),
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().padding(vertical = 15.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment     = Alignment.CenterVertically,
                    ) {
                        Text(if (isLast) "Get Started" else "Continue", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (isLast) Icons.Filled.AutoAwesome else Icons.Filled.ChevronRight,
                            null, tint = Color.White, modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Slate, fontSize = 14.sp)
    }
}

@Composable
private fun WizardCard(option: WizardOption, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape           = RoundedCornerShape(16.dp),
        color           = Color.White,
        border          = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, Green) else null,
        shadowElevation = if (isSelected) 8.dp else 3.dp,
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier         = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Green.copy(alpha = 0.12f) else SoftMint.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(option.icon, null, tint = if (isSelected) Green else Slate, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(option.title, color = Navy, fontWeight = FontWeight.SemiBold)
                Text(option.detail, color = Slate, fontSize = 12.sp)
            }
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint               = if (isSelected) Green else Slate.copy(alpha = 0.3f),
                modifier           = Modifier.size(24.dp),
            )
        }
    }
}
