// Export all personal data to a JSON file, or import one (rows with the same id are replaced).
package com.clement.nutrimo.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.data.sync.ServerBackup
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatusRunner
import com.clement.nutrimo.ui.common.SwitchRow
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.dateTime
import com.clement.nutrimo.ui.common.rememberStatusRunner
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIME_JSON = "application/json"

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val runner = rememberStatusRunner()
    ScreenScaffold("Sauvegarde", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                "Vos données (profil, objectifs, pesées, mensurations, journal, recettes, aliments perso) sont " +
                    "stockées sur ce téléphone. Sauvegardez-les sur votre PC ou dans un fichier.",
            )
            ServerCard(runner)
            FileCard(runner)
            runner.status?.let { Text(it) }
        }
    }
}

/** Export to / import from a file chosen with the system picker. */
@Composable
private fun FileCard(runner: StatusRunner) {
    val c = appContainer()
    val resolver = LocalContext.current.contentResolver
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_JSON)) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runner.run {
            val json = c.backup.export()
            withContext(Dispatchers.IO) { resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) } }
            "Sauvegarde exportée."
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runner.run {
            val text = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } } ?: ""
            val s = c.backup.import(text)
            "Importé : ${s.weights} pesées, ${s.diary} entrées du journal, ${s.recipes} recettes, ${s.foods} aliments."
        }
    }
    SectionCard("Fichier") {
        WideButton("Exporter", { exporter.launch("nutrimo-${LocalDate.now()}.json") })
        WideButton("Importer une sauvegarde", { importer.launch(arrayOf(MIME_JSON, "*/*")) }, outlined = true)
    }
}

/** Backup on the Nutrimo server: now, restore the latest, and the daily automatic backup. */
@Composable
private fun ServerCard(runner: StatusRunner) {
    val c = appContainer()
    val context = LocalContext.current
    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
    SectionCard("Serveur Nutrimo (PC)") {
        HintText(if (s.lastServerBackup == 0L) "Aucune sauvegarde envoyée." else "Dernière sauvegarde : ${dateTime(s.lastServerBackup)}")
        WideButton("Sauvegarder maintenant", { runner.run { c.serverBackup.push(); "Sauvegarde envoyée au serveur." } })
        WideButton("Restaurer la dernière sauvegarde", { confirmRestore = true }, outlined = true)
        SwitchRow("Sauvegarde automatique (chaque jour, en Wi-Fi)", s.autoBackup, { on ->
            runner.run {
                c.settings.setAutoBackup(on)
                ServerBackup.schedule(context, on)
                if (on) "Sauvegarde automatique activée." else "Sauvegarde automatique désactivée."
            }
        })
    }
    if (confirmRestore) {
        ConfirmDialog(
            "Restaurer ?", "Les données du serveur seront fusionnées avec celles du téléphone (mêmes éléments remplacés).", "Restaurer",
            { runner.run { val r = c.serverBackup.restoreLatest(); "Restauré : ${r.weights} pesées, ${r.diary} entrées, ${r.recipes} recettes." } },
            { confirmRestore = false },
        )
    }
}
