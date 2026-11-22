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
    /** Copies the [meal] of [from] into [toMeal] of [to] (added to what is there); returns how many entries were copied. */
    suspend fun copyMeal(from: LocalDate, meal: Meal, to: LocalDate, toMeal: Meal): Int {
        val source = dao.meal(from, meal)
        val at = now()
        // One millisecond apart: the journal sorts by createdAt, so the copies keep the original order.
        dao.upsertAll(source.mapIndexed { i, e -> e.copy(id = newId(), date = to, meal = toMeal, createdAt = at + i, updatedAt = at) })
        return source.size
    }

    /** Moves the [meal] of [from] into [toMeal] of [to] (order kept); returns how many entries moved. */
    suspend fun moveMeal(from: LocalDate, meal: Meal, to: LocalDate, toMeal: Meal): Int {
        if (from == to && meal == toMeal) return 0
        val source = dao.meal(from, meal)
        val at = now()
        dao.upsertAll(source.map { it.copy(date = to, meal = toMeal, updatedAt = at) })
        return source.size
    }

    /**
     * Moves everything logged on [from] to [to], for a day logged on the wrong date: foods (each in its meal), water
     * (added to the target's), manual activities and the "Ma journée" note (merged into the target's, its mood kept
     * first). What [to] already has is kept. Health Connect data is not ours and stays where it is. Returns whether
     * anything moved.
     */
    suspend fun moveDay(from: LocalDate, to: LocalDate): Boolean {
        if (from == to) return false
        val at = now()
        return db.withTransaction {
            val foods = dao.onDate(from)
            dao.upsertAll(foods.map { it.copy(date = to, updatedAt = at) })
            val activityDao = db.activityDao()
            val activities = activityDao.onDate(from)
            activityDao.upsertAll(activities.map { it.copy(date = to, updatedAt = at) })
            val water = waterDao.get(from)?.ml ?: 0
            if (water > 0) {
                waterDao.upsert(WaterEntity(to, (waterDao.get(to)?.ml ?: 0) + water, at))
                waterDao.upsert(WaterEntity(from, 0, at))
            }
            val noteDao = db.dayNoteDao()
            val note = noteDao.get(from)?.takeIf { !it.isEmpty }
            if (note != null) {
                val target = noteDao.get(to)
                noteDao.upsert(
                    if (target == null) note.copy(date = to, updatedAt = at)
                    else target.copy(
                        mood = target.mood ?: note.mood,
                        gratitude = listOf(target.gratitude, note.gratitude).filter { it.isNotBlank() }.joinToString("\n\n"),
                        win = listOf(target.win, note.win).filter { it.isNotBlank() }.joinToString("\n\n"),
                        note = listOf(target.note, note.note).filter { it.isNotBlank() }.joinToString("\n\n"),
                        updatedAt = at,
                    ),
                )
                noteDao.delete(from)
            }
            foods.isNotEmpty() || activities.isNotEmpty() || water > 0 || note != null
        }
    }

    suspend fun hasEntries(date: LocalDate): Boolean = dao.countOn(date) > 0

    /** Adds [deltaMl] (negative to remove) to the stored amount, never below 0. */
    suspend fun addWater(date: LocalDate, deltaMl: Int) = db.withTransaction {
        val current = waterDao.get(date)?.ml ?: 0
        waterDao.upsert(WaterEntity(date, (current + deltaMl).coerceAtLeast(0)))
    }
}
