package com.dinofit.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SessionEntity::class, SetEntity::class, DailyStepsEntity::class, CustomExerciseEntity::class,
        WalkEntity::class, WalkPointEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class DinoFitDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun stepsDao(): StepsDao
    abstract fun customExerciseDao(): CustomExerciseDao
    abstract fun walkDao(): WalkDao
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
