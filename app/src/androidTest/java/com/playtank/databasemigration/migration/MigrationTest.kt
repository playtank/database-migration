package com.playtank.databasemigration.migration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playtank.databasemigration.data.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

private const val TEST_DB = "migration_test"

/**
 * Instrumented migration tests – must run on a real device or emulator.
 *
 * Each test follows the same pattern:
 *  1. Use [MigrationTestHelper.createDatabase] to open a database at the
 *     desired *starting* version.  Room creates the schema exactly as it was
 *     at that version by replaying the exported JSON schema file from
 *     app/schemas/.
 *  2. Insert seed data that already exists in the database *before* the
 *     migration runs (simulates a real user upgrade scenario).
 *  3. Call [MigrationTestHelper.runMigrationsAndValidate] with the migration
 *     under test.  This applies the migration and checks the resulting schema
 *     against the target-version JSON schema file.
 *  4. Assert that pre-existing data is intact and new columns have the correct
 *     default values.
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
    // Test 1 – Broken migration MUST fail
    // -----------------------------------------------------------------------

    /**
     * Verifies that [MIGRATION_1_2_BROKEN] is genuinely broken.
     *
     * SQLite disallows "ADD COLUMN … NOT NULL" without a DEFAULT value when the
     * table already contains rows, because it cannot backfill existing rows.
     * This test proves the bug exists so the fix can be properly motivated.
     *
     * The test passes when an exception is thrown; it fails if no exception is
     * thrown (which would mean the migration was silently accepted – a false
     * negative that hides the real production risk).
     */
    @Test(expected = Exception::class)
    @Throws(IOException::class)
    fun migration1To2_broken_throwsOnExistingRows() {
        // Create a v1 database and pre-populate it so the constraint is triggered.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (1, 'Alice')")
        }

        // Apply the broken migration – should throw.
        helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2_BROKEN)
    }

    // -----------------------------------------------------------------------
    // Test 2 – Fixed migration MUST succeed and preserve existing data
    // -----------------------------------------------------------------------

    /**
     * Verifies that [MIGRATION_1_2_FIXED] successfully migrates a v1 database
     * that already contains rows.
     *
     * After the migration:
     *  • The row inserted in v1 must still be present (data not lost).
     *  • The new `email` column must exist and contain the default value ''.
     */
    @Test
    @Throws(IOException::class)
    fun migration1To2_fixed_preservesDataAndDefaultsEmail() {
        // Arrange – seed the v1 database.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (1, 'Alice')")
        }

        // Act – apply the fixed migration.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2_FIXED)

        // Assert – Alice's row survived and email defaulted to ''.
        db.query("SELECT id, name, email FROM users WHERE id = 1").use { cursor ->
            assertNotNull("Row must survive migration", cursor)
            assertEquals("Exactly one row expected", 1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Alice", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals(
                "email must default to empty string",
                "",
                cursor.getString(cursor.getColumnIndexOrThrow("email"))
            )
        }
    }

    // -----------------------------------------------------------------------
    // Test 3 – Migration 2 → 3 adds the `age` column correctly
    // -----------------------------------------------------------------------

    /**
     * Verifies that [MIGRATION_2_3] successfully migrates a v2 database.
     *
     * After the migration:
     *  • Existing rows survive with `age` defaulted to 0.
     */
    @Test
    @Throws(IOException::class)
    fun migration2To3_addsAgeColumnWithDefaultZero() {
        // Arrange – seed the v2 database (users has id, name, email).
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO users (id, name, email) VALUES (2, 'Bob', 'bob@example.com')"
            )
        }

        // Act – apply the v2→v3 migration.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // Assert – Bob's row survived and age defaulted to 0.
        db.query("SELECT id, name, email, age FROM users WHERE id = 2").use { cursor ->
            assertNotNull("Row must survive migration", cursor)
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Bob", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("bob@example.com", cursor.getString(cursor.getColumnIndexOrThrow("email")))
            assertEquals(
                "age must default to 0",
                0,
                cursor.getInt(cursor.getColumnIndexOrThrow("age"))
            )
        }
    }

    // -----------------------------------------------------------------------
    // Test 4 – Full upgrade path v1 → v3 using only the fixed migrations
    // -----------------------------------------------------------------------

    /**
     * Runs both migrations in sequence (v1 → v2 → v3) in one call and
     * validates the final schema.
     *
     * This is the closest simulation to what a real user experiences when they
     * have not opened the app since version 1 and install the latest build.
     */
    @Test
    @Throws(IOException::class)
    fun migration_fullPath_v1ToV3_preservesDataAndAddsColumns() {
        // Arrange – seed the v1 database.
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (3, 'Carol')")
        }

        // Act – migrate all the way to v3.
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 3, true,
            MIGRATION_1_2_FIXED, MIGRATION_2_3
        )

        // Assert – Carol's row is intact with both new columns at their defaults.
        db.query("SELECT id, name, email, age FROM users WHERE id = 3").use { cursor ->
            assertNotNull(cursor)
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Carol", cursor.getString(cursor.getColumnIndexOrThrow("name")))
            assertEquals("", cursor.getString(cursor.getColumnIndexOrThrow("email")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("age")))
        }
    }
}
