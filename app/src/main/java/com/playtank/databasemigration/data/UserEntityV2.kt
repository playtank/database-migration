package com.playtank.databasemigration.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// v2 schema: id, name, email
@Entity(tableName = "users")
data class UserEntityV2(
    @PrimaryKey
    val id: Int,

    val name: String,

    @ColumnInfo(name = "email", defaultValue = "''")
    val email: String
)
