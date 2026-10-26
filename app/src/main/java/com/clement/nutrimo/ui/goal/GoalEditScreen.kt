// Start a new goal (the current one is archived), prefilled with the latest weigh-in.
package com.clement.nutrimo.ui.goal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.toInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalEditViewModel(private val c: AppContainer) : ViewModel() {
    val draft = MutableStateFlow(GoalDraft())
    val profile = c.body.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val weight = c.body.latestWeight()?.kg
            val previous = c.body.activeGoal.first()
            draft.value = GoalDraft(
                startKg = weight?.toInput() ?: "",
                targetKg = previous?.targetKg?.toInput() ?: "",
                preset = previous?.paceKgPerWeek?.takeIf { it in PACE_PRESETS } ?: 0.5,
            )
        }
    }

    fun save(onDone: () -> Unit) {
        val goal = draft.value.toEntity() ?: return
        viewModelScope.launch {
            if (c.body.weightOn(goal.startDate) == null) c.body.saveWeight(goal.startDate, goal.startKg)
            c.body.startGoal(goal)
            onDone()
        }
    }
}

@Composable
fun GoalEditScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> GoalEditViewModel(c) }
    val draft by vm.draft.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    ScreenScaffold("Nouvel objectif", onBack = onBack) { padding ->
        ScrollColumn(padding, spacing = 16.dp) {
            GoalForm(draft, { vm.draft.value = it }, profile)
            Button(onClick = { vm.save(onBack) }, enabled = draft.error == null, modifier = Modifier.fillMaxWidth()) {
                Text("Démarrer cet objectif")
            }
        }
    }
}
