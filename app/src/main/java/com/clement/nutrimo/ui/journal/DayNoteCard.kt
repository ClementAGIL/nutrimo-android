// "Ma journée" card of the journal: the mood of the day in one tap (5 faces), and a short journal (gratitude, victory
// of the day, free note) written in a dialog.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clement.nutrimo.data.db.DayNoteEntity
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.FormDialog
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.SectionCard
import java.time.LocalDate

/** From hard day to great day; index + 1 = mood stored. */
private val MOODS = listOf("😣" to "Difficile", "😕" to "Bof", "😐" to "Moyenne", "🙂" to "Bonne", "😄" to "Excellente")

const val GRATITUDE_PROMPT = "Pour quoi êtes-vous reconnaissant·e aujourd'hui ?"
const val WIN_PROMPT = "Une victoire du jour, même petite"
const val NOTE_PROMPT = "Quelques mots sur votre journée"

@Composable
fun DayNoteCard(date: LocalDate, note: DayNoteEntity?, onSave: (DayNoteEntity) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val current = note ?: DayNoteEntity(date)
    SectionCard {
        CardHeader("Ma journée", "Comment s'est passée votre journée ?")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MOODS.forEachIndexed { i, (emoji, label) ->
                val mood = i + 1
                // A second tap on the chosen face clears it.
                MoodFace(emoji, label, current.mood == mood) { onSave(current.copy(mood = if (current.mood == mood) null else mood)) }
            }
        }
        current.mood?.let { HintText("Journée ${MOODS[it - 1].second.lowercase()}") }
        val written = listOf(GRATITUDE_PROMPT to current.gratitude, WIN_PROMPT to current.win, NOTE_PROMPT to current.note).filter { it.second.isNotBlank() }
        if (written.isEmpty()) {
            HintText("Une gratitude, une victoire, quelques mots : écrire aide à voir le chemin parcouru et à être doux·ce avec soi.")
        } else {
            written.forEach { (prompt, text) ->
                Column {
                    Text(prompt, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        OutlinedButton({ editing = true }, Modifier.fillMaxWidth()) { Text(if (written.isEmpty()) "Écrire" else "Modifier") }
    }
    if (editing) DayNoteDialog(current, onSave) { editing = false }
}

@Composable
private fun MoodFace(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Box(
        Modifier.size(48.dp).background(background, CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "Journée $label" + if (selected) ", choisie" else "" },
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = if (selected) 28.sp else 24.sp)
    }
}

@Composable
private fun DayNoteDialog(note: DayNoteEntity, onSave: (DayNoteEntity) -> Unit, onDismiss: () -> Unit) {
    var gratitude by remember { mutableStateOf(note.gratitude) }
    var win by remember { mutableStateOf(note.win) }
    var text by remember { mutableStateOf(note.note) }
    FormDialog("Ma journée", onDismiss, { onSave(note.copy(gratitude = gratitude, win = win, note = text)) }, confirmLabel = "Enregistrer") {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(gratitude, { gratitude = it }, Modifier.fillMaxWidth(), label = { Text(GRATITUDE_PROMPT) }, minLines = 2)
            OutlinedTextField(win, { win = it }, Modifier.fillMaxWidth(), label = { Text(WIN_PROMPT) }, minLines = 2)
            OutlinedTextField(text, { text = it }, Modifier.fillMaxWidth(), label = { Text(NOTE_PROMPT) }, minLines = 3)
        }
    }
}
