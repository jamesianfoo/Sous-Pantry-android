package com.souspantry.app.ui.founder

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.souspantry.app.R
import com.souspantry.app.data.local.UserPreferencesRepository
import com.souspantry.app.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Handwritten font (Oregano) for the personal note — mirrors iOS.
private val Oregano = FontFamily(
    Font(R.font.oregano_regular, style = FontStyle.Normal),
    Font(R.font.oregano_italic,  style = FontStyle.Italic),
)

// Bespoke palette from iOS FounderNoteView (not AppTheme).
private val NoteBg      = Color(0xFFF5F3EF)
private val NoteBody    = Color(0xFF2C2C2A)
private val NoteSignoff = Color(0xFF5F5E5A)
private val NoteLater   = Color(0xFF888780)
private val NoteCta     = Color(0xFF1D9E75)

private const val BODY = """hey there 👋

We built Sous Pantry because we kept standing in front of a full pantry with absolutely no idea what to cook. I wanted something that actually knew what was in our pantry — and could just tell us what to make with it. So we built that.

Every meal suggestion, every recipe, every shopping list is designed to make dinner feel less like a puzzle and more like something you've got under control.

Try it free for 7 days. No pressure, no gotchas. If it makes your week a little easier, I'd love to have you.

Love,"""

@HiltViewModel
class FounderNoteViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
) : ViewModel() {
    /** Mark the note seen so it never shows again, then proceed. */
    fun complete(onDone: () -> Unit) = viewModelScope.launch {
        prefs.setFounderNoteSeen()
        onDone()
    }
}

@Composable
fun FounderNoteScreen(
    onProceed : () -> Unit,   // "Let's get started" — would open paywall (#5); for now → app
    onSkip    : () -> Unit,   // "maybe later" — straight to app
    vm        : FounderNoteViewModel = hiltViewModel(),
) {
    // Staggered entrance: icon → body → sign-off → buttons
    var stage by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        stage = 1; kotlinx.coroutines.delay(200)
        stage = 2; kotlinx.coroutines.delay(300)
        stage = 3; kotlinx.coroutines.delay(200)
        stage = 4
    }
    val iconA   by animateFloatAsState(if (stage >= 1) 1f else 0f, tween(400), label = "icon")
    val bodyA   by animateFloatAsState(if (stage >= 2) 1f else 0f, tween(500), label = "body")
    val signA   by animateFloatAsState(if (stage >= 3) 1f else 0f, tween(400), label = "sign")
    val btnA    by animateFloatAsState(if (stage >= 4) 1f else 0f, tween(300), label = "btn")

    Column(modifier = Modifier.fillMaxSize().background(NoteBg)) {
        // ── Scrollable note ─────────────────────────────────────────────
        Column(
            modifier            = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            // App icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .alpha(iconA)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFECE7DD)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_launcher_fg),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(32.dp))

            // Body — handwritten
            Text(
                BODY,
                fontFamily  = Oregano,
                fontSize    = 19.sp,
                lineHeight  = 27.sp,
                color       = NoteBody,
                textAlign   = TextAlign.Center,
                modifier    = Modifier.widthIn(max = 340.dp).alpha(bodyA),
            )

            Spacer(Modifier.height(20.dp))

            // Sign-off — handwritten italic
            Text(
                "Jam x Cyn",
                fontFamily = Oregano,
                fontStyle  = FontStyle.Italic,
                fontSize   = 26.sp,
                color      = NoteSignoff,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.alpha(signA),
            )

            Spacer(Modifier.height(64.dp))
        }

        // ── Fixed bottom buttons ────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth().alpha(btnA).padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { vm.complete(onProceed) },
                shape    = RoundedCornerShape(16.dp),
                color    = NoteCta,
            ) {
                Text(
                    "Let's get started →",
                    color      = Color.White,
                    fontSize   = 16.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "maybe later",
                color    = NoteLater,
                fontSize = 14.sp,
                modifier = Modifier.clickable { vm.complete(onSkip) }.padding(8.dp),
            )
        }
    }
}
