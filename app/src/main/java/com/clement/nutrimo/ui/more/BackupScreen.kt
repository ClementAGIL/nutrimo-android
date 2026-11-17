// Backups: on the PC server (data only), or a ZIP file with the data and the progress photos; import reads ZIP or JSON.
package com.clement.nutrimo.ui.more

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.repository.PhotoStorage
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
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.util.dateTime
import com.clement.nutrimo.util.fmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val MIME_JSON = "application/json"
private const val MIME_ZIP = "application/zip"
private const val BYTES_PER_MB = 1_000_000.0

/** Every action runs in the ViewModel scope: an import or a restore keeps going if the user leaves the screen. */
class BackupViewModel(private val c: AppContainer) : ViewModel() {
    val runner = StatusRunner(viewModelScope)
    val settings = c.settings.settings.stateWhileSubscribed(viewModelScope, null)

    suspend fun photoStorage(): PhotoStorage = c.photos.storage()

    fun export(resolver: ContentResolver, uri: Uri) = runner.run {
        withContext(Dispatchers.IO) { resolver.openOutputStream(uri) }?.use { c.archive.write(it) }
        "Sauvegarde exportée (données et photos)."
    }

    fun import(resolver: ContentResolver, uri: Uri) = runner.run {
        val bytes = withContext(Dispatchers.IO) { resolver.openInputStream(uri)?.use { it.readBytes() } } ?: ByteArray(0)
        val s = c.backup.import(c.archive.backupText(bytes))
        "Importé : ${s.weights} pesées, ${s.diary} entrées du journal, ${s.recipes} recettes, ${s.foods} aliments."
    }

    fun pushToServer() = runner.run { c.serverBackup.push(); "Sauvegarde envoyée au serveur." }

    fun restoreFromServer() = runner.run {
        val r = c.serverBackup.restoreLatest()
        "Restauré : ${r.weights} pesées, ${r.diary} entrées, ${r.recipes} recettes."
    }

    fun setAutoBackup(context: Context, on: Boolean) = runner.run {
        c.settings.setAutoBackup(on)
        ServerBackup.schedule(context, on)
        if (on) "Sauvegarde automatique activée." else "Sauvegarde automatique désactivée."
    }
}

@Composable
fun BackupScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> BackupViewModel(c) }
    val runner = vm.runner
    ScreenScaffold("Sauvegarde", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                "Vos données (profil, objectifs, pesées, mensurations, journal, recettes, aliments perso) sont " +
                    "stockées sur ce téléphone, jamais dans les sauvegardes Google. Sauvegardez-les sur votre PC ou dans un fichier.",
            )
            ServerCard(vm)
            FileCard(vm)
            runner.status?.let { Text(it) }
        }
    }
}

/** Export to / import from a file chosen with the system picker. */
@Composable
private fun FileCard(vm: BackupViewModel) {
    val resolver = LocalContext.current.contentResolver
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(MIME_ZIP)) { uri: Uri? ->
        uri?.let { vm.export(resolver, it) }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { vm.import(resolver, it) }
    }
    SectionCard("Fichier") {
        HintText("Un fichier ZIP avec vos données et vos photos de progression. L'import reprend les données (pas les photos).")
        PhotoStorageText(vm)
        WideButton("Exporter", { exporter.launch("nutrimo-${LocalDate.now()}.zip") })
        WideButton("Importer une sauvegarde", { importer.launch(arrayOf(MIME_ZIP, MIME_JSON, "*/*")) }, outlined = true)
    }
}

/** "12 photos · 9,4 Mo": what the photos take on the phone (and in the export). */
@Composable
private fun PhotoStorageText(vm: BackupViewModel) {
    val storage by produceState<PhotoStorage?>(null) { value = vm.photoStorage() }
    storage?.let { HintText("Photos : ${it.count} · ${fmt(it.bytes / BYTES_PER_MB)} Mo") }
}

/** Backup on the Nutrimo server (data only, never the photos): now, restore the latest, and the daily automatic backup. */
@Composable
private fun ServerCard(vm: BackupViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
    SectionCard("Serveur Nutrimo (PC)") {
        HintText(if (s.lastServerBackup == 0L) "Aucune sauvegarde envoyée." else "Dernière sauvegarde : ${dateTime(s.lastServerBackup)}")
        WideButton("Sauvegarder maintenant", vm::pushToServer)
        WideButton("Restaurer la dernière sauvegarde", { confirmRestore = true }, outlined = true)
        SwitchRow("Sauvegarde automatique (chaque jour, en Wi-Fi)", s.autoBackup, { vm.setAutoBackup(context, it) })
    }
    if (confirmRestore) {
        ConfirmDialog(
            "Restaurer ?", "Les données du serveur seront fusionnées avec celles du téléphone (mêmes éléments remplacés).", "Restaurer",
            vm::restoreFromServer,
            { confirmRestore = false },
        )
    }
}
