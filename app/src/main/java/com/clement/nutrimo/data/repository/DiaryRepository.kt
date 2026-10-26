// Food diary (entries with a nutrient snapshot) and daily water.
package com.clement.nutrimo.data.repository

import com.clement.nutrimo.data.db.DayTotal
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.WaterEntity
import com.clement.nutrimo.data.db.newId
import com.clement.nutrimo.data.db.now
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.UnitChoice
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class DiaryRepository(
    db: NutrimoDatabase,
    private val foods: FoodRepository,
    private val recipes: RecipeRepository,
) {
    private val dao = db.diaryDao()
    private val waterDao = db.waterDao()

    fun day(date: LocalDate): Flow<List<DiaryEntity>> = dao.observeDay(date)

    fun dailyTotals(from: LocalDate, to: LocalDate): Flow<List<DayTotal>> = dao.observeDailyTotals(from, to)

    fun water(date: LocalDate): Flow<WaterEntity?> = waterDao.observe(date)

    fun waterBetween(from: LocalDate, to: LocalDate): Flow<List<WaterEntity>> = waterDao.observeRange(from, to)

    suspend fun entry(id: String): DiaryEntity? = dao.byId(id)

    suspend fun log(date: LocalDate, meal: Meal, item: Loggable, quantity: Double, choice: UnitChoice) {
        dao.upsert(
            DiaryEntity(
                date = date,
                meal = meal,
                foodId = item.foodId,
                recipeId = item.recipeId,
                label = item.name,
                quantity = quantity,
                unit = choice.unit,
                portionLabel = choice.portion?.label,
                portionGrams = choice.portion?.grams,
                nutrients = item.nutrientsFor(quantity, choice),
            ),
        )
        item.foodId?.let { foods.markUsed(it) }
        item.recipeId?.let { recipes.markUsed(it) }
    }

    /** Rescales the snapshot instead of reloading the food, which may have changed or been deleted. */
    suspend fun update(entry: DiaryEntity, quantity: Double, meal: Meal) {
        val factor = if (entry.quantity > 0) quantity / entry.quantity else 1.0
        dao.upsert(entry.copy(quantity = quantity, meal = meal, nutrients = entry.nutrients.scaled(factor), updatedAt = now()))
    }

    suspend fun delete(id: String) = dao.delete(id)

    /** Copies the entries of [meal] from [from] to [to]; returns how many were copied. */
    suspend fun copyMeal(from: LocalDate, to: LocalDate, meal: Meal): Int {
        val source = dao.meal(from, meal)
        dao.upsertAll(source.map { it.copy(id = newId(), date = to, createdAt = now(), updatedAt = now()) })
        return source.size
    }

    suspend fun hasEntries(date: LocalDate): Boolean = dao.countOn(date) > 0

    suspend fun addWater(date: LocalDate, current: Int, deltaMl: Int) =
        waterDao.upsert(WaterEntity(date, (current + deltaMl).coerceAtLeast(0)))
}
