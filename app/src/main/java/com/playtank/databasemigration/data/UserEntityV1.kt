package com.playtank.databasemigration.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The v1 schema: only id and name.
 * Used exclusively by [AppDatabaseV1] to reproduce the crash scenario.
 */
@Entity(tableName = "users")
data class UserEntityV1(
    @PrimaryKey val id: Int,
    val name: String
)
