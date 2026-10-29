package com.playtank.databasemigration.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// ❌ Broken: missing DEFAULT — SQLite rejects NOT NULL without a default on existing rows
val MIGRATION_1_2_BROKEN = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL")
    }
}

// ✅ Fixed: DEFAULT '' backfills existing rows so the NOT NULL constraint is satisfied
val MIGRATION_1_2_FIXED = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE users ADD COLUMN email TEXT NOT NULL DEFAULT ''")
    }
}
