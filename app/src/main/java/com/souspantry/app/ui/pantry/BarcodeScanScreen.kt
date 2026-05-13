package com.souspantry.app.ui.pantry

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.souspantry.app.ui.camera.CameraPreview
import com.souspantry.app.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun BarcodeScanScreen(
    onDismiss : () -> Unit,
    onSaved   : () -> Unit,
    vm        : BarcodeScanViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state) { if (state is BarcodeScanState.Saved) onSaved() }

    val analysisUseCase = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build().also { analysis ->
                val scanner  = BarcodeScanning.getClient()
                val executor = Executors.newSingleThreadExecutor()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    @androidx.camera.core.ExperimentalGetImage
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val img = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(img)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull { it.rawValue != null }?.rawValue
                                    ?.let { vm.onBarcodeDetected(it) }
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else proxy.close()
                }
            }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        CameraPreview(modifier = Modifier.fillMaxSize(), onCameraReady = {}, analysisUseCase = analysisUseCase)

        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            Icon(Icons.Filled.Close, "Close", tint = Color.White)
        }

        if (state is BarcodeScanState.Scanning) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
                shape = RoundedCornerShape(12.dp), color = Color.Black.copy(0.7f)) {
                Text("Point at a barcode", style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.padding(16.dp))
            }
        }

        if (state is BarcodeScanState.Loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
        }

        if (state is BarcodeScanState.Result) {
            val item = (state as BarcodeScanState.Result).item
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = Cream) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Found: ${item.name}", style = MaterialTheme.typography.headlineMedium)
                    item.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    item.category?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { vm.rescan() }, Modifier.weight(1f)) { Text("Rescan") }
                        Button(onClick = { vm.saveItem(item) }, Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Add to Pantry") }
                    }
                }
            }
        }

        if (state is BarcodeScanState.Error) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = Cream) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text((state as BarcodeScanState.Error).message, style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = { vm.rescan() }, Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Try Again") }
                }
            }
        }
    }
}
