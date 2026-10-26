// Small reusable Compose building blocks shared by the screens.
package com.clement.nutrimo.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.clement.nutrimo.ui.theme.NutrimoColors
import androidx.compose.ui.unit.dp

/** Bottom space to keep the last items of a list reachable above a floating action button. */
val FAB_CLEARANCE = 88.dp
private val STAT_VALUE_MAX_WIDTH = 220.dp
/** Just enough shadow to lift white cards off the tinted page. */
private val CARD_ELEVATION = 1.dp

/** Scaffold with a top bar; [onBack] adds a back arrow. */
@Composable
fun ScreenScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbar: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Retour") }
                    }
                },
                actions = { actions() },
                // Same color as the page so the cards stand out, not the bar.
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = { SnackbarHost(snackbar) },
        content = content,
    )
}

/** Card with an optional title; [onClick] makes the whole card clickable (with the card-shaped ripple). */
@Composable
fun SectionCard(title: String? = null, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = CARD_ELEVATION)
    if (onClick != null) Card(onClick, Modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, content = inner)
    else Card(Modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, content = inner)
}

/** Sub-tabs on the page color (a white strip would look like a card). */
@Composable
fun AppTabs(titles: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    PrimaryTabRow(selectedTabIndex = selected, containerColor = MaterialTheme.colorScheme.background) {
        titles.forEachIndexed { i, title -> Tab(selected = selected == i, onClick = { onSelect(i) }, text = { Text(title) }) }
    }
}

/** Card title row: [title] and optional [subtitle] on the left, icon buttons ([actions]) on the right. */
@Composable
fun CardHeader(title: String, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        actions()
    }
}

/** Opens the settings screen where the card's goal is set. */
@Composable
fun GoalSettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Default.Tune, "Modifier l'objectif") }
}

/** Scrollable screen body: scaffold [padding] + 16 dp margins, children spaced by [spacing]. */
@Composable
fun ScrollColumn(padding: PaddingValues, spacing: Dp = 12.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}

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

/** Text field for decimal numbers; accepts both "," and ".". */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() || it == ',' || it == '.' }) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Confirmer",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
fun WarningText(text: String) {
    Text(text, color = NutrimoColors.warning, style = MaterialTheme.typography.bodySmall)
}
