// Error handling shared by the data sources that may fail for reasons outside the app (network, Health Connect).
package com.clement.nutrimo.data

import kotlin.coroutines.cancellation.CancellationException

/** Runs [block], turning any failure into null; cancellation still propagates so coroutines stop properly. */
suspend fun <T> nullOnError(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (_: Exception) {
    null
}
