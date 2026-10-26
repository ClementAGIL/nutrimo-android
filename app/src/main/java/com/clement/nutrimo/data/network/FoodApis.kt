// Remote food sources: the user's Nutrimo API on the PC, and the public Open Food Facts API as a fallback.
package com.clement.nutrimo.data.network

import com.clement.nutrimo.domain.model.NutrientCatalog
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.URLEncoder

/** Backend calls fail fast so the search falls back to OFF quickly when away from home. */
private const val BACKEND_TIMEOUT_MS = 2500L
private const val BACKUP_TIMEOUT_MS = 30_000L
private const val OFF_BASE = "https://world.openfoodfacts.org"
private const val OFF_USER_AGENT = "Nutrimo/0.1 (Android; personal project)"
private const val OFF_FIELDS = "code,product_name,product_name_fr,brands,serving_size,serving_quantity,nutriments"
private const val KJ_PER_KCAL = 4.184
private val MICRO_FACTOR = mapOf("mg" to 1e3, "µg" to 1e6)

class NutrimoApi(private val http: Http) {
    suspend fun search(baseUrl: String, apiKey: String, query: String): List<FoodDto> {
        val body = http.get("${baseUrl.trimEnd('/')}/foods/search?q=${enc(query)}&limit=40", headers(apiKey), BACKEND_TIMEOUT_MS)
        return http.json.decodeFromString<SearchResponse>(body).items
    }

    /** Null when the backend answers 404. */
    suspend fun barcode(baseUrl: String, apiKey: String, code: String): FoodDto? = try {
        http.json.decodeFromString<FoodDto>(
            http.get("${baseUrl.trimEnd('/')}/foods/barcode/$code", headers(apiKey), BACKEND_TIMEOUT_MS * 3),
        )
    } catch (e: HttpException) {
        if (e.code == 404) null else throw e
    }

    /** Backups can be several MB: allow more time than a search. */
    suspend fun uploadBackup(baseUrl: String, apiKey: String, device: String, json: String): String =
        http.postJson("${baseUrl.trimEnd('/')}/backups", json, headers(apiKey) + ("X-Device" to device), BACKUP_TIMEOUT_MS)

    /** The latest backup JSON, or null when the server has none. */
    suspend fun latestBackup(baseUrl: String, apiKey: String): String? = try {
        http.get("${baseUrl.trimEnd('/')}/backups/latest", headers(apiKey), BACKUP_TIMEOUT_MS)
    } catch (e: HttpException) {
        if (e.code == 404) null else throw e
    }

    suspend fun health(baseUrl: String, apiKey: String): String =
        http.get("${baseUrl.trimEnd('/')}/health", headers(apiKey), BACKEND_TIMEOUT_MS)

    private fun headers(apiKey: String) = if (apiKey.isBlank()) emptyMap() else mapOf("X-Api-Key" to apiKey)
}

class OffApi(private val http: Http) {
    private val headers = mapOf("User-Agent" to OFF_USER_AGENT)

    suspend fun search(query: String): List<FoodDto> {
        val url = "$OFF_BASE/cgi/search.pl?search_terms=${enc(query)}&search_simple=1&action=process&json=1" +
            "&page_size=30&tagtype_0=countries&tag_contains_0=contains&tag_0=france&fields=$OFF_FIELDS"
        val products = http.json.parseToJsonElement(http.get(url, headers)).jsonObject["products"]?.jsonArray
        return products.orEmpty().mapNotNull { mapOffProduct(it.jsonObject) }
    }

    suspend fun product(code: String): FoodDto? {
        val body = http.json.parseToJsonElement(http.get("$OFF_BASE/api/v2/product/$code.json?fields=$OFF_FIELDS", headers))
        val product = body.jsonObject["product"]?.jsonObject ?: return null
        return mapOffProduct(product)
    }
}

/** Mirrors nutrimo-api src/off/mapper.ts: OFF micros are in grams, converted to the catalogue unit. */
internal fun mapOffProduct(p: JsonObject): FoodDto? {
    val code = p.str("code") ?: return null
    val name = p.str("product_name_fr") ?: p.str("product_name") ?: return null
    val n = p["nutriments"]?.jsonObject ?: return null
    val kcal = n.num("energy-kcal_100g") ?: n.num("energy_100g")?.div(KJ_PER_KCAL) ?: return null
    val micros = NutrientCatalog.OFF_KEYS.mapNotNull { (key, offKey) ->
        n.num(offKey)?.let { key to it * (MICRO_FACTOR[NutrientCatalog.unitOf(key)] ?: 1.0) }
    }.toMap()
    return FoodDto(
        id = "off:$code", source = "off", name = name, brand = p.str("brands")?.substringBefore(',')?.trim(),
        barcode = code, servingLabel = p.str("serving_size"), servingG = p.num("serving_quantity"),
        kcal = kcal, fat = n.num("fat_100g"), satFat = n.num("saturated-fat_100g"), carbs = n.num("carbohydrates_100g"),
        sugars = n.num("sugars_100g"), fiber = n.num("fiber_100g"), protein = n.num("proteins_100g"),
        salt = n.num("salt_100g"), micros = micros,
    )
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.content?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

private fun JsonObject.num(key: String): Double? = (this[key] as? JsonPrimitive)?.let { prim: JsonElement ->
    (prim as JsonPrimitive).doubleOrNull ?: prim.content.replace(',', '.').toDoubleOrNull()
}

private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
