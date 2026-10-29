package com.playtank.databasemigration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED
import com.playtank.databasemigration.migration.MIGRATION_2_3

/**
 * Current database version: 3
 *
 * Schema evolution:
 *  1 → 2  Added `email TEXT NOT NULL DEFAULT ''`
 *  2 → 3  Added `age INTEGER NOT NULL DEFAULT 0`
 */
@Database(
    entities = [UserEntity::class],
    version = 3,
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
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2_FIXED, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
