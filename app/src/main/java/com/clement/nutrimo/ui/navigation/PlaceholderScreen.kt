// Temporary screen for tabs not built yet.
package com.clement.nutrimo.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.clement.nutrimo.ui.common.EmptyState
import com.clement.nutrimo.ui.common.ScreenScaffold

@Composable
fun PlaceholderScreen(title: String) {
    ScreenScaffold(title) { EmptyState("Bientôt disponible", Modifier.padding(it)) }
}
