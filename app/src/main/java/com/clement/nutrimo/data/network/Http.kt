// Minimal JSON-over-HTTP helper on OkHttp (the server URL changes at runtime, so no Retrofit).
package com.clement.nutrimo.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpException(val code: Int) : IOException("HTTP $code")

class Http {
    val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private val client = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /** GET [url] and return the body; throws [HttpException] on non-2xx and IOException on network errors. */
    suspend fun get(url: String, headers: Map<String, String> = emptyMap(), timeoutMs: Long? = null): String =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).apply { headers.forEach { (k, v) -> header(k, v) } }.build()
            val call = client.newCall(request)
            timeoutMs?.let { call.timeout().timeout(it, TimeUnit.MILLISECONDS) }
            call.execute().use { response ->
                if (!response.isSuccessful) throw HttpException(response.code)
                response.body?.string() ?: ""
            }
        }
}
