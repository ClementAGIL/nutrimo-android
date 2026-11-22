// Bubble over the hub chart for the tapped day: date, weight, measurements, photos, and links to their screens.
package com.clement.nutrimo.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.util.day
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.kg
import kotlin.math.roundToInt

private val BUBBLE_MAX_WIDTH = 240.dp

/** Centered over [x] (px) at the top of the chart, kept inside it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DayBubble(day: HubDay, x: Float, actions: WeightHubActions, onClose: () -> Unit) {
    Card(
        Modifier.centeredOver(x).widthIn(max = BUBBLE_MAX_WIDTH),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(Modifier.padding(start = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day.date.day(), Modifier.weight(1f), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, stringResource(R.string.common_fermer)) }
            }
            day.kg?.let { Text(if (day.weighed) stringResource(R.string.day_bubble_pesee_1, kg(it)) else stringResource(R.string.day_bubble_1_courbe, kg(it)), style = MaterialTheme.typography.bodySmall) }
            day.measurement?.let { Text(measurementSummary(it), style = MaterialTheme.typography.bodySmall) }
            if (day.photos.isNotEmpty()) Text(stringResource(R.string.day_bubble_photos_1, day.photos.map { stringResource(it.pose.label) }.joinToString()), style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (day.weighed) TextButton(onClick = actions.onHistory) { Text(stringResource(R.string.common_historique)) }
                if (day.measurement != null) TextButton(onClick = actions.onMeasurements) { Text(stringResource(R.string.common_mensurations)) }
                if (day.photos.isNotEmpty()) TextButton(onClick = actions.onPhotos) { Text(stringResource(R.string.common_photos)) }
            }
        }
    }
}

/** Centers the element over [x] (px), measured against the whole parent width then clamped to stay inside it. */
internal fun Modifier.centeredOver(x: Float) = layout { measurable, constraints ->
    val p = measurable.measure(constraints.copy(minWidth = 0))
    val left = (x - p.width / 2f).roundToInt().coerceIn(0, (constraints.maxWidth - p.width).coerceAtLeast(0))
    layout(constraints.maxWidth, p.height) { p.place(left, 0) }
}

/** "Taille 98 cm · Hanches 104 cm" (the first two measured fields). */
@Composable
private fun measurementSummary(m: com.clement.nutrimo.data.db.MeasurementEntity): String =
    MEASUREMENT_FIELDS.mapNotNull { f -> f.get(m)?.let { stringResource(R.string.measure_value, stringResource(f.label), fmt(it)) } }.take(2).joinToString(" · ")
