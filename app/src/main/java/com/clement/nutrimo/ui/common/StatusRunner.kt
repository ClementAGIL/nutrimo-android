// Runs a screen action that ends with a message for the user ("Sauvegarde exportée.", "Erreur : …").
package com.clement.nutrimo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** [status] is the last message, [busy] is true while an action runs. */
class StatusRunner(private val scope: CoroutineScope) {
    var status by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    /** [block] returns the success message; any failure becomes "Erreur : …". */
    fun run(block: suspend () -> String) {
        busy = true
        scope.launch {
            status = runCatching { block() }.getOrElse { "Erreur : ${it.message}" }
            busy = false
        }
    }
}

/** Pass the app scope for work that must finish even if the user leaves the screen. */
@Composable
fun rememberStatusRunner(scope: CoroutineScope = rememberCoroutineScope()): StatusRunner = remember(scope) { StatusRunner(scope) }
