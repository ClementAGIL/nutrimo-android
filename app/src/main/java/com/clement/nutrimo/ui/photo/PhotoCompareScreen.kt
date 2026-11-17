// Comparison of photos A and B: side by side, or B over A with an opacity slider; each photo zooms and moves alone.
package com.clement.nutrimo.ui.photo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.kg
import com.clement.nutrimo.ui.common.short

private const val DEFAULT_OPACITY = 0.5f

@Composable
fun PhotoCompareScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, handle -> PhotoCompareViewModel(c, handle) }
    val state by vm.state.collectAsStateWithLifecycle()
    val zoomA = remember { ZoomState() }
    val zoomB = remember { ZoomState() }
    var overlay by rememberSaveable { mutableStateOf(false) }
    SecureWindow()
    ScreenScaffold(
        "Comparer",
        onBack = onBack,
        actions = { IconButton(onClick = { zoomA.reset(); zoomB.reset() }) { Icon(Icons.Default.RestartAlt, "Recadrer") } },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            PhotoLockGate {
                val a = state.a
                val b = state.b
                if (a == null || b == null) return@PhotoLockGate
                ModeSwitch(overlay) { overlay = it }
                if (overlay) OverlayView(a, b, zoomA, zoomB) else SideBySide(a, b, zoomA, zoomB)
            }
        }
    }
}

@Composable
private fun ModeSwitch(overlay: Boolean, onChange: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        listOf(false to "Côte à côte", true to "Superposer").forEachIndexed { i, (value, label) ->
            SegmentedButton(selected = overlay == value, onClick = { onChange(value) }, shape = SegmentedButtonDefaults.itemShape(i, 2)) { Text(label) }
        }
    }
}

@Composable
private fun SideBySide(a: CompareSide, b: CompareSide, zoomA: ZoomState, zoomB: ZoomState) {
    Row(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(Triple("A", a, zoomA), Triple("B", b, zoomB)).forEach { (letter, side, zoom) ->
            val image by rememberPhoto(side.photo, COMPARE_SIDE)
            Box(Modifier.weight(1f).fillMaxSize().clipToBounds().zoomGestures { zoom }) {
                ZoomedPhoto(image, zoom)
                SideLabel(letter, side, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

/** B over A; the chips choose which photo the fingers move. */
@Composable
private fun OverlayView(a: CompareSide, b: CompareSide, zoomA: ZoomState, zoomB: ZoomState) {
    var opacity by rememberSaveable { mutableFloatStateOf(DEFAULT_OPACITY) }
    var movingB by rememberSaveable { mutableStateOf(true) }
    val imageA by rememberPhoto(a.photo, COMPARE_SIDE)
    val imageB by rememberPhoto(b.photo, COMPARE_SIDE)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().zoomGestures { if (movingB) zoomB else zoomA }) {
            ZoomedPhoto(imageA, zoomA)
            ZoomedPhoto(imageB, zoomB, alpha = opacity)
            Column(Modifier.align(Alignment.BottomCenter), horizontalAlignment = Alignment.CenterHorizontally) {
                SideLabel("A", a, Modifier)
                SideLabel("B", b, Modifier)
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Opacité de B : ${(opacity * 100).toInt()} %", style = MaterialTheme.typography.labelLarge)
            Slider(opacity, { opacity = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Déplacer :")
                FilterChip(selected = !movingB, onClick = { movingB = false }, label = { Text("Photo A") })
                FilterChip(selected = movingB, onClick = { movingB = true }, label = { Text("Photo B") })
            }
        }
    }
}

/** "A · 14/06 · Face · 110,2 kg" over the photo. */
@Composable
private fun SideLabel(letter: String, side: CompareSide, modifier: Modifier) {
    val weight = side.kg?.let { " · ${kg(it)}" }.orEmpty()
    Text(
        "$letter · ${side.photo.date.short()} · ${side.photo.pose.label}$weight",
        modifier.padding(6.dp).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 3.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
    )
}
