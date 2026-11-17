// Developer section: replay the feature tour, load the demo history, or erase every personal data (back to onboarding).
package com.clement.nutrimo.ui.more

import androidx.annotation.StringRes
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
import com.clement.nutrimo.ui.common.asString
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.util.uiText

/** Each action with its button, confirmation texts and the work it does (returns the success message). */
enum class DevAction(
    @StringRes val button: Int,
    @StringRes val confirmTitle: Int,
    @StringRes val confirmLabel: Int,
    val run: suspend (AppContainer) -> UiText,
) {
    DEMO(R.string.dev_demo_button, R.string.dev_demo_confirm, R.string.dev_demo_action, { c -> DemoData(c).load(); uiText(R.string.dev_demo_done) }),
    CLEAR(R.string.dev_clear_button, R.string.dev_clear_confirm, R.string.dev_clear_action, { c ->
        DemoData(c).clear()
        c.settings.setOnboardingDone(false)
        uiText(R.string.dev_clear_done)
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
                    WideButton(stringResource(action.button), { confirm = action }, enabled = !runner.busy, outlined = action == DevAction.CLEAR)
                }
                if (runner.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                runner.status?.let { Text(it.asString()) }
            }
        }
    }
    confirm?.let { action ->
        ConfirmDialog(
            title = stringResource(action.confirmTitle),
            text = stringResource(R.string.dev_toutes_vos_donnees_actuelles_seront),
            confirmLabel = stringResource(action.confirmLabel),
            onConfirm = { vm.run(action) },
            onDismiss = { confirm = null },
        )
    }
}
