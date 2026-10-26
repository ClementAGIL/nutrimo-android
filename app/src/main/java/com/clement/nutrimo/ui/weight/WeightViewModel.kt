// State of the weight tab: goal progress, BMI, calorie target, the weigh-in list and the body measurements.
package com.clement.nutrimo.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.GoalEntity
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.data.db.WeightEntity
import com.clement.nutrimo.domain.calc.Bmi
import com.clement.nutrimo.domain.calc.CalorieTarget
import com.clement.nutrimo.domain.calc.GoalMath
import com.clement.nutrimo.domain.calc.GoalProgress
import com.clement.nutrimo.domain.calc.WeightPoint
import com.clement.nutrimo.domain.model.GoalStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class WeightUiState(
    val profile: ProfileEntity? = null,
    val goal: GoalEntity? = null,
    /** Oldest first. */
    val weights: List<WeightEntity> = emptyList(),
    val progress: GoalProgress? = null,
    val bmi: Double? = null,
    val target: CalorieTarget? = null,
) {
    val goalReached: Boolean get() = progress != null && progress.currentKg <= progress.targetKg
}

class WeightViewModel(private val c: AppContainer) : ViewModel() {
    val state: StateFlow<WeightUiState> = combine(
        c.body.profile, c.body.activeGoal, c.body.weightHistory, c.body.calorieTarget,
    ) { profile, goal, weights, target ->
        val current = weights.lastOrNull()?.kg
        WeightUiState(
            profile = profile,
            goal = goal,
            weights = weights,
            progress = goal?.let { progressOf(it, weights) },
            bmi = if (profile != null && current != null) Bmi.of(current, profile.heightCm) else null,
            target = target,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WeightUiState())

    /** Newest first. */
    val measurements: StateFlow<List<MeasurementEntity>> =
        c.body.measurementHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun existingOn(date: LocalDate): WeightEntity? = c.body.weightOn(date)

    fun save(date: LocalDate, kg: Double) = viewModelScope.launch { c.body.saveWeight(date, kg) }

    fun delete(id: String) = viewModelScope.launch { c.body.deleteWeight(id) }

    fun saveMeasurement(m: MeasurementEntity) = viewModelScope.launch { c.body.saveMeasurement(m) }

    fun deleteMeasurement(id: String) = viewModelScope.launch { c.body.deleteMeasurement(id) }

    fun markGoalAchieved() = viewModelScope.launch { c.body.closeActiveGoal(GoalStatus.ACHIEVED) }
}

/** Only the weigh-ins since the goal started count for its progress and trend. */
fun progressOf(goal: GoalEntity, weights: List<WeightEntity>): GoalProgress = GoalMath.progress(
    start = goal.startDate,
    end = goal.endDate,
    startKg = goal.startKg,
    targetKg = goal.targetKg,
    weights = weights.filter { !it.date.isBefore(goal.startDate) }.map { WeightPoint(it.date, it.kg) },
)
