package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [CodeNote::class], version = 2, exportSchema = false)
abstract class CodeNoteDatabase : RoomDatabase() {
    abstract val codeNoteDao: CodeNoteDao

    companion object {
        @Volatile
        private var INSTANCE: CodeNoteDatabase? = null

        fun getDatabase(context: Context): CodeNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeNoteDatabase::class.java,
                    "code_notes_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
