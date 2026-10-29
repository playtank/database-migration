package com.playtank.databasemigration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * BROKEN – version = 1, but UserEntity already declares the `email` column.
 *
 * Room computes an identity hash from the compiled entity and compares it to
 * the hash stored in room_master_table on disk. The compiled hash now reflects
 * the `email` column, but the version number hasn't changed, so Room has no
 * migration to run and no way to reconcile the mismatch.
 *
 * First DB access throws:
 *   IllegalStateException: Room cannot verify the data integrity.
 *   Expected identity hash "72f8ca49…" but found "a53dda5d…"
 *
 * Fix: bump version to 2 and register MIGRATION_1_2_FIXED.
 */
@Database(
    entities = [UserEntity::class],
    version = 1,            // ← intentionally NOT bumped after adding `email`
    exportSchema = false    // no schema export for the broken version
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
                    // ❌ No migration registered – Room cannot reconcile the hash mismatch
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
