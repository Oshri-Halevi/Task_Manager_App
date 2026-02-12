package com.example.taskmanagerapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_lists")
data class ListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val ownerId: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val remoteId: String? = null
)
