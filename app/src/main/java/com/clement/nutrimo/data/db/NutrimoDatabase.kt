// The app's single Room database; all personal data lives here, on the phone.
package com.clement.nutrimo.data.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ProfileEntity::class, WeightEntity::class, MeasurementEntity::class, GoalEntity::class, WaterEntity::class,
        FoodEntity::class, PortionEntity::class, RecipeEntity::class, IngredientEntity::class, DiaryEntity::class,
        SavedMealEntity::class, SavedMealItemEntity::class, ActivityEntity::class,
    ],
    version = 3,
    // New tables only: Room generates the migration from the exported schemas (app/schemas). Never drop user data.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
@TypeConverters(Converters::class)
abstract class NutrimoDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun goalDao(): GoalDao
    abstract fun waterDao(): WaterDao
    abstract fun foodDao(): FoodDao
    abstract fun recipeDao(): RecipeDao
    abstract fun diaryDao(): DiaryDao
    abstract fun savedMealDao(): SavedMealDao
    abstract fun activityDao(): ActivityDao

    companion object {
        fun create(context: Context): NutrimoDatabase =
            Room.databaseBuilder(context, NutrimoDatabase::class.java, "nutrimo.db").build()
    }
}
