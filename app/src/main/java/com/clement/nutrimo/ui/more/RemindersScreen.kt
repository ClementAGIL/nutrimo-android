// Reminder settings: weekly weigh-in (day + time) and daily meal logging (time). Asks for the notification permission.
package com.clement.nutrimo.ui.more

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.reminders.Reminders
import com.clement.nutrimo.data.repository.ReminderSettings
import com.clement.nutrimo.ui.common.Dropdown
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appContainer
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsRepo = appContainer().settings
    val settings by settingsRepo.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    // The notification permission only exists from Android 13; before that notifications are allowed by default.
    val askPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val s = settings ?: return
    ScreenScaffold("Rappels", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ReminderCard("Pesée hebdomadaire", "Seulement si aucune pesée ce jour-là.", s.weighReminder, withDay = true) { r ->
                if (r.enabled) askPermission()
                scope.launch { settingsRepo.setWeighReminder(r); Reminders.scheduleWeigh(context, r) }
            }
            ReminderCard("Saisie des repas", "Chaque jour, si le journal est vide.", s.mealReminder, withDay = false) { r ->
                if (r.enabled) askPermission()
                scope.launch { settingsRepo.setMealReminder(r); Reminders.scheduleMeal(context, r) }
            }
        }
    }
}

@Composable
private fun ReminderCard(title: String, hint: String, r: ReminderSettings, withDay: Boolean, onChange: (ReminderSettings) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title)
                Text(hint)
            }
            Switch(checked = r.enabled, onCheckedChange = { onChange(r.copy(enabled = it)) })
        }
        if (!r.enabled) return@SectionCard
        if (withDay) {
            Dropdown("Jour", r.day, DayOfWeek.entries, { it.getDisplayName(TextStyle.FULL, Locale.FRANCE) }, { onChange(r.copy(day = it)) },
                Modifier.fillMaxWidth())
        }
        TextButton(onClick = { picking = true }) { Text("Heure : %02d:%02d".format(r.hour, r.minute)) }
    }
    if (picking) TimeDialog(r.hour, r.minute, { h, m -> onChange(r.copy(hour = h, minute = m)) }, { picking = false })
}

@Composable
private fun TimeDialog(hour: Int, minute: Int, onPick: (Int, Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(hour, minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state) },
        confirmButton = { TextButton(onClick = { onPick(state.hour, state.minute); onDismiss() }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}
