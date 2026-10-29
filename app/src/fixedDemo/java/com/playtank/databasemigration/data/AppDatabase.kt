package com.playtank.databasemigration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED

/**
 * FIXED DEMO – version = 2, MIGRATION_1_2_FIXED registered.
 *
 * Install this variant AFTER the crashDemo variant (the emulator already has
 * "app_database" on disk at v1 with Alice's row).  When the app launches,
 * Room finds version 1 on disk and version 2 in code, selects
 * MIGRATION_1_2_FIXED, runs:
 *
 *   ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''
 *
 * Alice's row survives with email = '' (the DEFAULT).  No crash.  No data loss.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * DEMO STEP 2:
 *   Install the `fixedDemo` variant (do NOT clear app data).  Tap the button
 *   to see Alice's row with the defaulted email displayed on screen.
 * ──────────────────────────────────────────────────────────────────────────
 */
@Database(
    entities = [UserEntity::class],
    version = 2,            // ✅ bumped to match the new schema
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"          // same file name as crashDemo
                )
                    .addMigrations(MIGRATION_1_2_FIXED) // ✅ migration registered
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
