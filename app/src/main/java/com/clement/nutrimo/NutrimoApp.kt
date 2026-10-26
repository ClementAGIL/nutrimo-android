// Application: owns the dependency container (manual DI, no framework needed at this size).
package com.clement.nutrimo

import android.app.Application
import com.clement.nutrimo.data.AppContainer
import com.clement.nutrimo.data.reminders.Reminders

class NutrimoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Reminders.createChannel(this)
    }
}
