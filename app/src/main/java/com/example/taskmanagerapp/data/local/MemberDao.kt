package com.example.taskmanagerapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemberDao {

    @Query("SELECT * FROM list_members WHERE listId = :listId")
    suspend fun getMembers(listId: Long): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(members: List<MemberEntity>)
}
