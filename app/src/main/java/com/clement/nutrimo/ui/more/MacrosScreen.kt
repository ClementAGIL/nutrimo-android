// Macro targets: presets or custom percentages, with the resulting grams and protein per kg.
package com.clement.nutrimo.ui.more

import com.clement.nutrimo.ui.common.IntroText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.domain.calc.MACRO_PRESETS
import com.clement.nutrimo.domain.calc.MacroSplit
import com.clement.nutrimo.ui.common.NumberField
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard
import com.clement.nutrimo.ui.common.StatRow
import com.clement.nutrimo.ui.common.WarningText
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.fmt
import kotlinx.coroutines.launch

@Composable
fun MacrosScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val target by c.body.calorieTarget.collectAsStateWithLifecycle(initialValue = null)
    val weights by c.body.weightHistory.collectAsStateWithLifecycle(initialValue = emptyList())
    var split by remember { mutableStateOf(MacroSplit.BALANCED) }
    LaunchedEffect(Unit) { split = c.settings.current().macroSplit }
    ScreenScaffold("Objectifs de macros", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText("Part de vos calories pour chaque macronutriment.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MACRO_PRESETS.forEach { (label, preset) ->
                    FilterChip(selected = split == preset, onClick = { split = preset }, label = { Text(label) })
                }
            }
            PercentFields(split) { split = it }
            if (split.total != 100) WarningText("Total : ${split.total} % (doit faire 100 %)")
            target?.let { t ->
                val g = split.grams(t.target)
                SectionCard("Pour ${fmt(t.target, 0)} kcal / jour") {
                    StatRow("Protéines", "${fmt(g.protein, 0)} g" + (weights.lastOrNull()?.let { " (${fmt(g.protein / it.kg, 1)} g/kg)" } ?: ""))
                    StatRow("Glucides", "${fmt(g.carbs, 0)} g")
                    StatRow("Lipides", "${fmt(g.fat, 0)} g")
                }
            }
            Button(
                onClick = { scope.launch { c.settings.setMacroSplit(split); onBack() } },
                enabled = split.isValid,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enregistrer") }
        }
    }
}

@Composable
private fun PercentFields(split: MacroSplit, onChange: (MacroSplit) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val pct = { text: String -> text.toIntOrNull() ?: 0 }
        NumberField(split.proteinPct.toString(), { onChange(split.copy(proteinPct = pct(it))) }, "Protéines", Modifier.weight(1f), "%")
        NumberField(split.carbsPct.toString(), { onChange(split.copy(carbsPct = pct(it))) }, "Glucides", Modifier.weight(1f), "%")
        NumberField(split.fatPct.toString(), { onChange(split.copy(fatPct = pct(it))) }, "Lipides", Modifier.weight(1f), "%")
    }
}
