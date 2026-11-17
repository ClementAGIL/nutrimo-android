// Macro targets: presets or custom percentages, with the resulting grams and protein per kg.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.R
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.domain.calc.MACRO_PRESETS
import com.clement.nutrimo.domain.calc.MacroSplit
import com.clement.nutrimo.ui.common.ChoiceChips
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.appViewModel
import com.clement.nutrimo.ui.common.stateWhileSubscribed
import com.clement.nutrimo.util.fmt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MacrosViewModel(private val c: AppContainer) : ViewModel() {
    val target = c.body.calorieTarget.stateWhileSubscribed(viewModelScope, null)
    val latestKg = c.body.weightHistory.map { it.lastOrNull()?.kg }.stateWhileSubscribed(viewModelScope, null)

    suspend fun saved(): MacroSplit = c.settings.current().macroSplit

    /** In the ViewModel scope: going back right away doesn't cancel the save. */
    fun save(split: MacroSplit, onDone: () -> Unit) = viewModelScope.launch { c.settings.setMacroSplit(split); onDone() }
}

@Composable
fun MacrosScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> MacrosViewModel(c) }
    val target by vm.target.collectAsStateWithLifecycle()
    val latestKg by vm.latestKg.collectAsStateWithLifecycle()
    var split by remember { mutableStateOf(MacroSplit.BALANCED) }
    LaunchedEffect(Unit) { split = vm.saved() }
    ScreenScaffold(stringResource(R.string.macros_objectifs_macros), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(stringResource(R.string.macros_part_vos_calories_pour_chaque))
            ChoiceChips(MACRO_PRESETS, MACRO_PRESETS.find { it.second == split }, { stringResource(it.first) }) { split = it.second }
            PercentFields(split) { split = it }
            if (split.total != 100) WarningText(stringResource(R.string.macros_total_1_doit_faire_100, split.total))
            target?.let { t ->
                val g = split.grams(t.target)
                SectionCard(stringResource(R.string.macros_pour_1_kcal_jour, fmt(t.target, 0))) {
                    StatRow(stringResource(R.string.common_proteines), "${fmt(g.protein, 0)} g" + (latestKg?.let { " (${fmt(g.protein / it, 1)} g/kg)" } ?: ""))
                    StatRow(stringResource(R.string.common_glucides), "${fmt(g.carbs, 0)} g")
                    StatRow(stringResource(R.string.common_lipides), "${fmt(g.fat, 0)} g")
                }
            }
            Button(
                onClick = { vm.save(split, onBack) },
                enabled = split.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.common_enregistrer)) }
        }
    }
}

@Composable
private fun PercentFields(split: MacroSplit, onChange: (MacroSplit) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val pct = { text: String -> text.toIntOrNull() ?: 0 }
        NumberField(split.proteinPct.toString(), { onChange(split.copy(proteinPct = pct(it))) }, stringResource(R.string.common_proteines), Modifier.weight(1f), "%")
        NumberField(split.carbsPct.toString(), { onChange(split.copy(carbsPct = pct(it))) }, stringResource(R.string.common_glucides), Modifier.weight(1f), "%")
        NumberField(split.fatPct.toString(), { onChange(split.copy(fatPct = pct(it))) }, stringResource(R.string.common_lipides), Modifier.weight(1f), "%")
    }
}
