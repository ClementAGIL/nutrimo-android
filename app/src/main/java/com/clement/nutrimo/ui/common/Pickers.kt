// Date field and enum dropdown built on Material 3 pickers.
package com.clement.nutrimo.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Read-only field showing [date]; the calendar button opens a picker limited by [min] / [max]. */
@Composable
fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    min: LocalDate? = null,
    max: LocalDate? = null,
) {
    var open by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = date.full(),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = { IconButton(onClick = { open = true }) { Icon(Icons.Default.CalendarMonth, "Choisir") } },
        modifier = modifier,
    )
    if (!open) return
    val state = rememberDatePickerState(
        initialSelectedDateMillis = date.toMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val d = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                return (min == null || !d.isBefore(min)) && (max == null || !d.isAfter(max))
            }
        },
    )
    DatePickerDialog(
        onDismissRequest = { open = false },
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                open = false
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = { open = false }) { Text("Annuler") } },
    ) { DatePicker(state) }
}

private fun LocalDate.toMillis() = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()

/** Dropdown to pick one of [options], shown with [label]. */
@Composable
fun <T> Dropdown(
    title: String,
    selected: T,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = label(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(label(option)) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
