package com.playtank.databasemigration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Current schema (v2).
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  INPUT 1 – Problem state (v1, causes the crash)                 │
 * │                                                                 │
 * │  @Entity(tableName = "users")                                   │
 * │  data class UserEntity(                                         │
 * │      @PrimaryKey val id: Int,                                   │
 * │      val name: String                       ← only two fields   │
 * │  )                                                              │
 * │                                                                 │
 * │  Adding `email` below while keeping @Database(version = 1)     │
 * │  triggers:                                                      │
 * │  IllegalStateException: Room cannot verify the data integrity.  │
 * │  Expected identity hash does not match …                        │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * FINAL OUTPUT 1 – Solution (v2):
 *  • Increment @Database version to 2.
 *  • Declare the new column with @ColumnInfo(defaultValue = "''") so
 *    that Room's schema export matches the ALTER TABLE DEFAULT value.
 *  • Supply MIGRATION_1_2_FIXED in Room.databaseBuilder.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int,

    val name: String,

    // Added in v2 – existing rows receive '' via the migration DEFAULT.
    @ColumnInfo(name = "email", defaultValue = "''")
    val email: String
)
