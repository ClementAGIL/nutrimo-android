// Full nutrient detail of a day: calories per meal, then the complete table with vitamins and minerals.
package com.clement.nutrimo.ui.journal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.common.NutrientTable
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.day
import com.clement.nutrimo.ui.common.kcal
import java.time.LocalDate

@Composable
fun DayNutrientsScreen(epochDay: Long, onBack: () -> Unit) {
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    val diary = appContainer().diary
    val entries by remember(date) { diary.day(date) }.collectAsStateWithLifecycle(initialValue = emptyList())
    ScreenScaffold(date.day(), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Par repas") {
                Meal.entries.forEach { meal -> StatRow(meal.label, kcal(entries.filter { it.meal == meal }.sumOf { it.nutrients.kcal })) }
            }
            NutrientTable(Nutrients.sum(entries.map { it.nutrients }), "Total de la journée")
        }
    }
}
