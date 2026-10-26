// Developer tool: replaces all personal data with a realistic demo history (profile, 2 goals, weigh-ins,
// measurements, water, custom food, recipes with a sub-recipe, 30 days of meals) so every screen has content.
package com.clement.nutrimo.data.demo

import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.GoalEntity
import com.clement.nutrimo.data.db.IngredientEntity
import com.clement.nutrimo.data.db.MeasurementEntity
import com.clement.nutrimo.data.db.PortionEntity
import com.clement.nutrimo.data.db.ProfileEntity
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.network.searchTextOf
import com.clement.nutrimo.domain.calc.GoalMath
import com.clement.nutrimo.domain.model.ActivityLevel
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.GoalMode
import com.clement.nutrimo.domain.model.GoalStatus
import com.clement.nutrimo.domain.model.Nutrients
import com.clement.nutrimo.domain.model.Sex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

/** Same seed → same demo every time. */
private const val SEED = 42
private const val PREVIOUS_GOAL_START_DAYS = 150L
private const val GOAL_START_DAYS = 60L
private const val START_KG = 95.0
private const val TARGET_KG = 82.0
private const val PACE = 0.5
private const val LOSS_PER_DAY = 0.075
private const val WEIGH_IN_PROBABILITY = 0.6
private const val WATER_DAYS = 30L

const val DEMO_GRANOLA_ID = "custom:demo-granola"
const val DEMO_SAUCE_ID = "demo-sauce-tomate"
const val DEMO_RICE_ID = "demo-riz-poulet"
const val DEMO_CAKE_ID = "demo-gateau"

class DemoData(private val c: AppContainer) {
    private val random = Random(SEED)
    private val today = LocalDate.now()

    /** Wipes the database, restores CIQUAL, then writes the demo history. */
    suspend fun load() {
        withContext(Dispatchers.IO) { c.db.clearAllTables() }
        c.foods.seedCiqualIfNeeded()
        c.body.saveProfile(ProfileEntity(sex = Sex.MALE, birthDate = LocalDate.of(1990, 5, 14), heightCm = 178.0, activity = ActivityLevel.LIGHT))
        goals()
        weights()
        measurements()
        water()
        customFood()
        recipes()
        DemoDiary(c, random, today).fill()
        listOf(DemoFoods.BANANA, DemoFoods.YOGURT).forEach { c.foods.setFavorite(it, true) }
        c.recipes.setFavorite(DEMO_CAKE_ID, true)
        c.settings.setOnboardingDone()
    }

    /** Wipes everything; the app goes back to onboarding. */
    suspend fun clear() {
        withContext(Dispatchers.IO) { c.db.clearAllTables() }
        c.foods.seedCiqualIfNeeded()
    }

    private suspend fun goals() {
        val previousStart = today.minusDays(PREVIOUS_GOAL_START_DAYS)
        c.body.startGoal(
            GoalEntity(
                startDate = previousStart, startKg = 99.0, targetKg = START_KG, mode = GoalMode.PACE, paceKgPerWeek = 0.5,
                endDate = GoalMath.endDateForPace(previousStart, 99.0, START_KG, 0.5),
            ),
        )
        c.body.closeActiveGoal(GoalStatus.ACHIEVED)
        val start = today.minusDays(GOAL_START_DAYS)
        c.body.startGoal(
            GoalEntity(
                startDate = start, startKg = START_KG, targetKg = TARGET_KG, mode = GoalMode.PACE, paceKgPerWeek = PACE,
                endDate = GoalMath.endDateForPace(start, START_KG, TARGET_KG, PACE),
            ),
        )
    }

    /** A noisy downward line through both goals, about 0.5 kg a week, with a weigh-in every other day or so. */
    private suspend fun weights() {
        for (daysAgo in PREVIOUS_GOAL_START_DAYS downTo 0) {
            if (daysAgo != 0L && daysAgo != GOAL_START_DAYS && random.nextDouble() > WEIGH_IN_PROBABILITY) continue
            val kg = 99.0 - (PREVIOUS_GOAL_START_DAYS - daysAgo) * (99.0 - START_KG) / (PREVIOUS_GOAL_START_DAYS - GOAL_START_DAYS)
            val trend = if (daysAgo >= GOAL_START_DAYS) kg else START_KG - (GOAL_START_DAYS - daysAgo) * LOSS_PER_DAY
            val noise = if (daysAgo == GOAL_START_DAYS) 0.0 else random.nextDouble(-0.4, 0.4)
            c.body.saveWeight(today.minusDays(daysAgo), Math.round((trend + noise) * 10) / 10.0)
        }
    }

    private suspend fun measurements() {
        for (i in 0..4) {
            c.body.saveMeasurement(
                MeasurementEntity(
                    date = today.minusDays(GOAL_START_DAYS - i * 14L),
                    waist = 104.0 - i * 1.2, hips = 108.0 - i * 0.8, chest = 106.0 - i * 0.7, arm = 34.0 - i * 0.2, thigh = 60.0 - i * 0.4,
                ),
            )
        }
    }

    private suspend fun water() {
        for (daysAgo in 0 until WATER_DAYS) c.diary.addWater(today.minusDays(daysAgo), 0, 250 * random.nextInt(5, 11))
    }

    private suspend fun customFood() {
        val per100 = Nutrients(kcal = 452.0, fat = 18.0, satFat = 3.1, carbs = 58.0, sugars = 17.0, fiber = 8.5, protein = 11.0, salt = 0.1)
        val granola = FoodEntity(
            id = DEMO_GRANOLA_ID, source = FoodSource.CUSTOM, name = "Granola maison", brand = null,
            per100 = per100, searchText = searchTextOf("Granola maison", null),
        )
        c.foods.saveCustom(granola, listOf(PortionEntity(foodId = DEMO_GRANOLA_ID, label = "1 bol", grams = 50.0)))
    }

    private suspend fun recipes() {
        recipe(DEMO_SAUCE_ID, "Sauce tomate maison", parts = 4.0, cooked = 320.0, DemoFoods.TOMATO to 400.0, DemoFoods.OLIVE_OIL to 20.0)
        recipe(DEMO_RICE_ID, "Riz au poulet, sauce tomate", parts = 4.0, cooked = null, DemoFoods.RICE to 600.0, DemoFoods.CHICKEN to 400.0)
        // Sub-recipe: 4 parts of the sauce, added after the foods.
        val rice = c.recipes.recipe(DEMO_RICE_ID) ?: return
        val lines = c.recipes.detail(DEMO_RICE_ID)?.lines?.map { it.ingredient }.orEmpty()
        c.recipes.save(rice, lines + IngredientEntity(recipeId = DEMO_RICE_ID, position = lines.size, subRecipeId = DEMO_SAUCE_ID, quantity = 4.0))
        recipe(
            DEMO_CAKE_ID, "Gâteau au yaourt et chocolat", parts = 7.0, cooked = 620.0,
            DemoFoods.YOGURT to 125.0, DemoFoods.FLOUR to 250.0, DemoFoods.BUTTER to 60.0, DemoFoods.DARK_CHOCOLATE to 100.0, DemoFoods.MILK to 100.0,
        )
    }

    private suspend fun recipe(id: String, name: String, parts: Double, cooked: Double?, vararg foods: Pair<String, Double>) {
        val ingredients = foods.mapIndexed { i, (foodId, grams) -> IngredientEntity(recipeId = id, position = i, foodId = foodId, quantity = grams) }
        c.recipes.save(RecipeEntity(id = id, name = name, parts = parts, cookedWeightG = cooked), ingredients)
    }
}
