// Recipes: storage, nutrition (recursive through sub-recipes) and cycle protection.
package com.clement.nutrimo.data.repository

import androidx.room.withTransaction
import com.clement.nutrimo.data.db.IngredientEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.RecipeEntity
import com.clement.nutrimo.data.db.RecipeWithUses
import com.clement.nutrimo.data.db.now
import com.clement.nutrimo.domain.calc.IngredientContribution
import com.clement.nutrimo.domain.calc.RecipeMath
import com.clement.nutrimo.domain.calc.RecipeTotals
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.normalized
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class IngredientLine(
    val ingredient: IngredientEntity,
    val name: String,
    /** Null when the food or sub-recipe was deleted. */
    val contribution: IngredientContribution?,
)

data class RecipeDetail(val recipe: RecipeEntity, val lines: List<IngredientLine>, val totals: RecipeTotals)

class RecipeRepository(private val db: NutrimoDatabase) {
    private val dao = db.recipeDao()
    private val foods = db.foodDao()

    val recipesWithUses: Flow<List<RecipeWithUses>> = dao.observeWithUses()
    val recipes: Flow<List<RecipeEntity>> = recipesWithUses.map { list -> list.map { it.recipe } }

    suspend fun recipe(id: String): RecipeEntity? = dao.byId(id)

    suspend fun detail(id: String): RecipeDetail? {
        val recipe = dao.byId(id) ?: return null
        val lines = dao.ingredients(id).map { line(it, setOf(id)) }
        return RecipeDetail(recipe, lines, RecipeMath.totals(lines.mapNotNull { it.contribution }, recipe.parts, recipe.cookedWeightG))
    }

    /** Name and contribution of one ingredient; [visiting] guards against cycles. */
    suspend fun line(ingredient: IngredientEntity, visiting: Set<String> = emptySet()): IngredientLine {
        ingredient.foodId?.let { foodId ->
            val food = foods.byId(foodId) ?: return IngredientLine(ingredient, "Aliment supprimé", null)
            return IngredientLine(ingredient, food.name, RecipeMath.ofFood(food.per100, ingredient.quantity))
        }
        val subId = ingredient.subRecipeId ?: return IngredientLine(ingredient, "?", null)
        val sub = dao.byId(subId) ?: return IngredientLine(ingredient, "Recette supprimée", null)
        val totals = totals(subId, visiting)
        return IngredientLine(ingredient, sub.name, totals?.let { RecipeMath.ofSubRecipe(it, ingredient.quantity) })
    }

    suspend fun totals(id: String, visiting: Set<String> = emptySet()): RecipeTotals? {
        if (id in visiting) return null
        val recipe = dao.byId(id) ?: return null
        val items = dao.ingredients(id).mapNotNull { line(it, visiting + id).contribution }
        return RecipeMath.totals(items, recipe.parts, recipe.cookedWeightG)
    }

    suspend fun loggable(id: String): Loggable? {
        val recipe = dao.byId(id) ?: return null
        val totals = totals(id) ?: return null
        return Loggable(
            recipeId = id,
            name = recipe.name,
            subtitle = "Recette · ${formatParts(recipe.parts)} parts",
            per100 = totals.per100,
            perPart = totals.perPart,
        )
    }

    /** False when [candidateId] is [recipeId] itself or already contains it (directly or deeper). */
    suspend fun canUseAsSub(recipeId: String, candidateId: String): Boolean {
        if (candidateId == recipeId) return false
        val pending = ArrayDeque(listOf(candidateId))
        val seen = mutableSetOf<String>()
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            if (!seen.add(id)) continue
            val subs = dao.ingredients(id).mapNotNull { it.subRecipeId }
            if (recipeId in subs) return false
            pending.addAll(subs)
        }
        return true
    }

    suspend fun search(query: String): List<RecipeEntity> {
        val q = query.normalized()
        return dao.all().filter { it.name.normalized().contains(q) }
    }

    suspend fun save(recipe: RecipeEntity, ingredients: List<IngredientEntity>) = db.withTransaction {
        dao.upsert(recipe.copy(updatedAt = now()))
        dao.deleteIngredients(recipe.id)
        dao.upsertIngredients(ingredients.mapIndexed { i, it -> it.copy(recipeId = recipe.id, position = i) })
    }

    suspend fun delete(id: String) = dao.delete(id)

    /** Recipes using [id] as a sub-recipe (deleting it would break them). */
    suspend fun parentsOf(id: String): List<String> = dao.parentsOf(id).mapNotNull { dao.byId(it)?.name }

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun markUsed(id: String) = dao.markUsed(id, now())
}

fun formatParts(parts: Double): String = if (parts % 1.0 == 0.0) parts.toInt().toString() else "%.1f".format(parts)
