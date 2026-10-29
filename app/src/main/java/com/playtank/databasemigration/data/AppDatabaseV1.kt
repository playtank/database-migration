package com.playtank.databasemigration.data

import androidx.room.Database
import androidx.room.RoomDatabase

// v1 schema: users(id, name). Used to seed the "before" state on disk.
@Database(entities = [UserEntityV1::class], version = 1, exportSchema = false)
abstract class AppDatabaseV1 : RoomDatabase() {
    abstract fun userDao(): UserDaoV1
}
