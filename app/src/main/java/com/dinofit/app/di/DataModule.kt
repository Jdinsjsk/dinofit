package com.dinofit.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.dinofit.app.core.data.DataStoreProfileRepository
import com.dinofit.app.core.data.OfflineFirstStepsRepository
import com.dinofit.app.core.data.OfflineFirstWorkoutRepository
import com.dinofit.app.core.data.ProfileRepository
import com.dinofit.app.core.data.StepsRepository
import com.dinofit.app.core.data.WorkoutRepository
import com.dinofit.app.core.database.CustomExerciseDao
import com.dinofit.app.core.database.DinoFitDatabase
import com.dinofit.app.core.database.MIGRATION_1_2
import com.dinofit.app.core.database.MIGRATION_2_3
import com.dinofit.app.core.database.MIGRATION_3_4
import com.dinofit.app.core.database.FavoriteMealDao
import com.dinofit.app.core.database.TemplateDao
import com.dinofit.app.core.database.MealDao
import com.dinofit.app.core.data.NutritionRepository
import com.dinofit.app.core.data.OfflineFirstNutritionRepository
import com.dinofit.app.core.database.WalkDao
import com.dinofit.app.core.data.OfflineFirstWalkRepository
import com.dinofit.app.core.data.WalkRepository
import com.dinofit.app.core.database.StepsDao
import com.dinofit.app.core.database.WorkoutDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): DinoFitDatabase =
        Room.databaseBuilder(context, DinoFitDatabase::class.java, "dinofit.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides fun workoutDao(db: DinoFitDatabase): WorkoutDao = db.workoutDao()
    @Provides fun stepsDao(db: DinoFitDatabase): StepsDao = db.stepsDao()
    @Provides fun customExerciseDao(db: DinoFitDatabase): CustomExerciseDao = db.customExerciseDao()
    @Provides fun walkDao(db: DinoFitDatabase): WalkDao = db.walkDao()
    @Provides fun mealDao(db: DinoFitDatabase): MealDao = db.mealDao()
    @Provides fun favoriteMealDao(db: DinoFitDatabase): FavoriteMealDao = db.favoriteMealDao()
    @Provides fun templateDao(db: DinoFitDatabase): TemplateDao = db.templateDao()

    @Provides
    @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("dinofit") }
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {
    @Binds abstract fun workout(impl: OfflineFirstWorkoutRepository): WorkoutRepository
    @Binds abstract fun steps(impl: OfflineFirstStepsRepository): StepsRepository
    @Binds abstract fun profile(impl: DataStoreProfileRepository): ProfileRepository
    @Binds abstract fun walks(impl: OfflineFirstWalkRepository): WalkRepository
    @Binds abstract fun nutrition(impl: OfflineFirstNutritionRepository): NutritionRepository
}
