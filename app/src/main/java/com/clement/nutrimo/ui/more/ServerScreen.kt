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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.clement.nutrimo.ui.common.IntroText
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.WideButton
import com.clement.nutrimo.ui.common.appContainer
import com.clement.nutrimo.ui.common.rememberStatusRunner
import kotlinx.coroutines.launch

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    val tester = rememberStatusRunner(scope)
    var url by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(Unit) { c.settings.current().let { url = it.serverUrl; key = it.apiKey } }
    ScreenScaffold("Serveur Nutrimo", onBack = onBack) { padding ->
        ScrollColumn(padding) {
            IntroText(
                "Adresse du PC qui fait tourner nutrimo-api, sur le même Wi-Fi (ex : http://192.168.1.20:3000). " +
                    "Sans serveur, la recherche utilise le téléphone puis Open Food Facts.",
            )
            OutlinedTextField(url, { url = it.trim() }, label = { Text("Adresse du serveur") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(key, { key = it.trim() }, label = { Text("Clé API (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            WideButton(if (tester.busy) "Test en cours…" else "Tester la connexion", {
                tester.run { c.foods.testServer(url, key).fold({ "✅ Connecté : $it" }, { "❌ Injoignable (${it.message})" }) }
            }, enabled = url.isNotBlank() && !tester.busy, outlined = true)
            WideButton("Enregistrer", { scope.launch { c.settings.setServer(url, key); onBack() } })
            tester.status?.let { Text(it) }
        }
    }
}
