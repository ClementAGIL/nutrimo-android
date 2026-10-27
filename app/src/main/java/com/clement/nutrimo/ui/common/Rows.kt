// Content rows used inside cards: label / value, progress bars, on-off switch, goal settings button.
package com.clement.nutrimo.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val STAT_VALUE_MAX_WIDTH = 220.dp

@Composable
fun StatRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            // Bounded so a long value wraps instead of squeezing its label to nothing.
            modifier = Modifier.widthIn(max = STAT_VALUE_MAX_WIDTH),
        )
    }
}

/** Label, value text and a bar; [progress] is clamped to 0..1. */
@Composable
fun ProgressRow(label: String, value: String, progress: Double, color: Color = MaterialTheme.colorScheme.primary) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        StatRow(label, value)
        ProgressBar(progress, color, Modifier.fillMaxWidth())
    }
}

/** Rounded bar; [progress] is clamped to 0..1. */
@Composable
fun ProgressBar(progress: Double, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.toFloat().coerceIn(0f, 1f) },
        modifier = modifier.height(10.dp),
        color = color,
        strokeCap = StrokeCap.Round,
        drawStopIndicator = {},
    )
}

/** [label] (and optional [hint] under it) with a switch on the right. */
@Composable
fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label)
            hint?.let { HintText(it) }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Opens the settings screen where the card's goal is set. */
@Composable
fun GoalSettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Default.Tune, "Modifier l'objectif") }
}
