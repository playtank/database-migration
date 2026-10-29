package com.playtank.databasemigration.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ---------------------------------------------------------------------------
// Migration 1 → 2
// ---------------------------------------------------------------------------

/**
 * BROKEN migration – intentionally left here to document the failure mode.
 *
 * `ALTER TABLE … ADD COLUMN email TEXT NOT NULL` has no DEFAULT clause.
 * SQLite requires a DEFAULT value when a NOT NULL column is added to a table
 * that already contains rows, because it cannot backfill existing rows with
 * NULL.  The statement is rejected with:
 *   "Cannot add a NOT NULL column with default value NULL"
 *
 * This constant must NOT be registered in AppDatabase.  It is only used in
 * the negative instrumented test that proves the bug exists.
 */
val MIGRATION_1_2_BROKEN = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ❌ Bug: NOT NULL without DEFAULT – throws on tables with existing rows.
        db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL")
    }
}

/**
 * FIXED migration – the correct implementation.
 *
 * `DEFAULT ''` satisfies SQLite's requirement: all existing rows are
 * backfilled automatically with an empty string, so the NOT NULL constraint
 * is never violated.
 */
val MIGRATION_1_2_FIXED = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ✅ Fix: DEFAULT '' backfills existing rows with an empty string.
        db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''")
    }
}
