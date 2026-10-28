package com.playtank.databasemigration.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ---------------------------------------------------------------------------
// Migration 1 → 2: adds the `email` column
// ---------------------------------------------------------------------------

/**
 * BROKEN migration: omits the DEFAULT clause.
 *
 * SQLite's ALTER TABLE … ADD COLUMN requires a DEFAULT value whenever the new
 * column is NOT NULL, because all existing rows must be given a value.
 * Without DEFAULT the statement is rejected with an error such as:
 *   "Cannot add a NOT NULL column with default value NULL"
 *
 * This object is intentionally left here to be used in tests that VERIFY the
 * failure.  It must NOT be registered in AppDatabase.
 */
val MIGRATION_1_2_BROKEN = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ❌ Bug: NOT NULL without a DEFAULT value – SQLite will throw.
        db.execSQL(
            "ALTER TABLE users ADD COLUMN email TEXT NOT NULL"
        )
    }
}

/**
 * FIXED migration: adds the `email` column with a proper DEFAULT value.
 *
 * The DEFAULT clause satisfies SQLite's requirement that all existing rows
 * receive a valid non-NULL value when the column is first introduced.
 */
val MIGRATION_1_2_FIXED = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ✅ Fix: provide DEFAULT '' so existing rows are backfilled automatically.
        db.execSQL(
            "ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''"
        )
    }
}

// ---------------------------------------------------------------------------
// Migration 2 → 3: adds the `age` column
// ---------------------------------------------------------------------------

/**
 * Correct migration that adds an integer `age` column with a sensible default.
 * Demonstrates best-practice migration on top of the already-fixed v2 schema.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // ✅ DEFAULT 0 satisfies NOT NULL for existing rows.
        db.execSQL(
            "ALTER TABLE users ADD COLUMN age INTEGER NOT NULL DEFAULT 0"
        )
    }
}
