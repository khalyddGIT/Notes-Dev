package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CodeNote
import com.example.data.CodeNoteDatabase
import com.example.data.CodeNoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CodeNoteViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CodeNoteRepository
    
    // UI State filters
    val searchQuery = MutableStateFlow("")
    val selectedLanguage = MutableStateFlow("Todos")
    val showFavoritesOnly = MutableStateFlow(false)
    val editorTheme = MutableStateFlow("Monokai")

    init {
        val database = CodeNoteDatabase.getDatabase(application)
        repository = CodeNoteRepository(database.codeNoteDao)
        
        // Populate sample notes on first launch if empty
        viewModelScope.launch {
            repository.allNotes.first().let { currentNotes ->
                if (currentNotes.isEmpty()) {
                    insertSampleNotes()
                }
            }
        }
    }

    // Combine database streams with filters reactively
    val uiState: StateFlow<List<CodeNote>> = combine(
        repository.allNotes,
        searchQuery,
        selectedLanguage,
        showFavoritesOnly
    ) { notes, query, lang, favOnly ->
        notes.asSequence()
            .filter { note ->
                val matchesQuery = query.isBlank() || 
                    note.title.contains(query, ignoreCase = true) ||
                    note.code.contains(query, ignoreCase = true) ||
                    note.notes.contains(query, ignoreCase = true) ||
                    note.tags.contains(query, ignoreCase = true)
                
                val matchesLang = lang == "Todos" || note.language.equals(lang, ignoreCase = true)
                val matchesFav = !favOnly || note.isFavorite
                
                matchesQuery && matchesLang && matchesFav
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setEditorTheme(theme: String) {
        editorTheme.value = theme
    }

    fun exportToJson(): String {
        return try {
            val notesList = uiState.value
            val jsonArray = org.json.JSONArray()
            for (note in notesList) {
                val jsonObject = org.json.JSONObject().apply {
                    put("title", note.title)
                    put("code", note.code)
                    put("cssCode", note.cssCode)
                    put("jsCode", note.jsCode)
                    put("notes", note.notes)
                    put("language", note.language)
                    put("tags", note.tags)
                    put("isFavorite", note.isFavorite)
                }
                jsonArray.put(jsonObject)
            }
            jsonArray.toString(4)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun importFromJson(jsonString: String): Boolean {
        return try {
            val jsonArray = org.json.JSONArray(jsonString)
            viewModelScope.launch {
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val note = CodeNote(
                        title = obj.optString("title", "Importado"),
                        code = obj.optString("code", "// Sin Código"),
                        cssCode = obj.optString("cssCode", ""),
                        jsCode = obj.optString("jsCode", ""),
                        notes = obj.optString("notes", ""),
                        language = obj.optString("language", "Kotlin"),
                        tags = obj.optString("tags", ""),
                        isFavorite = obj.optBoolean("isFavorite", false)
                    )
                    repository.insert(note)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun insertNote(title: String, code: String, cssCode: String = "", jsCode: String = "", notes: String, language: String, tags: String) {
        viewModelScope.launch {
            val codeNote = CodeNote(
                title = title.trim(),
                code = code,
                cssCode = cssCode,
                jsCode = jsCode,
                notes = notes.trim(),
                language = language,
                tags = tags.trim()
            )
            repository.insert(codeNote)
        }
    }

    fun updateNote(note: CodeNote) {
        viewModelScope.launch {
            repository.update(note)
        }
    }

    fun toggleFavorite(note: CodeNote) {
        viewModelScope.launch {
            repository.update(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            repository.allNotes.first().forEach { note ->
                repository.deleteById(note.id)
            }
        }
    }

    private suspend fun insertSampleNotes() {
        val sample1 = CodeNote(
            title = "Saludo en Jetpack Compose",
            code = """@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "¡Hola, ${'$'}name!",
        modifier = modifier.padding(16.dp),
        style = MaterialTheme.typography.titleLarge
    )
}""",
            notes = "Un componente composable básico escrito en Kotlin que dibuja un texto elegante con relleno y estilo de tipografía de Material Theme 3.",
            language = "Kotlin",
            tags = "UI, Compose, Android",
            isFavorite = true
        )

        val sample2 = CodeNote(
            title = "Algoritmo Quicksort",
            code = """def quicksort(arr):
    if len(arr) <= 1:
        return arr
    pivot = arr[len(arr) // 2]
    left = [x for x in arr if x < pivot]
    middle = [x for x in arr if x == pivot]
    right = [x for x in arr if x > pivot]
    return quicksort(left) + middle + quicksort(right)

# Ejemplo de uso
print(quicksort([3, 6, 8, 10, 1, 2, 1]))""",
            notes = "Algoritmo clásico de ordenación rápida eficiente por el método de dividir y conquistar utilizando listas por comprensión en Python.",
            language = "Python",
            tags = "Algoritmos, Ordenación",
            isFavorite = false
        )

        val sample3 = CodeNote(
            title = "Consulta con INNER JOIN",
            code = """SELECT 
    users.id AS usuario_id,
    users.name AS nombre_usuario,
    COUNT(orders.id) AS total_pedidos,
    SUM(orders.amount) AS total_gastado
FROM users
INNER JOIN orders ON users.id = orders.user_id
GROUP BY users.id, users.name
HAVING total_gastado > 150.00
ORDER BY total_gastado DESC;""",
            notes = "Agrupa pedidos por usuario y filtra combinando tablas para obtener de forma eficiente aquellos que han comprado más de $150.00.",
            language = "SQL",
            tags = "Database, SQL, Agrupación",
            isFavorite = true
        )

        repository.insert(sample1)
        repository.insert(sample2)
        repository.insert(sample3)
    }
}
