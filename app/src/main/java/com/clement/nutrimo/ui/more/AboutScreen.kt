// About: version, data sources and their licences (attribution is required by ODbL), health disclaimer.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.clement.nutrimo.BuildConfig
import com.clement.nutrimo.R
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.ScrollColumn
import com.clement.nutrimo.ui.common.SectionCard

@Composable
fun AboutScreen(onBack: () -> Unit) {
    ScreenScaffold(stringResource(R.string.about_propos), onBack = onBack) { padding ->
        ScrollColumn(padding) {
            SectionCard(stringResource(R.string.about_nutrimo_1, BuildConfig.VERSION_NAME)) {
                Text(stringResource(R.string.about_vos_donnees_restent_sur_ce))
            }
            SectionCard(stringResource(R.string.about_sources_donnees)) {
                Text(stringResource(R.string.about_table_composition_nutritionnelle_ciqual_2020))
                Text(stringResource(R.string.about_open_food_facts_openfoodfacts_org))
            }
            SectionCard(stringResource(R.string.about_avertissement)) {
                Text(
                    stringResource(R.string.about_besoins_caloriques_sont_estimations_formule),
                )
            }
        }
    }
}
