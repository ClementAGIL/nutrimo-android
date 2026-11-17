// Nutrient displays shared by the item detail, day detail and recipe screens.
package com.clement.nutrimo.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.domain.calc.MacroGrams
import com.clement.nutrimo.domain.model.MicroGroup
import com.clement.nutrimo.domain.model.NutrientCatalog
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.theme.NutrimoColors
import com.clement.nutrimo.util.fmt
import com.clement.nutrimo.util.grams
import com.clement.nutrimo.util.kcal

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

/** Full label-style table: energy, macros with their "dont" lines, fiber, salt, then the micros on demand. */
@Composable
fun NutrientTable(n: Nutrients, title: String? = null) {
    SectionCard(title) {
        StatRow(stringResource(R.string.nutrient_energie), kcal(n.kcal))
        StatRow(stringResource(R.string.nutrient_matieres_grasses), grams(n.fat))
        Sub(stringResource(R.string.nutrient_dont_acides_gras_satures), grams(n.satFat))
        StatRow(stringResource(R.string.common_glucides), grams(n.carbs))
        Sub(stringResource(R.string.nutrient_dont_sucres), grams(n.sugars))
        StatRow(stringResource(R.string.nutrient_fibres), grams(n.fiber))
        StatRow(stringResource(R.string.common_proteines), grams(n.protein))
        StatRow(stringResource(R.string.nutrient_sel), grams(n.salt))
        if (n.micros.isNotEmpty()) MicroSection(n.micros)
    }
}

@Composable
private fun Sub(label: String, value: String) {
    StatRow("   $label", value)
}

@Composable
private fun MicroSection(micros: Map<String, Double>) {
    var open by rememberSaveable { mutableStateOf(false) }
    TextButton(onClick = { open = !open }) { Text(if (open) stringResource(R.string.common_masquer_vitamines_mineraux) else stringResource(R.string.nutrient_vitamines_mineraux)) }
    if (!open) return
    MicroGroup.entries.forEach { group ->
        val items = NutrientCatalog.MICROS.filter { it.group == group && micros[it.key] != null }
        if (items.isEmpty()) return@forEach
        Text(stringResource(group.label), style = MaterialTheme.typography.titleSmall)
        items.forEach { StatRow(stringResource(it.label), "${fmt(micros.getValue(it.key), 2)} ${it.unit}") }
    }
}
