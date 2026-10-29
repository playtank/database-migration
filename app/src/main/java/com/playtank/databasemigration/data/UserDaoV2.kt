package com.playtank.databasemigration.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDaoV2 {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntityV2)

    @Query("SELECT * FROM users")
    suspend fun getAll(): List<UserEntityV2>
}
