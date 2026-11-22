// Full nutrient detail of a day: calories per meal, then the complete table with vitamins and minerals, each as a %
// of the daily reference for the profile's sex and age (DailyNeeds).
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.domain.calc.DailyNeeds
import com.clement.nutrimo.domain.calc.Energy
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.common.NutrientTable
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.ui.navigation.DayNutrientsRoute
import com.clement.nutrimo.util.day
import com.clement.nutrimo.util.kcal
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class DayNutrientsViewModel(c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val date: LocalDate = LocalDate.ofEpochDay(handle.toRoute<DayNutrientsRoute>().epochDay)
    val entries = c.diary.day(date).stateWhileSubscribed(viewModelScope, emptyList())
    /** Null without a profile: the table then shows plain amounts. */
    val needs = c.body.profile.map { p -> p?.let { DailyNeeds.of(it.sex, Energy.age(it.birthDate, date)) } }
        .stateWhileSubscribed(viewModelScope, null)
}

@Composable
fun DayNutrientsScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, handle -> DayNutrientsViewModel(c, handle) }
    val date = vm.date
    val entries by vm.entries.collectAsStateWithLifecycle()
    val needs by vm.needs.collectAsStateWithLifecycle()
    ScreenScaffold(date.day(), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            SectionCard(stringResource(R.string.day_nutrients_par_repas)) {
                Meal.entries.forEach { meal -> StatRow(stringResource(meal.label), kcal(entries.filter { it.meal == meal }.sumOf { it.nutrients.kcal })) }
            }
            NutrientTable(Nutrients.sum(entries.map { it.nutrients }), stringResource(R.string.day_nutrients_total_journee), needs)
        }
    }
}
