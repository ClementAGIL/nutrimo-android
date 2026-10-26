// DAOs for foods, portions, recipes and the diary.
package com.clement.nutrimo.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.clement.nutrimo.domain.model.Meal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FoodDao {
    /** [pattern] is a LIKE pattern on the normalized text, e.g. "%pomme%". */
    @Query("SELECT * FROM foods WHERE searchText LIKE :pattern ORDER BY isFavorite DESC, useCount DESC LIMIT :limit")
    suspend fun search(pattern: String, limit: Int): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun byId(id: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun byIds(ids: List<String>): List<FoodEntity>

    @Query("SELECT * FROM foods WHERE barcode = :barcode LIMIT 1")
    suspend fun byBarcode(barcode: String): FoodEntity?

    @Query("SELECT * FROM foods WHERE lastUsedAt IS NOT NULL ORDER BY lastUsedAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE isFavorite = 1 ORDER BY name")
    fun observeFavorites(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE source = 'CUSTOM' ORDER BY name")
    fun observeCustom(): Flow<List<FoodEntity>>

    @Query("SELECT COUNT(*) FROM foods WHERE source = 'CIQUAL'")
    suspend fun ciqualCount(): Int

    /** Foods worth backing up: custom, used, starred, or referenced by a recipe or the diary. */
    @Query(
        "SELECT * FROM foods WHERE source = 'CUSTOM' OR lastUsedAt IS NOT NULL OR isFavorite = 1 " +
            "OR id IN (SELECT foodId FROM recipe_ingredients) OR id IN (SELECT foodId FROM diary)",
    )
    suspend fun personal(): List<FoodEntity>

    @Upsert
    suspend fun upsert(food: FoodEntity)

    @Upsert
    suspend fun upsertAll(foods: List<FoodEntity>)

    @Query("UPDATE foods SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE foods SET useCount = useCount + 1, lastUsedAt = :at WHERE id = :id")
    suspend fun markUsed(id: String, at: Long)

    @Query("DELETE FROM foods WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM portions WHERE foodId = :foodId ORDER BY grams")
    suspend fun portions(foodId: String): List<PortionEntity>

    @Query("SELECT * FROM portions")
    suspend fun allPortions(): List<PortionEntity>

    @Upsert
    suspend fun upsertPortions(portions: List<PortionEntity>)

    @Query("DELETE FROM portions WHERE foodId = :foodId")
    suspend fun deletePortions(foodId: String)
}

@Dao
interface RecipeDao {
    /** Favorites first, then the most logged. */
    @Query(
        "SELECT r.*, (SELECT COUNT(*) FROM diary d WHERE d.recipeId = r.id) AS useCount FROM recipes r " +
            "ORDER BY r.isFavorite DESC, useCount DESC, r.name",
    )
    fun observeWithUses(): Flow<List<RecipeWithUses>>

    @Query("SELECT * FROM recipes")
    suspend fun all(): List<RecipeEntity>

    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun byId(id: String): RecipeEntity?

    @Query("SELECT * FROM recipe_ingredients WHERE recipeId = :recipeId ORDER BY position")
    suspend fun ingredients(recipeId: String): List<IngredientEntity>

    @Query("SELECT * FROM recipe_ingredients")
    suspend fun allIngredients(): List<IngredientEntity>

    @Upsert
    suspend fun upsert(recipe: RecipeEntity)

    @Upsert
    suspend fun upsertAll(recipes: List<RecipeEntity>)

    @Upsert
    suspend fun upsertIngredients(ingredients: List<IngredientEntity>)

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: String)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT DISTINCT recipeId FROM recipe_ingredients WHERE subRecipeId = :id")
    suspend fun parentsOf(id: String): List<String>

    @Query("UPDATE recipes SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE recipes SET lastUsedAt = :at WHERE id = :id")
    suspend fun markUsed(id: String, at: Long)
}

@Dao
interface SavedMealDao {
    @Query("SELECT * FROM saved_meals ORDER BY name")
    fun observeAll(): Flow<List<SavedMealEntity>>

    @Query("SELECT * FROM saved_meals")
    suspend fun all(): List<SavedMealEntity>

    @Query("SELECT * FROM saved_meal_items WHERE mealId = :mealId ORDER BY position")
    suspend fun items(mealId: String): List<SavedMealItemEntity>

    @Query("SELECT * FROM saved_meal_items")
    suspend fun allItems(): List<SavedMealItemEntity>

    @Upsert
    suspend fun upsert(meal: SavedMealEntity)

    @Upsert
    suspend fun upsertAll(meals: List<SavedMealEntity>)

    @Upsert
    suspend fun upsertItems(items: List<SavedMealItemEntity>)

    @Query("DELETE FROM saved_meals WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary WHERE date = :date ORDER BY createdAt")
    fun observeDay(date: LocalDate): Flow<List<DiaryEntity>>

    @Query("SELECT * FROM diary WHERE date = :date AND meal = :meal ORDER BY createdAt")
    suspend fun meal(date: LocalDate, meal: Meal): List<DiaryEntity>

    @Query("SELECT * FROM diary WHERE id = :id")
    suspend fun byId(id: String): DiaryEntity?

    @Query(
        "SELECT date, SUM(kcal) AS kcal, SUM(protein) AS protein, SUM(carbs) AS carbs, SUM(fat) AS fat " +
            "FROM diary WHERE date BETWEEN :from AND :to GROUP BY date ORDER BY date",
    )
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>>

    @Query("SELECT COUNT(*) FROM diary WHERE date = :date")
    suspend fun countOn(date: LocalDate): Int

    @Query("SELECT * FROM diary")
    suspend fun all(): List<DiaryEntity>

    @Upsert
    suspend fun upsert(entry: DiaryEntity)

    @Upsert
    suspend fun upsertAll(entries: List<DiaryEntity>)

    @Query("DELETE FROM diary WHERE id = :id")
    suspend fun delete(id: String)
}
