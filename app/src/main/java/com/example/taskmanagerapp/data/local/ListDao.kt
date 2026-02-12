package com.example.taskmanagerapp.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListDao {

    @Query("SELECT * FROM task_lists ORDER BY updatedAt DESC")
    fun getLists(): LiveData<List<ListEntity>>

    @Query("SELECT * FROM task_lists")
    suspend fun getAllListsOnce(): List<ListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(list: ListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(lists: List<ListEntity>)
}
