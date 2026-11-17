// Developer section: replay the feature tour, load the demo history, or erase every personal data (back to onboarding).
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.demo.DemoData
import com.clement.nutrimo.ui.common.ConfirmDialog
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatusRunner
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel

/** Each action with its button, confirmation texts and the work it does (returns the success message). */
enum class DevAction(val button: String, val confirmTitle: String, val confirmLabel: String, val run: suspend (AppContainer) -> String) {
    DEMO("Charger les données de démo", "Charger la démo ?", "Charger", { c -> DemoData(c).load(); "Données de démo chargées." }),
    CLEAR("Tout effacer", "Tout effacer ?", "Effacer", { c ->
        DemoData(c).clear()
        c.settings.setOnboardingDone(false)
        "Données effacées."
    }),
}

class DevViewModel(private val c: AppContainer) : ViewModel() {
    /** App scope: leaving the screen (or the ViewModel being cleared) must not stop the wipe or the load half-way. */
    val runner = StatusRunner(c.appScope)

    fun run(action: DevAction) = runner.run { action.run(c) }
}

@Composable
fun DevScreen(onBack: () -> Unit, onShowIntro: () -> Unit) {
    val vm = appViewModel { c, _ -> DevViewModel(c) }
    var confirm by remember { mutableStateOf<DevAction?>(null) }
    val runner = vm.runner
    ScreenScaffold(stringResource(R.string.dev_developpeur), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            SectionCard(stringResource(R.string.dev_presentation)) {
                IntroText(stringResource(R.string.dev_presentation_fonctionnalites_ne_affiche_qu))
                WideButton(stringResource(R.string.dev_revoir_presentation), onShowIntro)
            }
            SectionCard(stringResource(R.string.dev_donnees_demo)) {
                IntroText(
                    stringResource(R.string.dev_remplace_toutes_vos_donnees_par),
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
            text = stringResource(R.string.dev_toutes_vos_donnees_actuelles_seront),
            confirmLabel = action.confirmLabel,
            onConfirm = { vm.run(action) },
            onDismiss = { confirm = null },
        )
    }
}
