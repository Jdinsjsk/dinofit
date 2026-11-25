package com.dinofit.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class, SetEntity::class, DailyStepsEntity::class, CustomExerciseEntity::class,
        WalkEntity::class, WalkPointEntity::class, MealEntity::class,
        FavoriteMealEntity::class, TemplateEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class DinoFitDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun stepsDao(): StepsDao
    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun walkDao(): WalkDao
    abstract fun mealDao(): MealDao
    abstract fun favoriteMealDao(): FavoriteMealDao
    abstract fun templateDao(): TemplateDao
}

/** v1 → v2 : ajoute les marches GPS sans toucher aux séances déjà enregistrées. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `walks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `endedAt` INTEGER, `distanceM` REAL NOT NULL, `steps` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `walk_points` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`walkId` INTEGER NOT NULL, `time` INTEGER NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, " +
                "FOREIGN KEY(`walkId`) REFERENCES `walks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_walk_points_walkId` ON `walk_points` (`walkId`)")
    }
}

/** v2 → v3 : ajoute les repas. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, " +
                "`time` INTEGER NOT NULL, `name` TEXT NOT NULL, `kcal` REAL NOT NULL, `protein` REAL NOT NULL, " +
                "`carbs` REAL NOT NULL, `fat` REAL NOT NULL, `source` TEXT NOT NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_meals_date` ON `meals` (`date`)")
    }
}

/** v3 → v4 : repas favoris et séances modèles. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `favorite_meals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`kcal` REAL NOT NULL, `protein` REAL NOT NULL, `carbs` REAL NOT NULL, `fat` REAL NOT NULL)",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_meals_name` ON `favorite_meals` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `workout_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `exerciseIds` TEXT NOT NULL)",
        )
    }
}
