package com.playtank.databasemigration.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * FIXED state – version = 2, entity has (id, name, email).
 *
 * Used in two ways:
 *
 *  Step 2 (BROKEN): opened WITHOUT registering a migration against the v1
 *  file on disk.  Room detects the version jump and the hash mismatch → crash.
 *
 *  Step 3 (FIXED): opened WITH [com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED]
 *  registered.  Room runs the migration, backfills email = '', and succeeds.
 */
@Database(
    entities = [UserEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabaseV2 : RoomDatabase() {
    abstract fun userDao(): UserDao
}
