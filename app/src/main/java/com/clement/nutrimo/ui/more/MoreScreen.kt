// "Plus" tab: entry points to the profile, goals, measurements, reminders, server, backup and about screens.
package com.clement.nutrimo.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.navigation.AboutRoute
import com.clement.nutrimo.ui.navigation.BackupRoute
import com.clement.nutrimo.ui.navigation.DevRoute
import com.clement.nutrimo.ui.navigation.GoalEditRoute
import com.clement.nutrimo.ui.navigation.GoalsRoute
import com.clement.nutrimo.ui.navigation.MacrosRoute
import com.clement.nutrimo.ui.navigation.ProfileRoute
import com.clement.nutrimo.ui.navigation.RemindersRoute
import com.clement.nutrimo.ui.navigation.ServerRoute
import com.clement.nutrimo.ui.navigation.StepsRoute

data class MoreEntry(val title: String, val subtitle: String, val icon: ImageVector, val route: Any)

/** Grows as features land; each entry opens its own screen. */
val MORE_ENTRIES = listOf(
    MoreEntry("Profil", "Sexe, âge, taille, activité", Icons.Default.Person, ProfileRoute),
    MoreEntry("Nouvel objectif", "Poids cible et rythme", Icons.Default.Flag, GoalEditRoute),
    MoreEntry("Objectifs de macros", "Protéines, glucides, lipides", Icons.Default.PieChart, MacrosRoute),
    MoreEntry("Pas et sport", "Objectifs de pas et de sport, Health Connect", Icons.Default.DirectionsWalk, StepsRoute),
    MoreEntry("Mes objectifs", "Objectif en cours et archivés", Icons.Default.History, GoalsRoute),
    MoreEntry("Rappels", "Pesée et saisie des repas", Icons.Default.Notifications, RemindersRoute),
    MoreEntry("Serveur", "Adresse de nutrimo-api sur le Wi-Fi", Icons.Default.Dns, ServerRoute),
    MoreEntry("Sauvegarde", "Exporter / importer vos données", Icons.Default.Backup, BackupRoute),
    MoreEntry("À propos", "Sources des données et licences", Icons.Default.Info, AboutRoute),
    MoreEntry("Développeur", "Données de démo, tout effacer", Icons.Default.Science, DevRoute),
)

@Composable
fun MoreScreen(onOpen: (Any) -> Unit) {
    ScreenScaffold("Plus") { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(MORE_ENTRIES) { entry ->
                ListItem(
                    headlineContent = { Text(entry.title) },
                    supportingContent = { Text(entry.subtitle) },
                    leadingContent = { Icon(entry.icon, null) },
                    modifier = Modifier.clickable { onOpen(entry.route) },
                )
                HorizontalDivider()
            }
        }
    }
}
