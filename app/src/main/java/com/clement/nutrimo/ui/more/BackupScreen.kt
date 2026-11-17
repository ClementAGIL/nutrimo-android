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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
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
    ScreenScaffold(stringResource(R.string.backup_sauvegarde), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                stringResource(R.string.backup_vos_donnees_profil_objectifs_pesees),
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
    SectionCard(stringResource(R.string.backup_fichier)) {
        HintText(stringResource(R.string.backup_fichier_zip_avec_vos_donnees))
        PhotoStorageText(vm)
        WideButton(stringResource(R.string.backup_exporter), { exporter.launch("nutrimo-${LocalDate.now()}.zip") })
        WideButton(stringResource(R.string.backup_importer_sauvegarde), { importer.launch(arrayOf(MIME_ZIP, MIME_JSON, "*/*")) }, outlined = true)
    }
}

/** "12 photos · 9,4 Mo": what the photos take on the phone (and in the export). */
@Composable
private fun PhotoStorageText(vm: BackupViewModel) {
    val storage by produceState<PhotoStorage?>(null) { value = vm.photoStorage() }
    storage?.let { HintText(stringResource(R.string.backup_photos_1_2_mo, it.count, fmt(it.bytes / BYTES_PER_MB))) }
}

/** Backup on the Nutrimo server (data only, never the photos): now, restore the latest, and the daily automatic backup. */
@Composable
private fun ServerCard(vm: BackupViewModel) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmRestore by rememberSaveable { mutableStateOf(false) }
    val s = settings ?: return
    SectionCard(stringResource(R.string.backup_serveur_nutrimo_pc)) {
        HintText(if (s.lastServerBackup == 0L) stringResource(R.string.backup_aucune_sauvegarde_envoyee) else stringResource(R.string.backup_derniere_sauvegarde_1, dateTime(s.lastServerBackup)))
        WideButton(stringResource(R.string.backup_sauvegarder_maintenant), vm::pushToServer)
        WideButton(stringResource(R.string.backup_restaurer_derniere_sauvegarde), { confirmRestore = true }, outlined = true)
        SwitchRow(stringResource(R.string.backup_sauvegarde_automatique_chaque_jour_en), s.autoBackup, { vm.setAutoBackup(context, it) })
    }
    if (confirmRestore) {
        ConfirmDialog(
            stringResource(R.string.backup_restaurer), stringResource(R.string.backup_donnees_serveur_seront_fusionnees_avec), stringResource(R.string.backup_restaurer_2),
            vm::restoreFromServer,
            { confirmRestore = false },
        )
    }
}
