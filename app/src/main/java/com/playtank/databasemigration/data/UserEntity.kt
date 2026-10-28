package com.playtank.databasemigration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Version history:
 *  v1 – id, name
 *  v2 – added email (TEXT NOT NULL DEFAULT '')
 *  v3 – added age  (INTEGER NOT NULL DEFAULT 0)
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "email", defaultValue = "") val email: String,
    @ColumnInfo(name = "age", defaultValue = "0") val age: Int
)
