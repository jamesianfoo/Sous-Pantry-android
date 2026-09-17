package com.souspantry.app.ui.plancook

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.souspantry.app.services.IngredientScaler
import com.souspantry.app.ui.theme.*

/**
 * Mirrors iOS `MissingIngredientsSheet`: lists what an external recipe still
 * needs and adds it to the shopping list only when the user confirms.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatToBuySheet(
    recipeTitle  : String,
    matchPercent : Int,
    missing      : List<String>,
    loading      : Boolean = false,
    onDismiss    : () -> Unit,
    onAdd        : (List<String>) -> Unit,
) {
    val items = remember(missing) {
        missing.map { IngredientScaler.cleanName(it) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }
    var added by remember(missing) { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text("What to buy", color = Navy, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(recipeTitle, color = Slate, fontSize = 14.sp, maxLines = 2)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Cancel, "Close", tint = Slate.copy(alpha = 0.4f))
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Icon(Icons.Filled.Error, null, tint = Gold, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (loading) "Checking the recipe page…" else "$matchPercent% pantry match · ${items.size} to buy",
                    color = Gold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                )
            }

            if (loading) {
                Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            } else Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(top = 16.dp, bottom = 12.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                items.forEach { name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Cream, RoundedCornerShape(10.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(Icons.Filled.ShoppingCart, null, tint = Green, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(name.replaceFirstChar { it.uppercase() }, color = Navy, fontSize = 15.sp)
                    }
                }
            }

            Button(
                onClick  = { onAdd(items); added = true },
                enabled  = !loading && !added && items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Green,
                    disabledContainerColor = Green.copy(alpha = 0.55f),
                ),
            ) {
                Icon(
                    if (added) Icons.Filled.CheckCircle else Icons.Filled.AddShoppingCart,
                    null, tint = Color.White, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (added) "Added to Shopping List" else "Add ${items.size} to Shopping List",
                    color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
