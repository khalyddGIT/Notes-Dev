package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeNoteDao {
    @Query("SELECT * FROM code_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<CodeNote>>

    @Query("SELECT * FROM code_notes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteNotes(): Flow<List<CodeNote>>

    @Query("SELECT * FROM code_notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Int): CodeNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: CodeNote): Long

    @Update
    suspend fun updateNote(note: CodeNote)

    @Delete
    suspend fun deleteNote(note: CodeNote)

    @Query("DELETE FROM code_notes WHERE id = :id")
    suspend fun deleteNoteById(id: Int)
}
