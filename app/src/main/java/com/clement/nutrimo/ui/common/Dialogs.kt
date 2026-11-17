// Dialogs: generic form dialog (confirm + "Annuler"), confirmation, a text-input dialog, and an ⓘ help button.
package com.clement.nutrimo.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Dialog with [content], a confirm button ([confirmLabel], enabled by [canConfirm]) and "Annuler".
 * The dialog closes after [onConfirm] unless [closeOnConfirm] is false (e.g. a check must run first).
 */
@Composable
fun FormDialog(
    title: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = "OK",
    canConfirm: Boolean = true,
    closeOnConfirm: Boolean = true,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = title?.let { { Text(it) } },
        text = content,
        confirmButton = {
            TextButton(enabled = canConfirm, onClick = {
                onConfirm()
                if (closeOnConfirm) onDismiss()
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Confirmer",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormDialog(title, onDismiss, onConfirm, confirmLabel) { Text(text) }
}

/** Asks for a non-blank text; [onConfirm] receives it trimmed. */
@Composable
fun TextDialog(title: String, label: String, initial: String = "", onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    FormDialog(title, onDismiss, { onConfirm(text.trim()) }, canConfirm = text.isNotBlank()) {
        OutlinedTextField(text, { text = it }, label = { Text(label) }, singleLine = true)
    }
}

/** An ⓘ icon button that opens [title] / [text] in a dialog: keeps help text out of crowded cards. */
@Composable
fun InfoButton(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    IconButton({ open = true }) {
        Icon(Icons.Outlined.Info, contentDescription = "Aide : $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton({ open = false }) { Text("OK") } },
            title = { Text(title) },
            text = { Text(text) },
        )
    }
}
