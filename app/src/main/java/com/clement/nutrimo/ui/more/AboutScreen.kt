// About: version, data sources and their licences (attribution is required by ODbL), health disclaimer.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.clement.nutrimo.BuildConfig
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold("À propos", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Nutrimo ${BuildConfig.VERSION_NAME}") {
                Text("Vos données restent sur ce téléphone. Pensez à les exporter (Plus → Sauvegarde).")
            }
            SectionCard("Sources des données") {
                Text("Table de composition nutritionnelle Ciqual 2020, ANSES — Licence Ouverte / Etalab.")
                Text("Open Food Facts (openfoodfacts.org) — données sous Open Database License (ODbL).")
            }
            SectionCard("Avertissement") {
                Text(
                    "Les besoins caloriques sont des estimations (formule de Mifflin-St Jeor). " +
                        "Une perte de poids rapide ou un apport très bas doit se faire avec un suivi médical.",
                )
            }
        }
    }
}
