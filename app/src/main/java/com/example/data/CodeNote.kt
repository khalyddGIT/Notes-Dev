package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code_notes")
data class CodeNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val code: String,
    val cssCode: String = "",
    val jsCode: String = "",
    val notes: String = "",
    val language: String,
    val tags: String = "",
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
