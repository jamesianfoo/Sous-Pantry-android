package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.souspantry.app.data.models.SuggestedMeal
import com.souspantry.app.ui.theme.*

@Composable
fun PlanCookScreen(vm: PlanCookViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = Cream) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Plan & Cook", style = MaterialTheme.typography.headlineLarge)
                    Text("Meals you can make today", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (state.meals.isEmpty() && !state.loading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White),
                        shape    = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier            = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Ready to cook?", style = MaterialTheme.typography.headlineMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Sous AI will suggest meals from your pantry.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick  = { vm.generate() },
                                modifier = Modifier.fillMaxWidth(),
                                colors   = ButtonDefaults.buttonColors(containerColor = Green),
                            ) { Text("Generate Meal Plan") }
                        }
                    }
                }
            }

            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Green)
                            Spacer(Modifier.height(12.dp))
                            Text("Sous AI is planning your meals…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            state.error?.let { msg ->
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(msg, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.generate() }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }

            items(state.meals) { meal ->
                MealCard(meal)
            }

            if (state.meals.isNotEmpty() && !state.loading) {
                item {
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OutlinedButton(onClick = { vm.generate() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Regenerate")
                        }
                    }
                }
            }
        }
    }
}

// ── Meal card ─────────────────────────────────────────────────────────────────

@Composable
private fun MealCard(meal: SuggestedMeal) {
    var expanded by remember { mutableStateOf(false) }
    val imageUrl = "https://loremflickr.com/400/300/${meal.imageQuery.replace(" ", ",")},food"

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(modifier = Modifier.height(130.dp).fillMaxWidth()) {
                AsyncImage(
                    model              = imageUrl,
                    contentDescription = meal.title,
                    contentScale       = ContentScale.Crop,
                    modifier           = Modifier.fillMaxSize(),
                )
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)))
                ))
                Row(
                    modifier            = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                ) {
                    DifficultyChip(meal.difficulty)
                    Text(meal.prepTime, style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(meal.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(meal.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)

                if (expanded) {
                    Spacer(Modifier.height(12.dp))
                    Text("Ingredients", style = MaterialTheme.typography.titleMedium)
                    meal.ingredients.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }

                    Spacer(Modifier.height(12.dp))
                    Text("Instructions", style = MaterialTheme.typography.titleMedium)
                    meal.instructions.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }

                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Show less" else "See recipe")
                }
            }
        }
    }
}

@Composable
private fun DifficultyChip(difficulty: String) {
    val color = when (difficulty) {
        "Easy"   -> Color(0xFF2D5A3D)
        "Medium" -> Color(0xFFC4965A)
        else     -> Color(0xFFD32F2F)
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(difficulty, style = MaterialTheme.typography.labelSmall.copy(color = Color.White),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
