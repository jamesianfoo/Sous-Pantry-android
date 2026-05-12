package com.souspantry.app.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val state    by vm.state.collectAsState()
    var nameInput by remember(state.userName) { mutableStateOf(state.userName) }

    Scaffold(
        containerColor = Cream,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Cream),
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // Name card
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Your Name", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nameInput, onValueChange = { nameInput = it },
                        placeholder = { Text("Enter your name") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                    )
                    Button(
                        onClick  = { vm.setUserName(nameInput.trim()) },
                        colors   = ButtonDefaults.buttonColors(containerColor = Green),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save Name") }
                }
            }

            // Notifications
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Push Notifications", style = MaterialTheme.typography.titleMedium)
                        Text("Recipe ideas and pantry reminders", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = state.notifEnabled, onCheckedChange = { vm.setNotifEnabled(it) },
                        colors  = SwitchDefaults.colors(checkedThumbColor = Green, checkedTrackColor = SoftMint),
                    )
                }
            }

            // App info
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Text("Sous Pantry v1.0", style = MaterialTheme.typography.bodyMedium)
                    Text("Your AI-powered kitchen companion", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
