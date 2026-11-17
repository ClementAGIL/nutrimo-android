// Activity settings: daily step and sport goals, whether extra steps raise the calorie budget, and a link to Health Connect.
package com.clement.nutrimo.ui.more

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.domain.calc.STEPS_BASELINE
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.SwitchRow
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.util.fmt
import kotlinx.coroutines.launch

private const val MIN_STEP_GOAL = 1000
private const val MIN_SPORT_KCAL = 50

/** Form texts; the values are valid when the parsed goals are not null. */
class ActivityGoalsForm(steps: String = "", sport: String = "", inBudget: Boolean = false) {
    var steps by mutableStateOf(steps)
    var sport by mutableStateOf(sport)
    var inBudget by mutableStateOf(inBudget)
    val stepGoal: Int? get() = steps.toIntOrNull()?.takeIf { it >= MIN_STEP_GOAL }
    val sportGoal: Int? get() = sport.toIntOrNull()?.takeIf { it >= MIN_SPORT_KCAL }
}

class StepsViewModel(private val c: AppContainer) : ViewModel() {
    val form = ActivityGoalsForm()

    init {
        viewModelScope.launch {
            val s = c.settings.current()
            form.steps = s.stepGoal.toString()
            form.sport = s.sportGoalKcal.toString()
            form.inBudget = s.healthInBudget
        }
    }

    fun save(onDone: () -> Unit) {
        val steps = form.stepGoal ?: return
        val sport = form.sportGoal ?: return
        viewModelScope.launch { c.settings.setActivityGoals(steps, sport, form.inBudget); onDone() }
    }
}

@Composable
fun StepsScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> StepsViewModel(c) }
    val context = LocalContext.current
    val form = vm.form
    ScreenScaffold(stringResource(R.string.steps_pas_sport), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                stringResource(R.string.steps_pas_seances_sport_sont_lus),
            )
            GoalsCard(form)
            // Absent on phones without Health Connect: ignore instead of crashing.
            WideButton(stringResource(R.string.steps_ouvrir_reglages_health_connect), {
                runCatching { context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) }
            }, outlined = true)
            WideButton(stringResource(R.string.common_enregistrer), { vm.save(onBack) }, enabled = form.stepGoal != null && form.sportGoal != null)
        }
    }
}

@Composable
private fun GoalsCard(form: ActivityGoalsForm) {
    SectionCard {
        NumberField(
            form.steps, { form.steps = it.filter(Char::isDigit) }, stringResource(R.string.steps_objectif_pas_quotidien), Modifier.fillMaxWidth(), "pas",
            isError = form.stepGoal == null,
        )
        NumberField(
            form.sport, { form.sport = it.filter(Char::isDigit) }, stringResource(R.string.steps_objectif_sport_quotidien), Modifier.fillMaxWidth(), "kcal",
            isError = form.sportGoal == null,
        )
        SwitchRow(stringResource(R.string.steps_ajouter_pas_seances_au_budget), form.inBudget, { form.inBudget = it })
        HintText(
            stringResource(R.string.steps_votre_niveau_activite_inclut_deja, fmt(STEPS_BASELINE.toDouble(), 0)),
        )
    }
}
