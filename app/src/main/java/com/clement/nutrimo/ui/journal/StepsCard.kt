// Journal card for the day's steps from Health Connect: install / connect prompts, then progress to the goal.
package com.clement.nutrimo.ui.journal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.PermissionController
import com.clement.nutrimo.R
import com.clement.nutrimo.data.health.HEALTH_CONNECT_PACKAGE
import com.clement.nutrimo.data.health.HEALTH_PERMISSIONS
import com.clement.nutrimo.data.health.HealthInfo
import com.clement.nutrimo.data.health.HealthStatus
import com.clement.nutrimo.domain.calc.STEPS_BASELINE
import com.clement.nutrimo.ui.common.CardHeader
import com.clement.nutrimo.ui.common.GoalSettingsButton
import com.clement.nutrimo.ui.common.HintText
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.kcal

private const val PLAY_STORE = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"

@Composable
fun StepsCard(info: HealthInfo?, goal: Int, inBudget: Boolean, onRefresh: () -> Unit, onGoalSettings: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { onRefresh() }
    SectionCard {
        CardHeader("Pas") {
            // Other apps sync to Health Connect in the background, so the data can change while the journal is open.
            if (info?.canReadSteps == true) IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Actualiser") }
            GoalSettingsButton(onGoalSettings)
        }
        when {
            info == null -> Unit
            info.status == HealthStatus.UNAVAILABLE -> HintText(stringResource(R.string.steps_health_connect_n_est_pas))
            info.status == HealthStatus.NEEDS_UPDATE -> {
                HintText(stringResource(R.string.steps_installez_ou_mettez_jour_health))
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE)).setPackage("com.android.vending")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.steps_ouvrir_play_store)) }
            }
            !info.canReadSteps -> {
                HintText(stringResource(R.string.steps_nutrimo_peut_lire_vos_pas))
                WideButton(stringResource(R.string.steps_connecter_health_connect), { permission.launch(HEALTH_PERMISSIONS) }, outlined = true)
            }
            else -> {
                StepsProgress(info, goal, inBudget)
                if (!info.allGranted) TextButton(onClick = { permission.launch(HEALTH_PERMISSIONS) }) {
                    Text(stringResource(R.string.steps_autoriser_aussi_sport_calories))
                }
            }
        }
    }
}

@Composable
private fun StepsProgress(info: HealthInfo, goal: Int, inBudget: Boolean) {
    val steps = info.steps ?: 0
    ProgressRow(stringResource(R.string.steps_1_2_pas, fmt(steps.toDouble(), 0), fmt(goal.toDouble(), 0)), "", steps.toDouble() / goal, NutrimoColors.steps)
    // Health Connect's calories replace our step estimate (see healthBudgetKcal): the summary card shows them.
    if (info.activeKcal != null) return
    val detail = when {
        steps <= STEPS_BASELINE -> stringResource(R.string.steps_pas_au_dela_1_comptent, fmt(STEPS_BASELINE.toDouble(), 0))
        inBudget -> stringResource(R.string.steps_1_ajoutees_au_budget_pas, kcal(info.stepsKcal), fmt(STEPS_BASELINE.toDouble(), 0))
        else -> stringResource(R.string.steps_1_depensees_en_plus_non, kcal(info.stepsKcal))
    }
    HintText(detail)
}
