package com.souspantry.app.ui.pantry

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.souspantry.app.ui.camera.CameraPreview
import com.souspantry.app.ui.theme.*
import java.io.File

@Composable
fun ReceiptScanScreen(
    onDismiss : () -> Unit,
    onSaved   : () -> Unit,
    vm        : ReceiptScanViewModel = hiltViewModel(),
) {
    val state   by vm.state.collectAsState()
    val context  = LocalContext.current
    var capture by remember { mutableStateOf<ImageCapture?>(null) }

    LaunchedEffect(state) { if (state is ReceiptScanState.Saved) onSaved() }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        if (state is ReceiptScanState.Ready) {
            CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = { capture = it })

            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Filled.Close, "Close", tint = Color.White)
            }

            FloatingActionButton(
                onClick = {
                    val file    = File(context.cacheDir, "receipt_${System.currentTimeMillis()}.jpg")
                    val options = ImageCapture.OutputFileOptions.Builder(file).build()
                    capture?.takePicture(options, ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(out: ImageCapture.OutputFileResults) {
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                                    ?.let { vm.processImage(it) }
                            }
                            override fun onError(e: ImageCaptureException) {
                                /* will surface as Error state on retry */
                            }
                        })
                },
                containerColor = Green,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            ) { Icon(Icons.Filled.CameraAlt, "Capture") }

            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(0.6f)) {
                Text("Point at receipt and tap capture",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                    modifier = Modifier.padding(12.dp))
            }
        }

        if (state is ReceiptScanState.Loading) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(0.8f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Green)
                    Spacer(Modifier.height(12.dp))
                    Text("Reading receipt…", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White))
                }
            }
        }

        if (state is ReceiptScanState.Results) {
            val items = (state as ReceiptScanState.Results).items
            Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
                Column(Modifier.padding(20.dp)) {
                    Text("Found ${items.size} items", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f)) {
                        items(items) { line ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(line.name, style = MaterialTheme.typography.titleMedium)
                                    line.category?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.retry() }, Modifier.weight(1f)) { Text("Retake") }
                        Button(onClick = { vm.saveAll(items) }, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add All") }
                    }
                }
            }
        }

        if (state is ReceiptScanState.Error) {
            Box(Modifier.fillMaxSize().background(Cream), contentAlignment = Alignment.Center) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text((state as ReceiptScanState.Error).message, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.retry() }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}
