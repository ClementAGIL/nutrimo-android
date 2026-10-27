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
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.domain.calc.MacroGrams
import com.clement.nutrimo.domain.model.MicroGroup
import com.clement.nutrimo.domain.model.NutrientCatalog
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.ui.theme.NutrimoColors

/** Protein / carbs / fat bars against the reference grams of [targets]. */
@Composable
fun MacroBars(n: Nutrients, targets: MacroGrams?) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MacroBar("Protéines", n.protein, targets?.protein, NutrimoColors.protein)
        MacroBar("Glucides", n.carbs, targets?.carbs, NutrimoColors.carbs)
        MacroBar("Lipides", n.fat, targets?.fat, NutrimoColors.fat)
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
        StatRow("Énergie", kcal(n.kcal))
        StatRow("Matières grasses", grams(n.fat))
        Sub("dont acides gras saturés", grams(n.satFat))
        StatRow("Glucides", grams(n.carbs))
        Sub("dont sucres", grams(n.sugars))
        StatRow("Fibres", grams(n.fiber))
        StatRow("Protéines", grams(n.protein))
        StatRow("Sel", grams(n.salt))
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
    TextButton(onClick = { open = !open }) { Text(if (open) "Masquer vitamines et minéraux" else "Vitamines et minéraux") }
    if (!open) return
    MicroGroup.entries.forEach { group ->
        val items = NutrientCatalog.MICROS.filter { it.group == group && micros[it.key] != null }
        if (items.isEmpty()) return@forEach
        Text(group.label, style = MaterialTheme.typography.titleSmall)
        items.forEach { StatRow(it.label, "${fmt(micros.getValue(it.key), 2)} ${it.unit}") }
    }
}
