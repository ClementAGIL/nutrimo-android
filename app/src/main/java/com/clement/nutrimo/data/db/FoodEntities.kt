// Entities about food: foods (CIQUAL seed, cached OFF products, custom foods), portions, recipes, diary.
@file:UseSerializers(LocalDateSerializer::class)

package com.clement.nutrimo.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.domain.model.QuantityUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

/** Ids: `ciqual:<code>`, `off:<barcode>`, `custom:<uuid>`. Nutrients are per 100 g. */
@Serializable
@Entity(tableName = "foods", indices = [Index("barcode"), Index("searchText"), Index("lastUsedAt")])
data class FoodEntity(
    @PrimaryKey val id: String,
    val source: FoodSource,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    @Embedded val per100: Nutrients,
    /** Lowercase name + brand without accents, for LIKE search. */
    val searchText: String,
    val isFavorite: Boolean = false,
    val useCount: Int = 0,
    val lastUsedAt: Long? = null,
    val updatedAt: Long = now(),
)

@Serializable
@Entity(
    tableName = "portions",
    indices = [Index("foodId")],
    foreignKeys = [ForeignKey(FoodEntity::class, ["id"], ["foodId"], onDelete = ForeignKey.CASCADE)],
)
data class PortionEntity(
    @PrimaryKey val id: String = newId(),
    val foodId: String,
    val label: String,
    val grams: Double,
)

@Serializable
@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String = newId(),
    val name: String,
    val parts: Double,
    /** Weight after cooking; when null the raw ingredient weight is used for per-100 g values. */
    val cookedWeightG: Double? = null,
    val isFavorite: Boolean = false,
    val lastUsedAt: Long? = null,
    val updatedAt: Long = now(),
)

/** Either a food ([quantity] in grams) or a sub-recipe ([quantity] in parts). */
@Serializable
@Entity(
    tableName = "recipe_ingredients",
    indices = [Index("recipeId")],
    foreignKeys = [ForeignKey(RecipeEntity::class, ["id"], ["recipeId"], onDelete = ForeignKey.CASCADE)],
)
data class IngredientEntity(
    @PrimaryKey val id: String = newId(),
    val recipeId: String,
    val position: Int,
    val foodId: String? = null,
    val subRecipeId: String? = null,
    val quantity: Double,
)

/** [nutrients] is a snapshot of what was eaten, so editing a food or recipe later keeps past days unchanged. */
@Serializable
@Entity(tableName = "diary", indices = [Index("date")])
data class DiaryEntity(
    @PrimaryKey val id: String = newId(),
    val date: LocalDate,
    val meal: Meal,
    val foodId: String? = null,
    val recipeId: String? = null,
    val label: String,
    val quantity: Double,
    val unit: QuantityUnit,
    /** Name of the portion when [unit] is PORTION (e.g. "1 pot"), its weight in [portionGrams]. */
    val portionLabel: String? = null,
    val portionGrams: Double? = null,
    @Embedded val nutrients: Nutrients,
    val createdAt: Long = now(),
    val updatedAt: Long = now(),
)
