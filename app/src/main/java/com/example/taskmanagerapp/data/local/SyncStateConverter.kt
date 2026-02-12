package com.example.taskmanagerapp.data.local

import androidx.room.TypeConverter

class SyncStateConverter {
    @TypeConverter
    fun toString(state: SyncState): String = state.name

    @TypeConverter
    fun fromString(value: String): SyncState = runCatching { SyncState.valueOf(value) }
        .getOrDefault(SyncState.SYNCED)
}
