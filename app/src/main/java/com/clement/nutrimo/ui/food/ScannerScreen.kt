// Barcode scanner: camera preview (CameraX) analysed on-device by ML Kit; returns the first EAN/UPC found.
package com.clement.nutrimo.ui.food

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun ScannerScreen(onBack: () -> Unit, onResult: (String) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    ScreenScaffold("Scanner un code-barres", onBack = onBack) { padding ->
        if (granted) CameraPreview(onResult, Modifier.fillMaxSize().padding(padding))
        else EmptyState("L'accès à la caméra est nécessaire pour scanner.", Modifier.padding(padding))
    }
}

@Composable
private fun CameraPreview(onResult: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val done = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build(),
        )
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown(); scanner.close() } }
    AndroidView(modifier = modifier, factory = { ctx ->
        PreviewView(ctx).also { view ->
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor) { image -> analyze(image, scanner, done, onResult, ctx) } }
                providerFuture.get().apply {
                    unbindAll()
                    bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    })
}

/** Reports the first barcode once, on the main thread; [done] ignores frames analysed afterwards. */
@OptIn(ExperimentalGetImage::class)
private fun analyze(
    image: ImageProxy,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    done: AtomicBoolean,
    onResult: (String) -> Unit,
    context: android.content.Context,
) {
    val media = image.image
    if (media == null || done.get()) return image.close()
    scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
        .addOnSuccessListener { codes ->
            val value = codes.firstNotNullOfOrNull { it.rawValue }
            if (value != null && done.compareAndSet(false, true)) {
                ContextCompat.getMainExecutor(context).execute { onResult(value) }
            }
        }
        .addOnCompleteListener { image.close() }
}
