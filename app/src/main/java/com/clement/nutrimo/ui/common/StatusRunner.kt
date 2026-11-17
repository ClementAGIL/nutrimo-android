// Runs a screen action that ends with a message for the user (a UiText: "Sauvegarde exportée.", "Erreur : …").
package com.clement.nutrimo.ui.common

import com.clement.nutrimo.util.uiText
import com.clement.nutrimo.util.UserFacingException
import com.clement.nutrimo.util.UiText
import com.clement.nutrimo.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * [status] is the last message, [busy] is true while an action runs. Owned by a ViewModel, with its viewModelScope
 * (or the app scope for work that must finish even if the screen is closed).
 */
class StatusRunner(private val scope: CoroutineScope) {
    var status by mutableStateOf<UiText?>(null)
        private set
    var busy by mutableStateOf(false)
        private set

    /** [block] returns the success message; a [UserFacingException] shows its text, any other failure "Erreur : …". */
    fun run(block: suspend () -> UiText) {
        busy = true
        scope.launch {
            status = runCatching { block() }.getOrElse { (it as? UserFacingException)?.text ?: uiText(R.string.error_generic, it.message.orEmpty()) }
            busy = false
        }
    }
}
