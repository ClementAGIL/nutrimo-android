// Export all personal data to a JSON file, or import one (rows with the same id are replaced).
package com.clement.nutrimo.ui.more

import com.clement.nutrimo.ui.common.IntroText
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.clement.nutrimo.data.sync.ServerBackup
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appContainer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIME_JSON = "application/json"

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val c = appContainer()
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    val run = { block: suspend () -> String -> launchCatching(scope, { status = it }, block) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_JSON)) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run {
            val json = c.backup.export()
            withContext(Dispatchers.IO) { resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
            "Sauvegarde exportée."
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        run {
            val text = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } } ?: ""
            val s = c.backup.import(text)
            "Importé : ${s.weights} pesées, ${s.diary} entrées du journal, ${s.recipes} recettes, ${s.foods} aliments."
        }
    }
    ScreenScaffold("Sauvegarde", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                "Vos données (profil, objectifs, pesées, mensurations, journal, recettes, aliments perso) sont " +
                    "stockées sur ce téléphone. Sauvegardez-les sur votre PC ou dans un fichier.",
            )
            ServerCard(run)
            SectionCard("Fichier") {
                Button(onClick = { exporter.launch("nutrimo-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Exporter")
                }
                OutlinedButton(onClick = { importer.launch(arrayOf(MIME_JSON, "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Importer une sauvegarde")
                }
            }
            status?.let { Text(it) }
        }
    }
}

/** Backup on the Nutrimo server: now, restore the latest, and the daily automatic backup. */
@Composable
private fun ServerCard(run: (suspend () -> String) -> Unit) {
    val c = appContainer()
    val context = LocalContext.current
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
    SectionCard("Serveur Nutrimo (PC)") {
        Text(
            if (s.lastServerBackup == 0L) "Aucune sauvegarde envoyée." else "Dernière sauvegarde : ${formatInstant(s.lastServerBackup)}",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = { run { c.serverBackup.push(); "Sauvegarde envoyée au serveur." } }, modifier = Modifier.fillMaxWidth()) {
            Text("Sauvegarder maintenant")
        }
        OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Restaurer la dernière sauvegarde") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sauvegarde automatique (chaque jour, en Wi-Fi)", Modifier.weight(1f))
            Switch(checked = s.autoBackup, onCheckedChange = { on ->
                run { c.settings.setAutoBackup(on); ServerBackup.schedule(context, on); if (on) "Sauvegarde automatique activée." else "Sauvegarde automatique désactivée." }
            })
        }
    }
    if (confirmRestore) {
        ConfirmDialog(
            "Restaurer ?", "Les données du serveur seront fusionnées avec celles du téléphone (mêmes éléments remplacés).", "Restaurer",
            { run { val r = c.serverBackup.restoreLatest(); "Restauré : ${r.weights} pesées, ${r.diary} entrées, ${r.recipes} recettes." } },
            { confirmRestore = false },
        )
    }
}

private val INSTANT_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy 'à' HH:mm", Locale.FRANCE)

private fun formatInstant(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(INSTANT_FORMAT)

private fun launchCatching(scope: CoroutineScope, report: (String) -> Unit, block: suspend () -> String) {
    scope.launch {
        report(runCatching { block() }.getOrElse { "Erreur : ${it.message}" })
    }
}
