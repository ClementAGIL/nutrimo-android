// Photo camera: live preview with the ghost of the last photo of the pose, pose chips, camera switch, self-timer.
package com.clement.nutrimo.ui.photo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.domain.model.PhotoPose
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val GHOST_ALPHA = 0.3f
private const val CAMERA_RATIO = 3f / 4f

@Composable
fun PhotoCameraScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, handle -> PhotoCameraViewModel(c, handle) }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    LaunchedEffect(Unit) { vm.messages.collect { snackbar.showSnackbar(it) } }
    SecureWindow()
    ScreenScaffold(stringResource(R.string.photo_camera_photo_progression), onBack = onBack, snackbar = snackbar) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            PhotoLockGate {
                if (state.shot == null) CameraWithPermission(vm, state) else PhotoReview(vm, state)
            }
        }
    }
}

@Composable
private fun CameraWithPermission(vm: PhotoCameraViewModel, state: PhotoCameraState) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (granted) Aiming(vm, state) else EmptyState(stringResource(R.string.photo_camera_acces_camera_est_necessaire_pour))
}

@Composable
private fun Aiming(vm: PhotoCameraViewModel, state: PhotoCameraState) {
    val context = LocalContext.current
    val capture = remember { PhotoCapture() }
    val scope = rememberCoroutineScope()
    var countdown by remember { mutableStateOf(0) }
    val shoot = {
        scope.launch {
            for (s in state.timer downTo 1) {
                countdown = s
                delay(1000)
            }
            countdown = 0
            capture.take(context, vm::onShot) { vm.messages.tryEmit(it) }
        }
    }
    Column(Modifier.fillMaxSize()) {
        CameraControls(state, vm)
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            Box(Modifier.aspectRatio(CAMERA_RATIO).clip(MaterialTheme.shapes.large)) {
                CameraPreview(capture, state.front, Modifier.fillMaxSize())
                if (state.ghostOn) Ghost(state)
                if (countdown > 0) Countdown(countdown)
            }
        }
        PoseChips(state.pose, vm::setPose)
        ShutterButton(enabled = countdown == 0) { shoot() }
    }
}

/** The last photo of the pose over the preview; mirrored with the front camera, like the preview itself. */
@Composable
private fun Ghost(state: PhotoCameraState) {
    val ghost = state.ghost ?: return
    Image(
        ghost.asImageBitmap(), null,
        Modifier.fillMaxSize().graphicsLayer { alpha = GHOST_ALPHA; scaleX = if (state.front) -1f else 1f },
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun Countdown(seconds: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$seconds", color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CameraControls(state: PhotoCameraState, vm: PhotoCameraViewModel) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = vm::switchCamera) { Icon(Icons.Default.Cameraswitch, stringResource(R.string.photo_camera_changer_camera)) }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = vm::nextTimer).padding(8.dp)) {
            Icon(if (state.timer == 0) Icons.Default.TimerOff else Icons.Default.Timer, "Retardateur")
            Text(if (state.timer == 0) stringResource(R.string.photo_camera_sans_retardateur) else " ${state.timer} s")
        }
        IconButton(onClick = vm::toggleGhost, enabled = state.ghost != null) {
            Icon(if (state.ghostOn) Icons.Default.Layers else Icons.Default.LayersClear, stringResource(R.string.photo_camera_calque_derniere_photo))
        }
    }
}

@Composable
private fun PoseChips(pose: PhotoPose, onSelect: (PhotoPose) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        PhotoPose.entries.forEach { p -> FilterChip(selected = p == pose, onClick = { onSelect(p) }, label = { Text(p.label) }) }
    }
}

@Composable
private fun ShutterButton(enabled: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(72.dp).clip(CircleShape).border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                .padding(6.dp).clip(CircleShape)
                .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                .clickable(enabled = enabled, onClick = onClick),
        )
    }
}
