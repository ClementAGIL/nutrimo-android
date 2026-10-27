// Developer section: load the demo history, or erase every personal data (back to onboarding).
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.demo.DemoData
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.rememberStatusRunner

/** Each action with its button, confirmation texts and the work it does (returns the success message). */
private enum class DevAction(val button: String, val confirmTitle: String, val confirmLabel: String, val run: suspend (AppContainer) -> String) {
    DEMO("Charger les données de démo", "Charger la démo ?", "Charger", { c -> DemoData(c).load(); "Données de démo chargées." }),
    CLEAR("Tout effacer", "Tout effacer ?", "Effacer", { c ->
        DemoData(c).clear()
        c.settings.setOnboardingDone(false)
        "Données effacées."
    }),
}

@Composable
fun DevScreen(onBack: () -> Unit) {
    val c = appContainer()
    var confirm by remember { mutableStateOf<DevAction?>(null) }
    // App scope: leaving the screen must not stop the wipe or the load half-way.
    val runner = rememberStatusRunner(c.appScope)
    ScreenScaffold("Développeur", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            SectionCard("Données de démo") {
                IntroText(
                    "Remplace toutes vos données par un historique fictif : profil, 2 objectifs, 5 mois de pesées, " +
                        "mensurations, eau, activités, un aliment perso, 3 recettes (dont une sous-recette) et 30 jours de repas.",
                )
                DevAction.entries.forEach { action ->
                    WideButton(action.button, { confirm = action }, enabled = !runner.busy, outlined = action == DevAction.CLEAR)
                }
                if (runner.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                runner.status?.let { Text(it) }
            }
        }
    }
    confirm?.let { action ->
        ConfirmDialog(
            title = action.confirmTitle,
            text = "Toutes vos données actuelles seront supprimées. Exportez-les avant si besoin.",
            confirmLabel = action.confirmLabel,
            onConfirm = { runner.run { action.run(c) } },
            onDismiss = { confirm = null },
        )
    }
}
