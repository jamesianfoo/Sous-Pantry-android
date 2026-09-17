package com.souspantry.app.ui.funnel

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.BuildConfig
import com.souspantry.app.ui.paywall.PaywallScreen
import com.souspantry.app.ui.theme.*
import kotlinx.coroutines.delay

private val CtaGradient = Brush.linearGradient(listOf(Color(0xFF2D5A3D), Color(0xFF6B7A40)))

@Composable
fun FunnelScreen(
    onComplete : () -> Unit,
    vm         : FunnelViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    when (state.phase) {
        FunnelPhase.STEPS    -> FunnelStepsScreen(state, vm)
        FunnelPhase.BUILDING -> BuildingOverlay(state) { vm.advancePhase(FunnelPhase.REMINDER) }
        FunnelPhase.REMINDER -> TrialReminderScreen(state, vm) { vm.advancePhase(FunnelPhase.PAYWALL) }
        FunnelPhase.PAYWALL  -> Box {
            // HARD paywall during onboarding — no close (deliberate; mirrors iOS).
            PaywallScreen(
                onPurchased = { vm.complete(onComplete) },
                onDismiss   = { /* unreachable: close hidden */ },
                showClose   = false,
            )
            if (BuildConfig.DEBUG) {
                Text(
                    "DEBUG: skip paywall",
                    color      = Color.White.copy(alpha = 0.6f),
                    fontSize   = 11.sp,
                    modifier   = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .clickable { vm.complete(onComplete) },
                )
            }
        }
    }
}

// ── Steps engine ─────────────────────────────────────────────────────────────

@Composable
private fun FunnelStepsScreen(state: FunnelState, vm: FunnelViewModel) {
    val step = state.currentStep

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        // Top bar: back circle only, hidden on the first step. No skip anywhere.
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            if (state.stepIndex > 0) {
                Surface(
                    shape    = CircleShape,
                    color    = Color.White,
                    border   = BorderStroke(0.5.dp, Slate.copy(alpha = 0.25f)),
                    modifier = Modifier.size(44.dp).clickable { vm.back() },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Navy, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            when (step.kind) {
                StepKind.INFO, StepKind.FINALE -> InfoStep(step)
                else -> {
                    Text(step.title, color = Navy, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    if (step.subtitle.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(step.subtitle, color = Slate, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(20.dp))
                    when (step.kind) {
                        StepKind.SINGLE        -> step.options.forEach { OptionRow(it, state.singles[step.id] == it.id) { vm.selectSingle(step.id, it.id) } }
                        StepKind.MULTI         -> step.options.forEach { OptionRow(it, (state.multis[step.id] ?: emptySet()).contains(it.id)) { vm.toggleMulti(step.id, it.id, step.max) } }
                        StepKind.TEXT          -> FunnelTextField(state.name, vm::setName, "Enter here")
                        StepKind.OTHER_COUNTRY -> OtherCountryStep(state, vm)
                        StepKind.STEPPER       -> StepperRow(state.household, step.min, step.max, "people", vm::setHousehold)
                        StepKind.SLIDER        -> BudgetSlider(state, step, vm::setBudget)
                        else -> {}
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // Dots directly above CTA: current 18×6 green capsule, others 6×6 navy 18%.
        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        ) {
            state.visibleSteps.forEachIndexed { i, _ ->
                Box(
                    Modifier
                        .size(width = if (i == state.stepIndex) 18.dp else 6.dp, height = 6.dp)
                        .clip(CircleShape)
                        .background(if (i == state.stepIndex) Green else Navy.copy(alpha = 0.18f)),
                )
            }
        }

        CtaButton(
            label   = if (step.kind == StepKind.FINALE) "Generate Plan" else "Continue",
            enabled = state.canContinue,
        ) { vm.next() }
    }
}

@Composable
private fun CtaButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) CtaGradient else Brush.linearGradient(listOf(Slate.copy(alpha = 0.3f), Slate.copy(alpha = 0.3f))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun OptionRow(option: FunnelOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (selected) Green.copy(alpha = 0.10f) else Color.White,
        border   = BorderStroke(if (selected) 1.5.dp else 0.5.dp, if (selected) Green else Slate.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (option.emoji.isNotBlank()) { Text(option.emoji, fontSize = 20.sp); Spacer(Modifier.width(12.dp)) }
            Text(option.label, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (option.sublabel.isNotBlank()) Text(option.sublabel, color = Slate, fontSize = 13.sp)
        }
    }
}

@Composable
private fun FunnelTextField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Surface(shape = RoundedCornerShape(12.dp), color = Parchment, modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = value, onValueChange = onChange, singleLine = true,
            placeholder = { Text(placeholder, color = Slate.copy(alpha = 0.5f)) },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent, focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OtherCountryStep(state: FunnelState, vm: FunnelViewModel) {
    val context = LocalContext.current
    FunnelTextField(state.otherCountry, vm::setOtherCountry, "Your country")
    Spacer(Modifier.height(16.dp))
    Text(
        "Tell us where you are →",
        color = Green, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:hello@jamesfoodesign.com")).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Sous Pantry — shop support for my country")
                putExtra(Intent.EXTRA_TEXT, "Hi! I'm in ${state.otherCountry.ifBlank { "(country)" }} — please add local shops for us.")
            }
            runCatching { context.startActivity(intent) }
        },
    )
}

@Composable
private fun StepperRow(value: Int, min: Int, max: Int, unit: String, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        StepperButton(Icons.Filled.Remove, value > min) { onChange(value - 1) }
        Text(
            "$value $unit", color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.width(140.dp),
        )
        StepperButton(Icons.Filled.Add, value < max) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape)
            .background(if (enabled) SoftMint else Slate.copy(alpha = 0.12f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) Green else Slate.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)) }
}

@Composable
private fun BudgetSlider(state: FunnelState, step: FunnelStep, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text(
            "${state.currencySymbol}${state.budget}",
            color = Green, fontSize = 34.sp, fontWeight = FontWeight.Bold,
        )
        Slider(
            value = state.budget.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = step.min.toFloat()..step.max.toFloat(),
            colors = SliderDefaults.colors(thumbColor = Green, activeTrackColor = Green),
        )
    }
}

@Composable
private fun InfoStep(step: FunnelStep) {
    val info = step.info ?: return
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(info.emoji, fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text(info.title, color = Navy, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (info.stat.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(info.stat, color = Green, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(info.caption, color = Navy, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(16.dp))
        Text(info.body, color = Slate, fontSize = 16.sp, textAlign = TextAlign.Center)
        if (info.sourceName.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Text(
                info.sourceName, color = Slate.copy(alpha = 0.7f), fontSize = 12.sp,
                modifier = Modifier.clickable { runCatching { uriHandler.openUri(info.sourceUrl) } },
            )
        }
    }
}

// ── Finale sequence ──────────────────────────────────────────────────────────

@Composable
private fun BuildingOverlay(state: FunnelState, onDone: () -> Unit) {
    val moods    = (state.multis["moods"] ?: emptySet()).toList()
    val moodRows = FUNNEL_STEPS.first { it.id == "moods" }.options
        .filter { it.id in moods }.take(2).map { "Finding ${it.label.lowercase()} ideas" }
    val rows = if (moodRows.isEmpty())
        listOf("Lining up your dinners", "Matching your budget & shop", "Building your grocery list")
    else moodRows + "Building your grocery list"

    var ticked by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        delay(900);  ticked = 1
        delay(1000); ticked = 2
        delay(1000); ticked = 3
        delay(700);  onDone()   // advance at 3.6s total
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Cream).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (state.name.isBlank()) "we're building your week" else "${state.name}, we're building your week",
            color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text("🛒", fontSize = 64.sp)
        Spacer(Modifier.height(32.dp))
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                rows.forEachIndexed { i, label ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(
                            if (ticked > i) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            null, tint = if (ticked > i) Green else Slate.copy(alpha = 0.3f), modifier = Modifier.size(22.dp),
                        )
                        Text(label, color = Navy, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TrialReminderScreen(state: FunnelState, vm: FunnelViewModel, onContinue: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(64.dp))
            Text("🔔", fontSize = 56.sp)
            Spacer(Modifier.height(20.dp))
            Text("We'll remind you, promise", color = Navy, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                "Before your free trial ends, we'll send you a heads-up so you can decide with calm.",
                color = Slate, fontSize = 15.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            ReminderRow("1 day before", "Recommended: time to plan ahead", state.trialReminder == "1_day") { vm.setTrialReminder("1_day") }
            ReminderRow("2 days before", "Extra peace of mind", state.trialReminder == "2_days") { vm.setTrialReminder("2_days") }
            Spacer(Modifier.height(20.dp))
            Text("Easy to cancel. No penalties. No surprises.", color = Slate.copy(alpha = 0.7f), fontSize = 12.sp)
        }
        CtaButton("Continue", enabled = true, onClick = onContinue)
    }
}

@Composable
private fun ReminderRow(title: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = if (selected) Green.copy(alpha = 0.10f) else Color.White,
        border   = BorderStroke(if (selected) 1.5.dp else 0.5.dp, if (selected) Green else Slate.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = Navy, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = Slate, fontSize = 13.sp)
        }
    }
}
