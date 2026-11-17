// Application: owns the dependency container (manual DI, no framework needed at this size).
package com.clement.nutrimo

import android.app.Application
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.reminders.Reminders
import kotlinx.coroutines.launch

class NutrimoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannels(this)
        // Some reminders are on by default, and a restore brings settings back without their jobs: plan every
        // enabled one (KEEP leaves an already planned job alone).
        container.appScope.launch { Reminders.scheduleAll(this@NutrimoApp, container.settings.current()) }
    }
}
