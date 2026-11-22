// Food diary (entries with a nutrient snapshot) and daily water.
package com.clement.nutrimo.data.repository

import androidx.room.withTransaction
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
    private val db: NutrimoDatabase,
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
        val at = now()
        // One millisecond apart: the journal sorts by createdAt, so the copies keep the original order.
        dao.upsertAll(source.mapIndexed { i, e -> e.copy(id = newId(), date = to, createdAt = at + i, updatedAt = at) })
        return source.size
    }

    /** Moves the [meal] of [from] to [to] (same meal, order kept); returns how many entries moved. */
    suspend fun moveMeal(from: LocalDate, to: LocalDate, meal: Meal): Int {
        if (from == to) return 0
        val source = dao.meal(from, meal)
        val at = now()
        dao.upsertAll(source.map { it.copy(date = to, updatedAt = at) })
        return source.size
    }

    suspend fun hasEntries(date: LocalDate): Boolean = dao.countOn(date) > 0

    /** Adds [deltaMl] (negative to remove) to the stored amount, never below 0. */
    suspend fun addWater(date: LocalDate, deltaMl: Int) = db.withTransaction {
        val current = waterDao.get(date)?.ml ?: 0
        waterDao.upsert(WaterEntity(date, (current + deltaMl).coerceAtLeast(0)))
    }
}
