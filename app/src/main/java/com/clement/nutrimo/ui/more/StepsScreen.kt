// Activity settings: daily step and sport goals, whether extra steps raise the calorie budget, and a link to Health Connect.
package com.clement.nutrimo.ui.more

import android.content.Intent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import com.clement.nutrimo.domain.calc.STEPS_BASELINE
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.fmt
import kotlinx.coroutines.launch

private const val MIN_GOAL = 1000
private const val MIN_SPORT_KCAL = 50

@Composable
fun StepsScreen(onBack: () -> Unit) {
    val c = appContainer()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var goal by remember { mutableStateOf("") }
    var sport by remember { mutableStateOf("") }
    var inBudget by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { c.settings.current().let {
            goal = it.stepGoal.toString()
            sport = it.sportGoalKcal.toString()
            inBudget = it.healthInBudget
        } }
    val goalValue = goal.toIntOrNull()?.takeIf { it >= MIN_GOAL }
    val sportValue = sport.toIntOrNull()?.takeIf { it >= MIN_SPORT_KCAL }
    ScreenScaffold("Pas et sport", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            Text(
                "Les pas et les séances de sport sont lus dans Health Connect, où le téléphone, Google Fit, Mi Fitness, " +
                    "Samsung Health ou Strava les enregistrent. Si rien ne s'affiche, activez la synchronisation vers " +
                    "Health Connect dans ces applis.",
                style = MaterialTheme.typography.bodyMedium,
            )
            SectionCard {
                NumberField(goal, { goal = it.filter(Char::isDigit) }, "Objectif de pas quotidien", Modifier.fillMaxWidth(), "pas", isError = goalValue == null)
                NumberField(
                    sport, { sport = it.filter(Char::isDigit) }, "Objectif sport quotidien", Modifier.fillMaxWidth(), "kcal",
                    isError = sportValue == null,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ajouter pas et séances au budget calorique", Modifier.weight(1f))
                    Switch(checked = inBudget, onCheckedChange = { inBudget = it })
                }
                Text(
                    "Votre niveau d'activité inclut déjà la marche quotidienne : seuls les pas au-delà de " +
                        "${fmt(STEPS_BASELINE.toDouble(), 0)} sont comptés (≈ 0,04 kcal par pas pour 90 kg). " +
                        "Les séances de marche et de course ne s'ajoutent pas (leurs pas comptent déjà). " +
                        "Ne notez pas à la main une séance déjà importée.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            OutlinedButton(
                // Absent on phones without Health Connect: ignore instead of crashing.
                onClick = { runCatching { context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Ouvrir les réglages Health Connect") }
            Button(
                onClick = { scope.launch { c.settings.setActivityGoals(goalValue ?: return@launch, sportValue ?: return@launch, inBudget); onBack() } },
                enabled = goalValue != null && sportValue != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }
    }
}
