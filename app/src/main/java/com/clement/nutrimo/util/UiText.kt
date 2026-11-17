// Text that is resolved later, where a Context is at hand: ViewModels, repositories and domain code return it
// instead of French strings (so they stay translatable and testable); the UI resolves it with asString().
package com.clement.nutrimo.util

import android.content.res.Resources
import androidx.annotation.StringRes

sealed interface UiText {
    /** A string resource; [args] may themselves be UiText (resolved first), e.g. a label inside a sentence. */
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText

    /** Text that needs no translation: user data, a server or exception message. */
    data class Raw(val text: String) : UiText

    fun resolve(resources: Resources): String = when (this) {
        is Raw -> text
        is Res -> resources.getString(id, *args.map { if (it is UiText) it.resolve(resources) else it }.toTypedArray())
    }
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())
