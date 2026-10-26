// Minimal JSON-over-HTTP helper on OkHttp (the server URL changes at runtime, so no Retrofit).
package com.clement.nutrimo.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpException(val code: Int) : IOException("HTTP $code")

private val JSON_TYPE = "application/json".toMediaType()

class Http {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /** GET [url] and return the body; throws [HttpException] on non-2xx and IOException on network errors. */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long? = null): String =
        execute(Request.Builder().url(url), headers, timeoutMs)

    /** POST a JSON [body]; same errors as [get]. */
    suspend fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long? = null): String =
        execute(Request.Builder().url(url).post(body.toRequestBody(JSON_TYPE)), headers, timeoutMs)

    private suspend fun execute(builder: Request.Builder, headers: Map<String, String>, timeoutMs: Long?): String =
        withContext(Dispatchers.IO) {
            headers.forEach { (k, v) -> builder.header(k, v) }
            val call = client.newCall(builder.build())
            timeoutMs?.let { call.timeout().timeout(it, TimeUnit.MILLISECONDS) }
            call.execute().use { response ->
                if (!response.isSuccessful) throw HttpException(response.code)
                response.body?.string() ?: ""
            }
        }
}
