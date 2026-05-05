package com.souspantry.app.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.data.models.ShoppingItem
import com.souspantry.app.ui.theme.*

@Composable
fun ShoppingScreen(vm: ShoppingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()

    Scaffold(containerColor = Cream) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Shopping List", style = MaterialTheme.typography.headlineLarge)
                    Text("${state.items.count { !it.checked }} items remaining", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Empty / generate state
            if (state.items.isEmpty() && !state.loading) {
                item { GeneratePrompt(pantryEmpty = state.pantryEmpty, onGenerate = { vm.generate() }) }
            }

            if (state.loading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Green)
                            Spacer(Modifier.height(12.dp))
                            Text("Sous AI is building your list…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            state.error?.let { msg ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors   = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
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

            // Sections: Essential → Nice to Have → In Basket
            listOf("Essential", "Nice to Have").forEach { section ->
                val sectionItems = state.items.filter { it.priority == section && !it.checked }
                if (sectionItems.isNotEmpty()) {
                    item {
                        Text(
                            section.uppercase(),
                            style    = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                            color    = Slate,
                        )
                    }
                    items(sectionItems, key = { it.id }) { item ->
                        ShoppingRow(item, onToggle = { vm.toggle(item) })
                    }
                }
            }

            val inBasket = state.items.filter { it.checked }
            if (inBasket.isNotEmpty()) {
                item {
                    Text("IN BASKET", style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp), color = Slate)
                }
                items(inBasket, key = { it.id }) { item ->
                    ShoppingRow(item, onToggle = { vm.toggle(item) }, dimmed = true)
                }
            }

            // Refresh button at bottom if list exists
            if (state.items.isNotEmpty() && !state.loading) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        OutlinedButton(
                            onClick  = { vm.generate() },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Let's Restock")
                        }
                    }
                }
            }
        }
    }
}

// ── Generate prompt ───────────────────────────────────────────────────────────

@Composable
private fun GeneratePrompt(pantryEmpty: Boolean, onGenerate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier            = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (pantryEmpty) {
                Text("Your pantry is empty —\nlet's fix that!", style = MaterialTheme.typography.headlineMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Start with the essentials every kitchen needs.", style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = onGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Navy),
                ) {
                    Text("Stock Up the Essentials")
                }
            } else {
                Text("Your list is clear!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text("Ready to plan your next shop?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick  = onGenerate,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = Green),
                ) {
                    Text("Let's Restock")
                }
            }
        }
    }
}

// ── Shopping row ──────────────────────────────────────────────────────────────

@Composable
private fun ShoppingRow(item: ShoppingItem, onToggle: () -> Unit, dimmed: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = if (dimmed) Color(0xFFF0F0F0) else Color.White),
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector  = if (item.checked) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle",
                    tint         = if (item.checked) Green else Slate,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium.copy(
                    color = if (dimmed) Slate else Navy))
                item.reason?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            item.quantity?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
