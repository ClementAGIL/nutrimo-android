// JSON export / import of all personal data (the bundled CIQUAL foods are not exported).
package com.clement.nutrimo.data.repository

import androidx.room.withTransaction
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.GoalEntity
import com.clement.nutrimo.data.db.IngredientEntity
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.PortionEntity
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.db.WeightEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Bump when the format changes; import refuses newer versions. */
const val BACKUP_VERSION = 1

@Serializable
data class Backup(
    val version: Int = BACKUP_VERSION,
    val exportedAt: Long,
    val profile: ProfileEntity? = null,
    val weights: List<WeightEntity> = emptyList(),
    val measurements: List<MeasurementEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val water: List<WaterEntity> = emptyList(),
    val foods: List<FoodEntity> = emptyList(),
    val portions: List<PortionEntity> = emptyList(),
    val recipes: List<RecipeEntity> = emptyList(),
    val ingredients: List<IngredientEntity> = emptyList(),
    val diary: List<DiaryEntity> = emptyList(),
)

data class BackupSummary(val weights: Int, val diary: Int, val recipes: Int, val foods: Int)

class BackupRepository(private val db: NutrimoDatabase, json: Json) {
    private val json = Json(json) { prettyPrint = false; encodeDefaults = true }

    suspend fun export(): String {
        val foods = db.foodDao().personal()
        val foodIds = foods.map { it.id }.toSet()
        val backup = Backup(
            exportedAt = System.currentTimeMillis(),
            profile = db.profileDao().get(),
            weights = db.weightDao().all(),
            measurements = db.measurementDao().all(),
            goals = db.goalDao().all(),
            water = db.waterDao().all(),
            foods = foods,
            portions = db.foodDao().allPortions().filter { it.foodId in foodIds },
            recipes = db.recipeDao().all(),
            ingredients = db.recipeDao().allIngredients(),
            diary = db.diaryDao().all(),
        )
        return json.encodeToString(backup)
    }

    /** Merges the backup into the database (rows with the same id are replaced). */
    suspend fun import(text: String): BackupSummary {
        val b = json.decodeFromString<Backup>(text)
        require(b.version <= BACKUP_VERSION) { "Sauvegarde créée par une version plus récente de Nutrimo" }
        db.withTransaction {
            b.profile?.let { db.profileDao().upsert(it) }
            db.weightDao().upsertAll(b.weights)
            db.measurementDao().upsertAll(b.measurements)
            db.goalDao().upsertAll(b.goals)
            db.waterDao().upsertAll(b.water)
            db.foodDao().upsertAll(b.foods)
            db.foodDao().upsertPortions(b.portions)
            db.recipeDao().upsertAll(b.recipes)
            db.recipeDao().upsertIngredients(b.ingredients)
            db.diaryDao().upsertAll(b.diary)
        }
        return BackupSummary(b.weights.size, b.diary.size, b.recipes.size, b.foods.size)
    }
}
