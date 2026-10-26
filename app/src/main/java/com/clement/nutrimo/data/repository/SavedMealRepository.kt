// Saved meals: save a journal meal under a name, then add all its items to any day in one tap.
package com.clement.nutrimo.data.repository

import androidx.room.withTransaction
import com.clement.nutrimo.data.db.DiaryEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.SavedMealEntity
import com.clement.nutrimo.data.db.SavedMealItemEntity
import com.clement.nutrimo.domain.model.Meal
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class SavedMealRepository(private val db: NutrimoDatabase) {
    private val dao = db.savedMealDao()
    private val diary = db.diaryDao()

    val meals: Flow<List<SavedMealEntity>> = dao.observeAll()

    suspend fun saveFrom(name: String, entries: List<DiaryEntity>) = db.withTransaction {
        val meal = SavedMealEntity(name = name.trim())
        dao.upsert(meal)
        dao.upsertItems(
            entries.mapIndexed { i, e ->
                SavedMealItemEntity(
                    mealId = meal.id, position = i, foodId = e.foodId, recipeId = e.recipeId, label = e.label,
                    quantity = e.quantity, unit = e.unit, portionLabel = e.portionLabel, portionGrams = e.portionGrams,
                    nutrients = e.nutrients,
                )
            },
        )
    }

    /** Adds every item of the saved meal to [date] / [meal]; returns how many were added. */
    suspend fun logInto(mealId: String, date: LocalDate, meal: Meal): Int {
        val items = dao.items(mealId)
        diary.upsertAll(
            items.map {
                DiaryEntity(
                    date = date, meal = meal, foodId = it.foodId, recipeId = it.recipeId, label = it.label,
                    quantity = it.quantity, unit = it.unit, portionLabel = it.portionLabel, portionGrams = it.portionGrams,
                    nutrients = it.nutrients,
                )
            },
        )
        return items.size
    }

    suspend fun kcalOf(mealId: String): Double = dao.items(mealId).sumOf { it.nutrients.kcal }

    suspend fun delete(id: String) = dao.delete(id)
}
