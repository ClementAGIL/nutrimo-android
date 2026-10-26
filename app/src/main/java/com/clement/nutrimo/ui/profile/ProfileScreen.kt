// Edit the profile used for the calorie target.
package com.clement.nutrimo.ui.profile

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(private val c: AppContainer) : ViewModel() {
    val draft = MutableStateFlow(ProfileDraft())

    init {
        viewModelScope.launch { c.body.profile.first()?.let { draft.value = ProfileDraft.from(it) } }
    }

    fun save(onDone: () -> Unit) = viewModelScope.launch {
        c.body.saveProfile(draft.value.toEntity())
        onDone()
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> ProfileViewModel(c) }
    val draft by vm.draft.collectAsStateWithLifecycle()
    ScreenScaffold("Profil", onBack = onBack) { padding ->
        ScrollColumn(padding, spacing = 16.dp) {
            ProfileForm(draft) { vm.draft.value = it }
            Button(onClick = { vm.save(onBack) }, enabled = draft.isValid, modifier = Modifier.fillMaxWidth()) {
                Text("Enregistrer")
            }
        }
    }
}
