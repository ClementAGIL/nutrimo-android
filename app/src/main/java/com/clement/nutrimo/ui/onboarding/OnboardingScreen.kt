// First launch: profile, then current weight + first goal. Saves everything and opens the journal.
package com.clement.nutrimo.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.goal.GoalDraft
import com.clement.nutrimo.ui.goal.GoalForm
import com.clement.nutrimo.ui.profile.ProfileDraft
import com.clement.nutrimo.ui.profile.ProfileForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingState(val step: Int = 0, val profile: ProfileDraft = ProfileDraft(), val goal: GoalDraft = GoalDraft())

class OnboardingViewModel(private val c: AppContainer) : ViewModel() {
    val state = MutableStateFlow(OnboardingState())

    fun setProfile(p: ProfileDraft) = state.update { it.copy(profile = p) }
    fun setGoal(g: GoalDraft) = state.update { it.copy(goal = g) }
    fun setStep(step: Int) = state.update { it.copy(step = step) }

    fun finish(onDone: () -> Unit) {
        val s = state.value
        val goal = s.goal.toEntity() ?: return
        viewModelScope.launch {
            c.body.saveProfile(s.profile.toEntity())
            c.body.saveWeight(goal.startDate, goal.startKg)
            c.body.startGoal(goal)
            c.settings.setOnboardingDone()
            onDone()
        }
    }
}

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val vm = appViewModel { c, _ -> OnboardingViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    // Back on the goal step returns to the profile instead of closing the app and losing the input.
    BackHandler(enabled = state.step == 1) { vm.setStep(0) }
    ScreenScaffold(title = if (state.step == 0) "Bienvenue sur Nutrimo" else "Votre objectif") { padding ->
        ScrollColumn(padding, spacing = 16.dp) {
            if (state.step == 0) {
                Text("Quelques informations pour estimer vos besoins caloriques.", style = MaterialTheme.typography.bodyLarge)
                ProfileForm(state.profile, vm::setProfile)
                WideButton("Continuer", { vm.setStep(1) }, enabled = state.profile.isValid)
            } else {
                GoalForm(state.goal, vm::setGoal, state.profile.toEntity())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { vm.setStep(0) }, modifier = Modifier.weight(1f)) { Text("Retour") }
                    Button(onClick = { vm.finish(onDone) }, enabled = state.goal.error == null, modifier = Modifier.weight(1f)) {
                        Text("Commencer")
                    }
                }
            }
        }
    }
}
