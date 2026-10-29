package com.playtank.databasemigration

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playtank.databasemigration.data.AppDatabaseV2
import com.playtank.databasemigration.migration.MIGRATION_1_2_FIXED
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabaseV2::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration1To2() {
        // 1. 创建旧版本 (v1) 数据库
        helper.createDatabase("test-db", 1).use { db ->
            db.execSQL("INSERT INTO users (id, name) VALUES (1, 'Alvin')")
        }

        // 2. 执行迁移到 v2，如果 SQL 语句写错或者有不兼容改动，这里会直接抛出异常（Crash）
        val db = helper.runMigrationsAndValidate("test-db", 2, true, MIGRATION_1_2_FIXED)

        // 3. 断言数据是否完备，确保没有发生数据丢失
        // ...
    }
}
