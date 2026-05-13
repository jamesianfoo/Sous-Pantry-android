package com.souspantry.app.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

private data class OnboardingPage(val emoji: String, val title: String, val description: String)

private val pages = listOf(
    OnboardingPage("🥘", "Your AI Kitchen",    "Sous Pantry uses AI to suggest meals from what you already have."),
    OnboardingPage("📷", "Scan & Track",       "Scan barcodes or receipts to instantly add items to your pantry."),
    OnboardingPage("🛒", "Smart Shopping",     "Get a personalised shopping list to fill the gaps in your pantry."),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete : () -> Unit,
    vm         : OnboardingViewModel = hiltViewModel(),
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Green)))) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val p = pages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(p.emoji, fontSize = 72.sp)
                Spacer(Modifier.height(32.dp))
                Text(p.title, style = MaterialTheme.typography.headlineMedium.copy(color = White), textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(p.description, style = MaterialTheme.typography.bodyLarge.copy(color = White.copy(0.8f)), textAlign = TextAlign.Center)
            }
        }

        // Dots
        Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(pages.size) { i ->
                Box(Modifier.size(if (i == pagerState.currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (i == pagerState.currentPage) White else White.copy(0.4f)))
            }
        }

        Button(
            onClick  = {
                if (pagerState.currentPage == pages.size - 1) { vm.complete(); onComplete() }
                // Swipe to advance — button only triggers on last page
            },
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp).height(54.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = White),
        ) {
            Text(if (pagerState.currentPage < pages.size - 1) "Swipe to continue" else "Get Started",
                style = MaterialTheme.typography.titleMedium.copy(color = Navy))
        }
    }
}
