// Pinch-zoom and drag for one photo of the comparison; each photo keeps its own position.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale

private const val MIN_ZOOM = 0.5f
private const val MAX_ZOOM = 6f

/** Zoom and offset of one photo; photos not framed the same way can be lined up by hand. */
@Stable
class ZoomState {
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    fun apply(pan: Offset, zoom: Float) {
        scale = (scale * zoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
        offset += pan
    }
}

/** Gestures on this area move [state] (lets the overlay route them to photo A or B). */
fun Modifier.zoomGestures(state: () -> ZoomState): Modifier = pointerInput(Unit) {
    detectTransformGestures { _, pan, zoom, _ -> state().apply(pan, zoom) }
}

@Composable
fun ZoomedPhoto(image: ImageBitmap?, state: ZoomState, modifier: Modifier = Modifier, alpha: Float = 1f) {
    image ?: return
    Image(
        image, null,
        modifier.fillMaxSize().graphicsLayer {
            scaleX = state.scale
            scaleY = state.scale
            translationX = state.offset.x
            translationY = state.offset.y
            this.alpha = alpha
        },
        contentScale = ContentScale.Fit,
    )
}
