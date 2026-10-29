package com.playtank.databasemigration.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.playtank.databasemigration.data.AppDatabase
import java.io.IOException

private const val TEST_DB = "migration_test"

/**
 * Instrumented migration tests – must run on a real device or emulator.
 *
 * The two tests below mirror the two states described in the task:
 *
 *  Test 1 – BROKEN migration (INPUT 1 problem state)
 *   Proves that ALTER TABLE without DEFAULT crashes when rows already exist.
 *   @Test(expected = Exception::class) passes only when an exception is thrown,
 *   making the test red-if-silent and green-if-crashing – exactly the behaviour
 *   we need to document the bug.
 *
 *  Test 2 – FIXED migration (FINAL OUTPUT 1)
 *   Proves that ALTER TABLE … DEFAULT '' succeeds, existing rows survive, and
 *   the email column contains the default empty string (zero data loss).
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    // -----------------------------------------------------------------------
    // Test 1 – Broken migration MUST throw when rows exist (INPUT 1 problem)
    // -----------------------------------------------------------------------

    /**
     * SQLite rejects `ADD COLUMN … NOT NULL` without a DEFAULT on a non-empty
     * table with: "Cannot add a NOT NULL column with default value NULL".
     *
     * The test is annotated @Test(expected = Exception::class), so it PASSES
     * only when an exception is thrown.  If it were to pass silently the test
     * would fail – alerting us that the "broken" migration is no longer broken.
     */
    @Test(expected = Exception::class)
    @Throws(IOException::class)
    fun migration1To2_broken_throwsWhenRowsExist() {
        // Create v1 database and pre-populate it so the NOT NULL constraint fires.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (1, 'Alice')")
        }

        // Apply the broken migration – must throw.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2_BROKEN)
    }

    // -----------------------------------------------------------------------
    // Test 2 – Fixed migration MUST preserve data and default email (OUTPUT 1)
    // -----------------------------------------------------------------------

    /**
     * Verifies that MIGRATION_1_2_FIXED:
     *  1. Completes without throwing.
     *  2. Leaves the pre-existing row intact (no data loss).
     *  3. Sets email to '' (the DEFAULT) for rows that existed before the migration.
     */
    @Test
    @Throws(IOException::class)
    fun migration1To2_fixed_preservesDataAndDefaultsEmail() {
        // ── Arrange ──────────────────────────────────────────────────────────
        // Seed a v1 database with one user (no email column yet).
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (1, 'Alice')")
        }

        // ── Act ──────────────────────────────────────────────────────────────
        // Apply MIGRATION_1_2_FIXED and validate the resulting schema.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2_FIXED)

        // ── Assert ───────────────────────────────────────────────────────────
        db.query("SELECT id, name, email FROM users WHERE id = 1").use { cursor ->
            // Row must still exist.
            assertEquals("Expected exactly one row after migration", 1, cursor.count)
            cursor.moveToFirst()

            // Name is untouched.
            assertEquals("Alice", cursor.getString(cursor.getColumnIndexOrThrow("name")))

            // email was backfilled with the DEFAULT empty string – not NULL, not lost.
            assertEquals(
                "email must default to empty string for pre-migration rows",
                "",
                cursor.getString(cursor.getColumnIndexOrThrow("email"))
            )
        }
    }
}
