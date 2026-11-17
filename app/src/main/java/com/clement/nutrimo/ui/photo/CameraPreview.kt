// CameraX for progress photos: 4:3 preview + still capture, front or back camera, delivered as an upright Bitmap.
package com.clement.nutrimo.ui.photo

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.clement.nutrimo.R
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.uiText

/** Preview and photos share the 4:3 sensor ratio, so the ghost of a past photo (cropped the same way) lines up. */
private val RATIO_4_3 = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()

/** Holds the capture use case so the screen can take a picture. */
class PhotoCapture {
    val useCase: ImageCapture = ImageCapture.Builder()
        .setResolutionSelector(RATIO_4_3)
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
        .build()

    /**
     * The in-memory image is the sensor's view, upright once rotated: never mirrored, even from the front camera
     * (only the preview is shown as a mirror), so front and back photos compare the same way.
     */
    fun take(context: android.content.Context, onPhoto: (Bitmap) -> Unit, onError: (UiText) -> Unit) {
        useCase.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.use { it.toBitmap().rotated(it.imageInfo.rotationDegrees) }
                onPhoto(bitmap)
            }

            override fun onError(exception: ImageCaptureException) =
                onError(exception.message?.let { UiText.Raw(it) } ?: uiText(R.string.camera_error))
        })
    }
}

@Composable
fun CameraPreview(capture: PhotoCapture, front: Boolean, modifier: Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    // Rebinds when the camera is switched.
    LaunchedEffect(front) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val preview = Preview.Builder().setResolutionSelector(RATIO_4_3).build().also { it.surfaceProvider = view.surfaceProvider }
            val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            providerFuture.get().apply {
                unbindAll()
                bindToLifecycle(lifecycleOwner, selector, preview, capture.useCase)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    AndroidView(factory = { view }, modifier = modifier)
}

private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees == 0) return this
    return Bitmap.createBitmap(this, 0, 0, width, height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
}
