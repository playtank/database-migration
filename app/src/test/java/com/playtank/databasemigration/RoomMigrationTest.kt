package com.playtank.databasemigration

@RunWith(AndroidJUnit4::class) // 也可以使用本地 JVM 运行器配合 SQLite-JDBC
class RoomMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration1To2() {
        // 1. 创建旧版本 (v1) 数据库
        helper.createDatabase("test-db", 1).apply {
            execSQL("INSERT INTO users (id, name) VALUES (1, 'Alvin')")
            close()
        }

        // 2. 执行迁移到 v2，如果 SQL 语句写错或者有不兼容改动，这里会直接抛出异常（Crash）
        val db = helper.runMigrationsAndValidate("test-db", 2, true, MIGRATION_1_2)

        // 3. 断言数据是否完备，确保没有发生数据丢失
        // ...
    }
}