// Journal card for the day's steps from Health Connect: install / connect prompts, then progress to the goal.
package com.clement.nutrimo.ui.journal

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.clement.nutrimo.data.health.HEALTH_CONNECT_PACKAGE
import com.clement.nutrimo.data.health.HealthStatus
import com.clement.nutrimo.data.health.HEALTH_PERMISSIONS
import com.clement.nutrimo.domain.calc.STEPS_BASELINE
import com.clement.nutrimo.ui.common.ProgressRow
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.fmt
import com.clement.nutrimo.ui.common.kcal
import com.clement.nutrimo.ui.theme.NutrimoColors

private const val PLAY_STORE = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding"

@Composable
fun StepsCard(info: HealthInfo?, goal: Int, inBudget: Boolean, onRefresh: () -> Unit) {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { onRefresh() }
    SectionCard("Pas") {
        when {
            info == null -> Unit
            info.status == HealthStatus.UNAVAILABLE -> Hint("Health Connect n'est pas disponible sur ce téléphone.")
            info.status == HealthStatus.NEEDS_UPDATE -> {
                Hint("Installez ou mettez à jour Health Connect pour récupérer vos pas.")
                OutlinedButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE)).setPackage("com.android.vending")) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ouvrir le Play Store") }
            }
            !info.canReadSteps -> {
                Hint("Nutrimo peut lire vos pas et vos séances de sport enregistrés par le téléphone, Google Fit, Mi Fitness… via Health Connect.")
                OutlinedButton(onClick = { permission.launch(HEALTH_PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Connecter Health Connect")
                }
            }
            else -> StepsProgress(info, goal, inBudget)
        }
    }
}

@Composable
private fun StepsProgress(info: HealthInfo, goal: Int, inBudget: Boolean) {
    val steps = info.steps ?: 0
    ProgressRow("${fmt(steps.toDouble(), 0)} / ${fmt(goal.toDouble(), 0)} pas", "", steps.toDouble() / goal, NutrimoColors.weight)
    val detail = when {
        steps <= STEPS_BASELINE -> "Les pas au-delà de ${fmt(STEPS_BASELINE.toDouble(), 0)} comptent comme activité."
        inBudget -> "≈ ${kcal(info.stepsKcal)} ajoutées au budget (pas au-delà de ${fmt(STEPS_BASELINE.toDouble(), 0)})."
        else -> "≈ ${kcal(info.stepsKcal)} dépensées en plus (non ajoutées au budget, voir Plus → Pas)."
    }
    Hint(detail)
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
