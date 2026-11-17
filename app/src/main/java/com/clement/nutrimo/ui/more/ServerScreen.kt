// Nutrimo API settings: server URL on the home network, optional API key, connection test.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.StatusRunner
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appViewModel
import kotlinx.coroutines.launch

class ServerViewModel(private val c: AppContainer) : ViewModel() {
    val tester = StatusRunner(viewModelScope)

    suspend fun saved(): Pair<String, String> = c.settings.current().let { it.serverUrl to it.apiKey }

    fun test(url: String, key: String) = tester.run {
        c.foods.testServer(url, key).fold({ "✅ Connecté : $it" }, { "❌ Injoignable (${it.message})" })
    }

    fun save(url: String, key: String, onDone: () -> Unit) = viewModelScope.launch { c.settings.setServer(url, key); onDone() }
}

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> ServerViewModel(c) }
    val tester = vm.tester
    var url by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.saved().let { (u, k) -> url = u; key = k } }
    ScreenScaffold("Serveur Nutrimo", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                "Adresse du PC qui fait tourner nutrimo-api, sur le même Wi-Fi (ex : http://192.168.1.20:3000). " +
                    "Sans serveur, la recherche utilise le téléphone puis Open Food Facts.",
            )
            OutlinedTextField(url, { url = it.trim() }, label = { Text("Adresse du serveur") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(key, { key = it.trim() }, label = { Text("Clé API (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            WideButton(if (tester.busy) "Test en cours…" else "Tester la connexion", { vm.test(url, key) },
                enabled = url.isNotBlank() && !tester.busy, outlined = true)
            WideButton("Enregistrer", { vm.save(url, key, onBack) })
            tester.status?.let { Text(it) }
        }
    }
}
