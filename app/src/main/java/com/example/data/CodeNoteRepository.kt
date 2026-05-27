package com.example.data

import kotlinx.coroutines.flow.Flow

class CodeNoteRepository(private val dao: CodeNoteDao) {
    val allNotes: Flow<List<CodeNote>> = dao.getAllNotes()
    val favoriteNotes: Flow<List<CodeNote>> = dao.getFavoriteNotes()

    suspend fun getNoteById(id: Int): CodeNote? {
        return dao.getNoteById(id)
    }

    suspend fun insert(note: CodeNote) {
        dao.insertNote(note)
    }

    suspend fun update(note: CodeNote) {
        dao.updateNote(note)
    }

    suspend fun delete(note: CodeNote) {
        dao.deleteNote(note)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteNoteById(id)
    }
}
