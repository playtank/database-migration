package com.playtank.databasemigration.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * CRASH DEMO – version = 1, but UserEntity already declares the `email` column.
 *
 * Room computes an identity hash from the compiled entity and compares it to
 * the hash stored in room_master_table on disk.  The compiled hash now reflects
 * the `email` column, but the version number hasn't changed, so Room has no
 * migration to run and no way to reconcile the mismatch.
 *
 * First access throws:
 *   IllegalStateException: Room cannot verify the data integrity.
 *   Expected identity hash "72f8ca49…" but found "a53dda5d…"
 *
 * ──────────────────────────────────────────────────────────────────────────
 * DEMO STEP 1:
 *   Install the `crashDemo` variant.  The app seeds a user row (id=1, name=
 *   "Alice") into a v1 database, then tries to open the same DB with this
 *   broken config → crash shown on screen.
 * ──────────────────────────────────────────────────────────────────────────
 */
@Database(
    entities = [UserEntity::class],
    version = 1,            // ← intentionally NOT bumped after adding `email`
    exportSchema = false    // no schema export needed for the crash flavor
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
                    "app_database"          // same file name as fixedDemo
                )
                    // ❌ No migration registered – Room cannot reconcile the hash mismatch
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
