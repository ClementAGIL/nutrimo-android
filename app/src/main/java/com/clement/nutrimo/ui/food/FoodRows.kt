// List rows for search results: local foods, remote foods and recipes, with a source badge.
package com.clement.nutrimo.ui.food

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.R
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.network.FoodDto
import com.clement.nutrimo.util.fmt

@Composable
fun FoodRow(food: FoodEntity, onClick: () -> Unit) {
    ResultRow(food.name, food.brand, stringResource(R.string.food_rows_1_kcal_100_g, fmt(food.per100.kcal, 0)), food.source.label, food.isFavorite, onClick)
}

@Composable
fun RemoteFoodRow(food: FoodDto, onClick: () -> Unit) {
    ResultRow(food.name, food.brand, stringResource(R.string.food_rows_1_kcal_100_g, fmt(food.kcal, 0)), if (food.source == "ciqual") "CIQUAL" else "OFF", false, onClick)
}

@Composable
fun RecipeRow(recipe: RecipeEntity, onClick: () -> Unit) {
    ResultRow(recipe.name, null, stringResource(R.string.food_rows_1_parts, fmt(recipe.parts)), "Recette", recipe.isFavorite, onClick)
}

@Composable
private fun ResultRow(title: String, brand: String?, detail: String, badge: String, favorite: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title, maxLines = 2) },
        supportingContent = { Text(listOfNotNull(brand, detail).joinToString(" · ")) },
        leadingContent = if (favorite) ({ Icon(Icons.Default.Star, "Favori", tint = MaterialTheme.colorScheme.primary) }) else null,
        trailingContent = { AssistChip(onClick = onClick, label = { Text(badge, style = MaterialTheme.typography.labelSmall) }) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider(thickness = 0.5.dp)
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
