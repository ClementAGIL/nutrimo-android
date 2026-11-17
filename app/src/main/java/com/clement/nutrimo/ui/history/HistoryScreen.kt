// History screen (journal's chart icon): calendar of daily goals by default, charts in the second tab.
package com.clement.nutrimo.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clement.nutrimo.R
import com.clement.nutrimo.ui.common.AppTabs
import com.clement.nutrimo.ui.common.ScreenScaffold
import com.clement.nutrimo.ui.common.appViewModel

private val TABS = listOf(R.string.history_tab_calendar, R.string.history_tab_charts)

@Composable
fun HistoryScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, _ -> HistoryViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    ScreenScaffold(stringResource(R.string.common_historique), onBack = onBack) { padding ->
        Column(Modifier.padding(padding)) {
            AppTabs(TABS.map { stringResource(it) }, tab) { tab = it }
            if (tab == 0) CalendarTab(state, vm::showMonth, vm::select) else ChartsTab(state, vm::setChartDays)
        }
    }
}
