// "Plus" tab: entry points to the profile, goals, measurements, reminders, server, backup and about screens.
package com.clement.nutrimo.ui.more

import androidx.annotation.StringRes
import com.clement.nutrimo.R
import androidx.compose.ui.res.stringResource
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

data class MoreEntry(@StringRes val title: Int, @StringRes val subtitle: Int, val icon: ImageVector, val route: Any)

/** Grows as features land; each entry opens its own screen. */
val MORE_ENTRIES = listOf(
    MoreEntry(R.string.more_profile, R.string.more_profile_hint, Icons.Default.Person, ProfileRoute),
    MoreEntry(R.string.more_new_goal, R.string.more_new_goal_hint, Icons.Default.Flag, GoalEditRoute),
    MoreEntry(R.string.more_macros, R.string.more_macros_hint, Icons.Default.PieChart, MacrosRoute),
    MoreEntry(R.string.more_steps, R.string.more_steps_hint, Icons.Default.DirectionsWalk, StepsRoute),
    MoreEntry(R.string.more_goals, R.string.more_goals_hint, Icons.Default.History, GoalsRoute),
    MoreEntry(R.string.more_reminders, R.string.more_reminders_hint, Icons.Default.Notifications, RemindersRoute),
    MoreEntry(R.string.more_server, R.string.more_server_hint, Icons.Default.Dns, ServerRoute),
    MoreEntry(R.string.more_backup, R.string.more_backup_hint, Icons.Default.Backup, BackupRoute),
    MoreEntry(R.string.more_about, R.string.more_about_hint, Icons.Default.Info, AboutRoute),
    MoreEntry(R.string.more_dev, R.string.more_dev_hint, Icons.Default.Science, DevRoute),
)

@Composable
fun MoreScreen(onOpen: (Any) -> Unit) {
    ScreenScaffold(stringResource(R.string.more_plus)) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(MORE_ENTRIES) { entry ->
                ListItem(
                    headlineContent = { Text(stringResource(entry.title)) },
                    supportingContent = { Text(stringResource(entry.subtitle)) },
                    leadingContent = { Icon(entry.icon, null) },
                    modifier = Modifier.clickable { onOpen(entry.route) },
                )
                HorizontalDivider()
            }
        }
    }
}
