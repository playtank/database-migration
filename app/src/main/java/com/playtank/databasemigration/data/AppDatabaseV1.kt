package com.playtank.databasemigration.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * BROKEN state – version = 1, entity has only (id, name).
 *
 * Used in Step 1 to seed a genuine v1 database via Room itself.
 * This is the "before" state: the database on disk has the v1 schema
 * and Room's identity hash for v1.
 *
 * Step 2 then tries to open this same file with [AppDatabaseV2], whose
 * compiled entity includes `email` — triggering the schema mismatch crash.
 */
@Database(
    entities = [UserEntityV1::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabaseV1 : RoomDatabase() {
    abstract fun userDao(): UserDaoV1
}
