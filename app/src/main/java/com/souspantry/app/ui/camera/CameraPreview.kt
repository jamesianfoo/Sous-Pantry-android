package com.souspantry.app.ui.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
fun CameraPreview(
    modifier        : Modifier = Modifier,
    onCameraReady   : (ImageCapture) -> Unit,
    analysisUseCase : ImageAnalysis? = null,
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView    = remember { PreviewView(context) }
    val imageCapture   = remember { ImageCapture.Builder().build() }

    LaunchedEffect(analysisUseCase) {
        val cameraProvider = context.getCameraProvider()
        val preview = androidx.camera.core.Preview.Builder().build()
            .also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val useCases = buildList {
            add(preview); add(imageCapture)
            analysisUseCase?.let { add(it) }
        }
        runCatching {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, *useCases.toTypedArray())
            onCameraReady(imageCapture)
        }.onFailure { Log.e("CameraPreview", "bind failed", it) }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { cont ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ cont.resume(future.get(), null) }, ContextCompat.getMainExecutor(this))
    }
