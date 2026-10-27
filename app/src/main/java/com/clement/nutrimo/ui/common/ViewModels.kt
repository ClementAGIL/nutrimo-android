// ViewModel creation with access to the AppContainer and the navigation arguments.
package com.clement.nutrimo.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.clement.nutrimo.NutrimoApp
import com.clement.nutrimo.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Keeps the flow running 5 s after the screen stops collecting, so a rotation does not restart it. */
fun <T> Flow<T>.stateWhileSubscribed(scope: CoroutineScope, initial: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(5_000), initial)

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as NutrimoApp).container

@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer, SavedStateHandle) -> VM): VM {
    val container = appContainer()
    return viewModel { create(container, createSavedStateHandle()) }
}
