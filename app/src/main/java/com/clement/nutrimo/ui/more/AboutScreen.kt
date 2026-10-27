// About: version, data sources and their licences (attribution is required by ODbL), health disclaimer.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.clement.nutrimo.BuildConfig
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold("À propos", onBack = onBack) { padding ->
        ScrollColumn(padding) {
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
