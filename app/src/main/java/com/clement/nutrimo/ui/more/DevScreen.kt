// Developer section: load the demo history, or erase every personal data (back to onboarding).
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.clement.nutrimo.data.demo.DemoData
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appContainer
import kotlinx.coroutines.launch

private enum class DevAction { DEMO, CLEAR }

@Composable
fun DevScreen(onBack: () -> Unit) {
    val c = appContainer()
    var confirm by remember { mutableStateOf<DevAction?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    // Runs in the app scope so leaving the screen does not stop it half-way.
    val run = { action: DevAction ->
        busy = true
        c.appScope.launch {
            status = runCatching {
                if (action == DevAction.DEMO) DemoData(c).load() else DemoData(c).clear().also { c.settings.setOnboardingDone(false) }
                if (action == DevAction.DEMO) "Données de démo chargées." else "Données effacées."
            }.getOrElse { "Erreur : ${it.message}" }
            busy = false
        }
    }
    ScreenScaffold("Développeur", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            SectionCard("Données de démo") {
                Text(
                    "Remplace toutes vos données par un historique fictif : profil, 2 objectifs, 5 mois de pesées, " +
                        "mensurations, eau, un aliment perso, 3 recettes (dont une sous-recette) et 30 jours de repas.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = { confirm = DevAction.DEMO }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Charger les données de démo")
                }
                OutlinedButton(onClick = { confirm = DevAction.CLEAR }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text("Tout effacer")
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                status?.let { Text(it) }
            }
        }
    }
    confirm?.let { action ->
        ConfirmDialog(
            title = if (action == DevAction.DEMO) "Charger la démo ?" else "Tout effacer ?",
            text = "Toutes vos données actuelles seront supprimées. Exportez-les avant si besoin.",
            confirmLabel = if (action == DevAction.DEMO) "Charger" else "Effacer",
            onConfirm = { run(action) },
            onDismiss = { confirm = null },
        )
    }
}
