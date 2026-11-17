// "Ma journée" card of the journal: the mood of the day in one tap (5 faces), and a short journal written in a
// full-screen editor (close / save icons at the top, so the keyboard never hides them; nothing is lost by mistake).
package com.clement.nutrimo.ui.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.DayNoteEntity
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.SectionCard
import java.time.LocalDate

/** From hard day to great day; index + 1 = mood stored. */
private val MOODS = listOf("😣" to "Difficile", "😕" to "Bof", "😐" to "Moyenne", "🙂" to "Bonne", "😄" to "Excellente")

/** "🙂 Bonne", for the calendar. */
fun moodText(mood: Int): String = MOODS.getOrNull(mood - 1)?.let { (face, label) -> "$face $label" } ?: ""

private const val PLACEHOLDER =
    "Pour quoi êtes-vous reconnaissant·e aujourd'hui ?\nUne victoire du jour, même petite ?\nQuelques mots sur votre journée…"

/** The text of the day; notes written before the single field had three parts, joined here. */
private val DayNoteEntity.text: String get() = listOf(gratitude, win, note).filter { it.isNotBlank() }.joinToString("\n\n")

@Composable
fun DayNoteCard(date: LocalDate, note: DayNoteEntity?, onSave: (DayNoteEntity) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    val current = note ?: DayNoteEntity(date)
    SectionCard {
        CardHeader(stringResource(R.string.day_note_ma_journee), stringResource(R.string.day_note_comment_est_passee_votre_journee))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MOODS.forEachIndexed { i, (emoji, label) ->
                val mood = i + 1
                // A second tap on the chosen face clears it.
                MoodFace(emoji, label, current.mood == mood) { onSave(current.copy(mood = if (current.mood == mood) null else mood)) }
            }
        }
        current.mood?.let { HintText(stringResource(R.string.day_note_journee_1, MOODS[it - 1].second.lowercase())) }
        val text = current.text
        if (text.isEmpty()) {
            HintText(stringResource(R.string.day_note_gratitude_victoire_quelques_mots_ecrire))
        } else {
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton({ editing = true }, Modifier.fillMaxWidth()) { Text(if (text.isEmpty()) stringResource(R.string.day_note_ecrire) else stringResource(R.string.common_modifier)) }
    }
    if (editing) {
        DayNoteEditor(text = current.text, onSave = { onSave(current.copy(gratitude = "", win = "", note = it)) }) { editing = false }
    }
}

@Composable
private fun MoodFace(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    // Read here: the semantics block is not composable.
    val description = stringResource(R.string.day_note_journee_1, label) + if (selected) stringResource(R.string.day_note_choisie) else ""
    Box(
        Modifier.size(48.dp).background(background, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = if (selected) 28.sp else 24.sp)
    }
}

/**
 * Full screen, so a long text never pushes the buttons under the keyboard. A tap outside does nothing (it used to
 * close the dialog and lose the text); leaving with unsaved changes (✕ or back) asks first. The draft survives
 * a rotation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayNoteEditor(text: String, onSave: (String) -> Unit, onClose: () -> Unit) {
    var draft by rememberSaveable { mutableStateOf(text) }
    var confirmLeave by remember { mutableStateOf(false) }
    val changed = draft.trim() != text.trim()
    val leave = { if (changed) confirmLeave = true else onClose() }
    val focus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = leave,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false, decorFitsSystemWindows = false),
    ) {
        BackHandler(onBack = leave)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.day_note_ma_journee)) },
                    navigationIcon = { IconButton(leave) { Icon(Icons.Default.Close, stringResource(R.string.common_fermer)) } },
                    actions = {
                        IconButton({ onSave(draft); onClose() }) { Icon(Icons.Outlined.Save, stringResource(R.string.common_enregistrer)) }
                    },
                )
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().padding(16.dp)) {
                OutlinedTextField(
                    draft, { draft = it }, Modifier.fillMaxWidth().weight(1f).focusRequester(focus),
                    placeholder = { Text(PLACEHOLDER) },
                )
            }
        }
        // Inside the editor's window, so it shows above it.
        if (confirmLeave) {
            ConfirmDialog(
                title = stringResource(R.string.day_note_abandonner_texte),
                text = stringResource(R.string.day_note_vos_modifications_ne_seront_pas),
                confirmLabel = stringResource(R.string.day_note_abandonner),
                onConfirm = onClose,
                onDismiss = { confirmLeave = false },
            )
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }
}
