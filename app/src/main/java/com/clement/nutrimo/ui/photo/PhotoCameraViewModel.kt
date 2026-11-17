// State of the photo camera: pose, camera, timer, ghost of the last photo, then the review of the shot before saving.
package com.clement.nutrimo.ui.photo

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.domain.model.PhotoPose
import com.clement.nutrimo.ui.navigation.PhotoCameraRoute
import com.clement.nutrimo.util.toDecimal
import com.clement.nutrimo.util.toInput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val GHOST_SIDE = 1200

/** Self-timer choices in seconds (0 = off). */
val TIMER_STEPS = listOf(0, 3, 10)

data class PhotoCameraState(
    val pose: PhotoPose = PhotoPose.FRONT,
    val front: Boolean = true,
    val timer: Int = 0,
    val ghostOn: Boolean = true,
    /** Last photo of [pose], upright and not mirrored. */
    val ghost: Bitmap? = null,
    /** The shot being reviewed; null while aiming. */
    val shot: Bitmap? = null,
    val note: String = "",
    /** Pre-filled with today's weigh-in, saved with the photo when changed or new. */
    val weight: String = "",
    val saving: Boolean = false,
)

class PhotoCameraViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val state = MutableStateFlow(PhotoCameraState())
    val messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    private val today = LocalDate.now()

    init {
        val pose = handle.toRoute<PhotoCameraRoute>().pose?.let(PhotoPose::valueOf) ?: PhotoPose.FRONT
        setPose(pose)
    }

    fun setPose(pose: PhotoPose) {
        state.update { it.copy(pose = pose, ghost = null) }
        viewModelScope.launch {
            val last = c.photos.latest(pose)
            val ghost = last?.let { c.photos.load(c.photos.file(it), GHOST_SIDE) }
            state.update { if (it.pose == pose) it.copy(ghost = ghost) else it }
        }
    }

    fun switchCamera() = state.update { it.copy(front = !it.front) }

    fun nextTimer() = state.update { it.copy(timer = TIMER_STEPS[(TIMER_STEPS.indexOf(it.timer) + 1) % TIMER_STEPS.size]) }

    fun toggleGhost() = state.update { it.copy(ghostOn = !it.ghostOn) }

    fun onShot(bitmap: Bitmap) {
        state.update { it.copy(shot = bitmap) }
        viewModelScope.launch {
            val kg = c.body.weightOn(today)?.kg
            state.update { it.copy(weight = kg?.toInput().orEmpty()) }
        }
    }

    fun retake() = state.update { it.copy(shot = null) }

    fun setNote(note: String) = state.update { it.copy(note = note) }

    fun setWeight(weight: String) = state.update { it.copy(weight = weight) }

    /** Saves the shot (and the weigh-in if one was typed), then aims at the next pose for a full session. */
    fun save() {
        val s = state.value
        val shot = s.shot ?: return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            c.photos.save(today, s.pose, s.front, s.note, shot)
            s.weight.toDecimal()?.let { kg -> if (c.body.weightOn(today)?.kg != kg) c.body.saveWeight(today, kg) }
            messages.tryEmit("Photo « ${s.pose.label} » enregistrée")
            val next = PhotoPose.entries[(s.pose.ordinal + 1) % PhotoPose.entries.size]
            state.update { it.copy(shot = null, note = "", saving = false) }
            setPose(next)
        }
    }
}
