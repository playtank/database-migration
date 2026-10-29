package com.playtank.databasemigration.data

import androidx.room.Database
import androidx.room.RoomDatabase

// v2 schema: users(id, name, email).
@Database(entities = [UserEntityV2::class], version = 2, exportSchema = true)
abstract class AppDatabaseV2 : RoomDatabase() {
    abstract fun userDao(): UserDaoV2
}
