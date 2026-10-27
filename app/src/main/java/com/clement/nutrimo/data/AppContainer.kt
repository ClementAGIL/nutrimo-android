// Creates and holds the app-wide singletons (database, settings, repositories).
package com.clement.nutrimo.data

import android.content.Context
import com.clement.nutrimo.data.db.NutrimoDatabase
import com.clement.nutrimo.data.health.HealthRepository
import com.clement.nutrimo.data.network.Http
import com.clement.nutrimo.data.network.NutrimoApi
import com.clement.nutrimo.data.repository.ActivityRepository
import com.clement.nutrimo.data.repository.BackupRepository
import com.clement.nutrimo.data.repository.BodyRepository
import com.clement.nutrimo.data.repository.DiaryRepository
import com.clement.nutrimo.data.repository.FoodRepository
import com.clement.nutrimo.data.repository.RecipeRepository
import com.clement.nutrimo.data.repository.SavedMealRepository
import com.clement.nutrimo.data.repository.SettingsRepository
import com.clement.nutrimo.data.sync.ServerBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    /** For work that must outlive a screen (seeding, backup import). */
    val appScope = CoroutineScope(SupervisorJob())

    val db = NutrimoDatabase.create(context)
    val http = Http()
    val settings = SettingsRepository(context)
    val body = BodyRepository(db)
    val foods = FoodRepository(context, db, http, settings)
    val recipes = RecipeRepository(db)
    val diary = DiaryRepository(db, foods, recipes)
    val savedMeals = SavedMealRepository(db)
    val activities = ActivityRepository(db)
    val health = HealthRepository(context)
    val backup = BackupRepository(db, http.json)
    val serverBackup = ServerBackup(backup, NutrimoApi(http), settings)

    init {
        appScope.launch { foods.seedCiqualIfNeeded() }
    }
}
