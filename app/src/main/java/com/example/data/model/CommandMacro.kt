package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_macros")
data class CommandMacro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String, // "RCLONE", "ADB", "TERMUX", "Z_GHOST"
    val commandText: String,
    val description: String,
    val isFavorite: Boolean = false,
    val requiresRoot: Boolean = false
)
