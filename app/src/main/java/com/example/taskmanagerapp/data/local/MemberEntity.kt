package com.example.taskmanagerapp.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "list_members",
    indices = [Index(value = ["listId", "userId"], unique = true)]
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val userId: String,
    val role: String
)
