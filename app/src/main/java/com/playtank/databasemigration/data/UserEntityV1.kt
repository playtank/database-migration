package com.playtank.databasemigration.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The v1 schema: only id and name.
 */
@Entity(tableName = "users")
data class UserEntityV1(
    @PrimaryKey val id: Int,
    val name: String
)
