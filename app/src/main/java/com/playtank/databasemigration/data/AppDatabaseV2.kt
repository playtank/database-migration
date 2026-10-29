package com.playtank.databasemigration.data

import androidx.room.Database
import androidx.room.RoomDatabase

// v2 schema: users(id, name, email).
// Used twice: once without a migration (crashes), once with MIGRATION_1_2_FIXED (succeeds).
@Database(entities = [UserEntityV2::class], version = 2, exportSchema = true)
abstract class AppDatabaseV2 : RoomDatabase() {
    abstract fun userDao(): UserDaoV2
}
