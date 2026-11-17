// Reminder settings: one card per ReminderKind (switch, day for the weekly one, time). Asks for the notification
// permission, and says so when it is missing (some reminders are on by default).
package com.clement.nutrimo.ui.more

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.reminders.ReminderKind
import com.clement.nutrimo.data.repository.ReminderSettings
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.SwitchRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** Title and explanation of each reminder, in screen order. */
private val CARDS = listOf(
    ReminderKind.WEIGH to ("Pesée hebdomadaire" to "Seulement si aucune pesée ce jour-là."),
    ReminderKind.MEAL to ("Saisie des repas" to "Chaque jour, si le journal est vide."),
    ReminderKind.ENCOURAGEMENT to (
        "Encouragement du jour" to "Un message bienveillant chaque jour : vos victoires, la série à ne pas perdre, du réconfort les jours difficiles."
        ),
    ReminderKind.PHOTO to ("Photos de progression" to "Le 1er de chaque mois, si aucune photo ce mois-ci."),
)

class RemindersViewModel(private val c: AppContainer) : ViewModel() {
    val settings = c.settings.settings.stateWhileSubscribed(viewModelScope, null)

    fun canNotify(): Boolean = c.reminders.canNotify()

    /** In the ViewModel scope: leaving the screen right after a change no longer cancels saving it. */
    fun update(kind: ReminderKind, r: ReminderSettings) = viewModelScope.launch { c.reminders.update(kind, r) }
}

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> RemindersViewModel(c) }
    val settings by vm.settings.collectAsStateWithLifecycle()
    var allowed by remember { mutableStateOf(vm.canNotify()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { allowed = it }
    // The notification permission only exists from Android 13; before that notifications are allowed by default.
    val askPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !allowed) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val s = settings ?: return
    ScreenScaffold(stringResource(R.string.reminders_rappels), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            if (!allowed) {
                SectionCard(stringResource(R.string.reminders_notifications_desactivees)) {
                    WarningText(stringResource(R.string.reminders_rappels_encouragement_jour_ne_peuvent))
                    WideButton(stringResource(R.string.reminders_autoriser_notifications), askPermission)
                }
            }
            CARDS.forEach { (kind, text) ->
                ReminderCard(text.first, text.second, s.reminder(kind), withDay = kind.weekly) { r ->
                    if (r.enabled) askPermission()
                    vm.update(kind, r)
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(title: String, hint: String, r: ReminderSettings, withDay: Boolean, onChange: (ReminderSettings) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SectionCard {
        SwitchRow(title, r.enabled, { onChange(r.copy(enabled = it)) }, hint)
        if (!r.enabled) return@SectionCard
        if (withDay) {
            Dropdown(stringResource(R.string.reminders_jour), r.day, DayOfWeek.entries, { it.getDisplayName(TextStyle.FULL, Locale.FRANCE) }, { onChange(r.copy(day = it)) },
                Modifier.fillMaxWidth())
        }
        TextButton(onClick = { picking = true }) { Text(stringResource(R.string.reminders_heure_02d_02d).format(r.hour, r.minute)) }
    }
    if (picking) TimeDialog(r.hour, r.minute, { h, m -> onChange(r.copy(hour = h, minute = m)) }, { picking = false })
}

@Composable
private fun TimeDialog(hour: Int, minute: Int, onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(hour, minute, is24Hour = true)
    FormDialog(null, onDismiss, { onPick(state.hour, state.minute) }) { TimePicker(state) }
}
