// Export all personal data to a JSON file, or import one (rows with the same id are replaced).
package com.clement.nutrimo.ui.more

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appContainer
import java.time.LocalDate
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
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Toutes vos données sont stockées uniquement sur ce téléphone. Exportez-les régulièrement " +
                    "(profil, objectifs, pesées, mensurations, journal, recettes, aliments perso).",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = { exporter.launch("nutrimo-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
                Text("Exporter")
            }
            OutlinedButton(onClick = { importer.launch(arrayOf(MIME_JSON, "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Text("Importer une sauvegarde")
            }
            status?.let { Text(it) }
        }
    }
}

private fun launchCatching(scope: CoroutineScope, report: (String) -> Unit, block: suspend () -> String) {
    scope.launch {
        report(runCatching { block() }.getOrElse { "Erreur : ${it.message}" })
    }
}
