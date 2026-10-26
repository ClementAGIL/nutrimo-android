// Foods: bundled CIQUAL seed, local search, remote search (PC backend, then Open Food Facts), barcode, custom foods.
package com.clement.nutrimo.data.repository

import android.content.Context
import com.clement.nutrimo.data.db.FoodEntity
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.db.PortionEntity
import com.clement.nutrimo.data.db.now
import com.clement.nutrimo.data.network.FoodDto
import com.clement.nutrimo.data.network.Http
import com.clement.nutrimo.data.network.NutrimoApi
import com.clement.nutrimo.data.network.OffApi
import com.clement.nutrimo.data.network.servingPortion
import com.clement.nutrimo.data.network.toEntity
import com.clement.nutrimo.domain.model.FoodSource
import com.clement.nutrimo.domain.model.Loggable
import com.clement.nutrimo.domain.model.Portion
import com.clement.nutrimo.domain.model.normalized
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

private const val CIQUAL_ASSET = "ciqual_foods.json"
private const val LOCAL_CANDIDATES = 400
private const val LOCAL_RESULTS = 50
private const val RECENT_COUNT = 30

enum class RemoteSource(val label: String) { BACKEND("Serveur Nutrimo"), OFF("Open Food Facts") }

data class RemoteResult(val items: List<FoodDto>, val source: RemoteSource?, val error: String? = null)

class FoodRepository(
    private val context: Context,
    db: NutrimoDatabase,
    private val http: Http,
    private val settings: SettingsRepository,
) {
    private val dao = db.foodDao()
    private val api = NutrimoApi(http)
    private val off = OffApi(http)

    val recent: Flow<List<FoodEntity>> = dao.observeRecent(RECENT_COUNT)
    val favorites: Flow<List<FoodEntity>> = dao.observeFavorites()
    val custom: Flow<List<FoodEntity>> = dao.observeCustom()

    suspend fun seedCiqualIfNeeded() = withContext(Dispatchers.IO) {
        if (dao.ciqualCount() > 0) return@withContext
        val text = context.assets.open(CIQUAL_ASSET).bufferedReader().use { it.readText() }
        dao.upsertAll(http.json.decodeFromString<List<FoodDto>>(text).map { it.toEntity() })
    }

    /** Every word must appear; names starting with the query, favorites and often used foods come first. */
    suspend fun searchLocal(query: String): List<FoodEntity> {
        val q = query.normalized()
        val words = q.split(' ').filter { it.isNotBlank() }
        if (words.isEmpty()) return emptyList()
        return dao.search("%${words.maxBy { it.length }}%", LOCAL_CANDIDATES)
            .filter { food -> words.all { food.searchText.contains(it) } }
            .sortedWith(
                compareByDescending<FoodEntity> { it.searchText.startsWith(q) }
                    .thenByDescending { it.isFavorite }
                    .thenByDescending { it.useCount }
                    .thenBy { it.source != FoodSource.CIQUAL }
                    .thenBy { it.name.length },
            )
            .take(LOCAL_RESULTS)
    }

    /** PC backend first; Open Food Facts only when [allowOff] (explicit submit), to respect its rate limit. */
    suspend fun searchRemote(query: String, allowOff: Boolean): RemoteResult {
        val s = settings.current()
        if (s.serverUrl.isNotBlank()) {
            attempt { api.search(s.serverUrl, s.apiKey, query) }?.let { return RemoteResult(it, RemoteSource.BACKEND) }
        }
        if (!allowOff) return RemoteResult(emptyList(), null, "Serveur injoignable : validez pour chercher sur Open Food Facts")
        return attempt { off.search(query) }?.let { RemoteResult(it, RemoteSource.OFF) }
            ?: RemoteResult(emptyList(), null, "Pas de connexion internet")
    }

    /** Local cache, then the backend, then Open Food Facts. The found product is cached. */
    suspend fun findByBarcode(code: String): FoodEntity? {
        dao.byBarcode(code)?.let { return it }
        val s = settings.current()
        val dto = (if (s.serverUrl.isNotBlank()) attempt { api.barcode(s.serverUrl, s.apiKey, code) } else null)
            ?: attempt { off.product(code) }
        return dto?.let { cache(it) }
    }

    suspend fun testServer(url: String, apiKey: String): Result<String> = runCatching { api.health(url, apiKey) }

    /** Saves a remote food locally, keeping the user's favorite flag and usage stats. */
    suspend fun cache(dto: FoodDto): FoodEntity {
        val existing = dao.byId(dto.id)
        val entity = dto.toEntity().let {
            if (existing == null) it
            else it.copy(isFavorite = existing.isFavorite, useCount = existing.useCount, lastUsedAt = existing.lastUsedAt)
        }
        dao.upsert(entity)
        dto.servingPortion()?.let { serving ->
            if (dao.portions(dto.id).none { it.label == serving.label }) dao.upsertPortions(listOf(serving))
        }
        return entity
    }

    suspend fun food(id: String): FoodEntity? = dao.byId(id)

    suspend fun portions(foodId: String): List<PortionEntity> = dao.portions(foodId)

    suspend fun loggable(foodId: String): Loggable? {
        val food = dao.byId(foodId) ?: return null
        return Loggable(
            foodId = food.id,
            name = food.name,
            subtitle = listOfNotNull(food.brand, food.source.label).joinToString(" · "),
            per100 = food.per100,
            portions = dao.portions(food.id).map { Portion(it.label, it.grams) },
        )
    }

    suspend fun setFavorite(id: String, favorite: Boolean) = dao.setFavorite(id, favorite)

    suspend fun markUsed(id: String) = dao.markUsed(id, now())

    suspend fun saveCustom(food: FoodEntity, portions: List<PortionEntity>) {
        dao.upsert(food.copy(updatedAt = now()))
        dao.deletePortions(food.id)
        dao.upsertPortions(portions.map { it.copy(foodId = food.id) })
    }

    suspend fun delete(id: String) = dao.delete(id)
}

/** Runs a network call; null on any failure except coroutine cancellation. */
private suspend fun <T> attempt(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
