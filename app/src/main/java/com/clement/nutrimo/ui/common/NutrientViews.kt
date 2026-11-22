// Nutrient displays shared by the item detail, day detail and recipe screens.
package com.clement.nutrimo.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.domain.calc.DailyNeeds
import com.clement.nutrimo.domain.calc.DailyReference
import com.clement.nutrimo.domain.calc.MacroGrams
import com.clement.nutrimo.domain.model.MicroGroup
import com.clement.nutrimo.domain.model.NutrientCatalog
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.grams
import com.clement.nutrimo.util.kcal
import com.clement.nutrimo.util.percent

/** Protein / carbs / fat bars against the reference grams of [targets]. */
@Composable
fun MacroBars(n: Nutrients, targets: MacroGrams?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroBar(stringResource(R.string.common_proteines), n.protein, targets?.protein, NutrimoColors.protein)
        MacroBar(stringResource(R.string.common_glucides), n.carbs, targets?.carbs, NutrimoColors.carbs)
        MacroBar(stringResource(R.string.common_lipides), n.fat, targets?.fat, NutrimoColors.fat)
    }
}

@Composable
private fun MacroBar(label: String, value: Double?, target: Double?, color: androidx.compose.ui.graphics.Color) {
    val v = value ?: 0.0
    if (target == null || target <= 0) return StatRow(label, grams(value))
    ProgressRow(label, "${fmt(v, 0)} / ${fmt(target, 0)} g", v / target, color)
}

/**
 * Full label-style table: energy, macros with their "dont" lines, fiber, salt, then the micros on demand. With [needs]
 * (a whole day), each nutrient with a reference also shows its % of the daily reference and a bar.
 */
@Composable
fun NutrientTable(n: Nutrients, title: String? = null, needs: Map<String, DailyReference>? = null) {
    SectionCard(title) {
        if (needs != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HintText(stringResource(R.string.nutrient_pourcentages_reperes), Modifier.weight(1f))
                InfoButton(stringResource(R.string.nutrient_reperes_journaliers), stringResource(R.string.nutrient_reperes_aide))
            }
        }
        StatRow(stringResource(R.string.nutrient_energie), kcal(n.kcal))
        StatRow(stringResource(R.string.nutrient_matieres_grasses), grams(n.fat))
        NutrientRow("   " + stringResource(R.string.nutrient_dont_acides_gras_satures), n.satFat, "g", 1, needs?.get(DailyNeeds.SAT_FAT))
        StatRow(stringResource(R.string.common_glucides), grams(n.carbs))
        NutrientRow("   " + stringResource(R.string.nutrient_dont_sucres), n.sugars, "g", 1, needs?.get(DailyNeeds.SUGARS))
        NutrientRow(stringResource(R.string.nutrient_fibres), n.fiber, "g", 1, needs?.get(DailyNeeds.FIBER))
        StatRow(stringResource(R.string.common_proteines), grams(n.protein))
        NutrientRow(stringResource(R.string.nutrient_sel), n.salt, "g", 1, needs?.get(DailyNeeds.SALT))
        if (n.micros.isNotEmpty()) MicroSection(n.micros, needs)
    }
}

/** "21,4 g", or with a [reference] "21,4 g · 71 %" and a bar: green once a need is met, orange past a limit. */
@Composable
private fun NutrientRow(label: String, value: Double?, unit: String, decimals: Int, reference: DailyReference?) {
    val text = value?.let { "${fmt(it, decimals)} $unit" } ?: "–"
    if (reference == null || value == null) return StatRow(label, text)
    val ratio = reference.ratio(value)
    val color = when {
        reference.limit -> if (ratio > 1) NutrimoColors.warning else MaterialTheme.colorScheme.primary
        ratio >= 1 -> NutrimoColors.weight
        else -> MaterialTheme.colorScheme.primary
    }
    ProgressRow(label, "$text · ${percent(ratio)}", ratio, color)
}

@Composable
private fun MicroSection(micros: Map<String, Double>, needs: Map<String, DailyReference>?) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) stringResource(R.string.common_masquer_vitamines_mineraux) else stringResource(R.string.nutrient_vitamines_mineraux)) }
    if (!open) return
    MicroGroup.entries.forEach { group ->
        val items = NutrientCatalog.MICROS.filter { it.group == group && micros[it.key] != null }
        if (items.isEmpty()) return@forEach
        Text(stringResource(group.label), style = MaterialTheme.typography.titleSmall)
        items.forEach { NutrientRow(stringResource(it.label), micros.getValue(it.key), it.unit, 2, needs?.get(it.key)) }
    }
}
