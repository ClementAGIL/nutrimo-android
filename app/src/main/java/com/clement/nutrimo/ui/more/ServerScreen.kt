// Nutrimo API settings: server URL on the home network, optional API key, connection test.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appContainer
import kotlinx.coroutines.launch

@Composable
fun ServerScreen(onBack: () -> Unit) {
    val c = appContainer()
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf("") }
    var key by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { c.settings.current().let { url = it.serverUrl; key = it.apiKey } }
    ScreenScaffold("Serveur Nutrimo", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Adresse du PC qui fait tourner nutrimo-api, sur le même Wi-Fi (ex : http://192.168.1.20:3000). " +
                "Sans serveur, la recherche utilise le téléphone puis Open Food Facts.", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(url, { url = it.trim() }, label = { Text("Adresse du serveur") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(key, { key = it.trim() }, label = { Text("Clé API (facultatif)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    result = "Test en cours…"
                    scope.launch {
                        result = c.foods.testServer(url, key).fold({ "✅ Connecté : $it" }, { "❌ Injoignable (${it.message})" })
                    }
                }, enabled = url.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Tester") }
                Button(onClick = { scope.launch { c.settings.setServer(url, key); onBack() } }, modifier = Modifier.weight(1f)) {
                    Text("Enregistrer")
                }
            }
            result?.let { Text(it) }
        }
    }
}
