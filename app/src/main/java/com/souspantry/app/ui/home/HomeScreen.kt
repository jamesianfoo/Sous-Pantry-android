package com.souspantry.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.data.models.TrendingRecipe
import com.souspantry.app.data.models.AdventurousRecipe
import com.souspantry.app.ui.theme.*

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit = {},
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()

    LazyColumn(
        modifier            = Modifier.fillMaxSize().background(Cream),
        contentPadding      = PaddingValues(bottom = 24.dp),
    ) {
        item {
            HomeHeader(onNavigateToSettings)
        }

        // ── Suggested recipes ─────────────────────────────────────────────────
        item { SectionTitle("Suggested for You") }
        item {
            when {
                state.suggestedLoading -> SectionLoading()
                state.suggestedError   -> SectionError { vm.loadSuggested() }
                else -> HorizontalRecipeRow(state.suggestedRecipes)
            }
        }

        // ── Trending social ───────────────────────────────────────────────────
        item { SectionTitle("Trending on #Social") }
        item {
            when {
                state.trendingLoading -> SectionLoading()
                state.trendingError   -> SectionError { vm.loadTrending() }
                else -> HorizontalTrendingRow(state.trendingRecipes)
            }
        }

        // ── Pantry-based ──────────────────────────────────────────────────────
        item { SectionTitle("Based on your pantry") }
        item {
            when {
                state.pantryLoading -> SectionLoading()
                state.pantryError   -> SectionError { vm.loadPantryMeals() }
                else -> HorizontalRecipeRow(state.pantryRecipes)
            }
        }

        // ── Adventurous ───────────────────────────────────────────────────────
        item { SectionTitle("Try something adventurous") }
        item {
            when {
                state.adventurousLoading -> SectionLoading()
                state.adventurousError   -> SectionError { vm.loadAdventurous() }
                else -> HorizontalAdventureRow(state.adventurousRecipes)
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(onSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Navy, Green)))
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column {
            Text("Sous Pantry", style = MaterialTheme.typography.headlineLarge.copy(color = White))
            Text("What are we cooking today?", style = MaterialTheme.typography.bodyMedium.copy(color = White.copy(alpha = 0.75f)))
        }
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd)) {
            Icon(Icons.Filled.Settings, "Settings", tint = White)
        }
    }
}

// ── Section helpers ───────────────────────────────────────────────────────────

@Composable
private fun SectionTitle(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SectionLoading() {
    Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Green)
    }
}

@Composable
private fun SectionError(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier            = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Couldn't load recipes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("Check your connection and try again.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Text("Retry")
            }
        }
    }
}

// ── Recipe card ───────────────────────────────────────────────────────────────

@Composable
private fun RecipeCard(title: String, description: String, imageQuery: String, badge: String? = null) {
    val imageUrl = "https://loremflickr.com/400/300/${imageQuery.replace(" ", ",")},food"

    Card(
        modifier = Modifier.width(200.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                AsyncImage(
                    model             = imageUrl,
                    contentDescription = title,
                    contentScale      = ContentScale.Crop,
                    modifier          = Modifier.fillMaxSize(),
                )
                // Dark scrim
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)))
                ))
                badge?.let {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        shape    = RoundedCornerShape(50),
                        color    = Green,
                    ) {
                        Text(it, style = MaterialTheme.typography.labelSmall.copy(color = White),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}

@Composable
private fun HorizontalRecipeRow(meals: List<SuggestedMeal>) {
    LazyRow(
        contentPadding    = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(meals) { meal ->
            RecipeCard(meal.title, meal.description, meal.imageQuery)
        }
    }
}

@Composable
private fun HorizontalTrendingRow(recipes: List<TrendingRecipe>) {
    LazyRow(
        contentPadding    = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe.title, recipe.description, recipe.imageQuery, badge = recipe.platform)
        }
    }
}

@Composable
private fun HorizontalAdventureRow(recipes: List<AdventurousRecipe>) {
    LazyRow(
        contentPadding    = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(recipes) { recipe ->
            RecipeCard(recipe.title, recipe.description, recipe.imageQuery, badge = "${recipe.matchPercent}% match")
        }
    }
}
