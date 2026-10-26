// Demo journal: 30 days of varied meals built from bundled CIQUAL foods and the demo recipes.
package com.clement.nutrimo.data.demo

import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.Meal
import com.clement.nutrimo.domain.model.QuantityUnit
import com.clement.nutrimo.domain.model.UnitChoice
import java.time.LocalDate
import kotlin.random.Random

private const val DAYS = 30L
/** Some days are not logged at all, like in real life. */
private const val SKIP_PROBABILITY = 0.1
/** Quantities vary by ±15 % around the base amount. */
private const val QUANTITY_SPREAD = 0.15

/** CIQUAL ids used by the demo (all present in assets/ciqual_foods.json). */
object DemoFoods {
    const val MILK = "ciqual:19041"
    const val BANANA = "ciqual:13005"
    const val BAGUETTE = "ciqual:7001"
    const val BUTTER = "ciqual:16400"
    const val HAM = "ciqual:28900"
    const val CHICKEN = "ciqual:36005"
    const val RICE = "ciqual:9104"
    const val GREEN_BEANS = "ciqual:20030"
    const val APPLE = "ciqual:13191"
    const val YOGURT = "ciqual:19593"
    const val OLIVE_OIL = "ciqual:17270"
    const val SALMON = "ciqual:25996"
    const val BROCCOLI = "ciqual:20302"
    const val EMMENTAL = "ciqual:12115"
    const val FLOUR = "ciqual:9436"
    const val TOMATO = "ciqual:20047"
    const val ORANGE = "ciqual:13034"
    const val DARK_CHOCOLATE = "ciqual:31074"
    const val COTTAGE_CHEESE = "ciqual:19646"
    const val POTATO = "ciqual:4003"
    const val MUESLI = "ciqual:32004"
}

/** One item of a meal: a food in grams or a recipe in parts. */
private data class Item(val id: String, val amount: Double, val isRecipe: Boolean = false)

private fun food(id: String, grams: Double) = Item(id, grams)
private fun recipe(id: String, parts: Double) = Item(id, parts, isRecipe = true)

private val MENUS: Map<Meal, List<List<Item>>> = mapOf(
    Meal.BREAKFAST to listOf(
        listOf(food(DemoFoods.MUESLI, 50.0), food(DemoFoods.MILK, 200.0)),
        listOf(food(DemoFoods.BAGUETTE, 60.0), food(DemoFoods.BUTTER, 10.0), food(DemoFoods.ORANGE, 150.0)),
        listOf(food(DemoFoods.COTTAGE_CHEESE, 150.0), food(DemoFoods.BANANA, 120.0)),
        listOf(food(DEMO_GRANOLA_ID, 50.0), food(DemoFoods.YOGURT, 125.0)),
    ),
    Meal.LUNCH to listOf(
        listOf(recipe(DEMO_RICE_ID, 1.0), food(DemoFoods.APPLE, 150.0)),
        listOf(food(DemoFoods.CHICKEN, 150.0), food(DemoFoods.GREEN_BEANS, 200.0), food(DemoFoods.POTATO, 200.0)),
        listOf(food(DemoFoods.HAM, 80.0), food(DemoFoods.BAGUETTE, 80.0), food(DemoFoods.EMMENTAL, 30.0), food(DemoFoods.TOMATO, 150.0)),
    ),
    Meal.DINNER to listOf(
        listOf(food(DemoFoods.SALMON, 130.0), food(DemoFoods.BROCCOLI, 200.0), food(DemoFoods.RICE, 150.0)),
        listOf(food(DemoFoods.CHICKEN, 120.0), food(DemoFoods.POTATO, 250.0), food(DemoFoods.GREEN_BEANS, 150.0)),
        listOf(food(DemoFoods.TOMATO, 200.0), food(DemoFoods.OLIVE_OIL, 10.0), food(DemoFoods.EMMENTAL, 40.0), food(DemoFoods.BAGUETTE, 50.0)),
    ),
    Meal.SNACK to listOf(
        listOf(food(DemoFoods.APPLE, 150.0)),
        listOf(recipe(DEMO_CAKE_ID, 1.0)),
        listOf(food(DemoFoods.DARK_CHOCOLATE, 20.0), food(DemoFoods.ORANGE, 150.0)),
        listOf(food(DemoFoods.YOGURT, 125.0)),
    ),
)

class DemoDiary(private val c: AppContainer, private val random: Random, private val today: LocalDate) {
    private val cache = mutableMapOf<String, Loggable?>()

    suspend fun fill() {
        for (daysAgo in DAYS - 1 downTo 0) {
            if (daysAgo > 0 && random.nextDouble() < SKIP_PROBABILITY) continue
            val date = today.minusDays(daysAgo)
            MENUS.forEach { (meal, options) -> options.random(random).forEach { log(date, meal, it) } }
        }
    }

    private suspend fun log(date: LocalDate, meal: Meal, item: Item) {
        val loggable = cache.getOrPut(item.id) { if (item.isRecipe) c.recipes.loggable(item.id) else c.foods.loggable(item.id) } ?: return
        val unit = if (item.isRecipe) UnitChoice(QuantityUnit.PART, "part(s)") else UnitChoice(QuantityUnit.GRAM, "g")
        val amount = if (item.isRecipe) item.amount else Math.round(item.amount * random.nextDouble(1 - QUANTITY_SPREAD, 1 + QUANTITY_SPREAD)).toDouble()
        c.diary.log(date, meal, loggable, amount, unit)
    }
}
