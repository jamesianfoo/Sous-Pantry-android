package com.souspantry.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.R
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onSignedIn : () -> Unit,
    vm         : AuthViewModel = hiltViewModel(),
) {
    val state   by vm.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Navy, Color(0xFF1A3829)),   // navy → deep green (iOS gradient)
                ),
            ),
    ) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // ── Logo + wordmark ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFECE7DD)),   // icon cream
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_launcher_fg),
                    contentDescription = null,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Sous Pantry", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.weight(1f))

            // ── Sign-in card ────────────────────────────────────────────
            Text(
                "All-in-one pantry\nand recipe assistant",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                lineHeight = 28.sp,
            )
            Spacer(Modifier.height(18.dp))

            AuthButton(
                background = Color.White,
                loading    = state.loadingGoogle,
                enabled    = !state.loadingGoogle,
                onClick    = { vm.continueWithGoogle(context, onSignedIn) },
            ) {
                Image(
                    painter            = painterResource(R.drawable.ic_google_g),
                    contentDescription = null,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (state.loadingGoogle) "Signing in…" else "Continue with Google",
                    color      = Color.Black.copy(alpha = 0.75f),
                    fontWeight = FontWeight.SemiBold,
                )
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Color(0xFFFF8A80), fontSize = 12.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.weight(1f))

            // ── Privacy footer ──────────────────────────────────────────
            Text(
                "By continuing, you agree that your data is stored securely on your device. We never sell your personal information.",
                color      = Color.White.copy(alpha = 0.35f),
                fontSize   = 11.sp,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.padding(bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun AuthButton(
    background : Color,
    loading    : Boolean = false,
    enabled    : Boolean = true,
    onClick    : () -> Unit,
    content    : @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier        = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled && !loading, onClick = onClick),
        shape           = RoundedCornerShape(16.dp),
        color           = background,
        shadowElevation = if (background == Color.White) 6.dp else 0.dp,
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color       = if (background == Color.White) Slate else Color.White,
                    strokeWidth = 2.dp,
                    modifier    = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)w
