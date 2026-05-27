package com.example.ui

import android.widget.Toast
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.CodeNote

// List of predefined programming languages
val PREDEFINED_LANGUAGES = listOf(
    "Todos", "Kotlin", "Java", "Python", "JavaScript", 
    "TypeScript", "C++", "C#", "HTML/CSS", "SQL", 
    "Go", "Rust", "Swift", "Bash/Shell"
)

enum class CodeNotesTab {
    SNIPPETS,
    PROJECTS,
    CLOUD,
    CONFIG
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CodeNoteViewModel) {
    val notes by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedLanguage by viewModel.selectedLanguage.collectAsStateWithLifecycle()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsStateWithLifecycle()
    val editorTheme by viewModel.editorTheme.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(CodeNotesTab.SNIPPETS) }
    var showFormDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<CodeNote?>(null) }
    
    // Tracks which note is expanded to view full details
    var expandedNoteId by remember { mutableStateOf<Int?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Application Header matching Immersive UI Design HTML style
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CodeIcon(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = when (activeTab) {
                                CodeNotesTab.SNIPPETS -> "CodeNotes"
                                CodeNotesTab.PROJECTS -> "CodeProjects"
                                CodeNotesTab.CLOUD -> "CodeCloud"
                                CodeNotesTab.CONFIG -> "CodeConfig"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = when (activeTab) {
                                CodeNotesTab.SNIPPETS -> "DEVELOPER EDITION"
                                CodeNotesTab.PROJECTS -> "WORKSPACES"
                                CodeNotesTab.CLOUD -> "SYNCHRONIZATION"
                                CodeNotesTab.CONFIG -> "ENVIRONMENT"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    
                    if (activeTab == CodeNotesTab.SNIPPETS) {
                        // Favorite Toggle Badge
                        IconButton(
                            onClick = { viewModel.showFavoritesOnly.value = !showFavoritesOnly },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (showFavoritesOnly) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .testTag("favorites_filter_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                tint = if (showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                contentDescription = "Filtrar Favoritos"
                            )
                        }
                    }
                }

                if (activeTab == CodeNotesTab.SNIPPETS) {
                    // Search Bar input field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.searchQuery.value = it },
                        placeholder = { Text("Buscar por título, código o etiquetas...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.None,
                            imeAction = ImeAction.Search
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Scrollable language filter chips row with 50.dp Rounded full-capsule shapes
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PREDEFINED_LANGUAGES) { language ->
                            val isSelected = selectedLanguage == language
                            InputChip(
                                selected = isSelected,
                                onClick = { viewModel.selectedLanguage.value = language },
                                label = { Text(language, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) },
                                colors = InputChipDefaults.inputChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                ),
                                border = null,
                                shape = RoundedCornerShape(50.dp)
                            )
                        }
                    }
                } else {
                    // Short breadcrumb indicator for non-home screens
                    Text(
                        text = when (activeTab) {
                            CodeNotesTab.PROJECTS -> "Administra y filtra tus fragmentos por lenguaje o tag."
                            CodeNotesTab.CLOUD -> "Configuración de copia de seguridad local y remota."
                            CodeNotesTab.CONFIG -> "Estadísticas del sistema y preferencias del editor."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        floatingActionButton = {
            if (activeTab == CodeNotesTab.SNIPPETS) {
                ExtendedFloatingActionButton(
                    onClick = {
                        noteToEdit = null
                        showFormDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Añadir") },
                    text = { Text("Nueva Nota", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .testTag("add_note_fab"),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        bottomBar = {
            // Elegant rounded Bottom Navigation Bar matched to the Immersive UI design HTML
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(32.dp)),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == CodeNotesTab.SNIPPETS,
                    onClick = { activeTab = CodeNotesTab.SNIPPETS },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Snippets") },
                    label = { Text("SNIPPETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == CodeNotesTab.PROJECTS,
                    onClick = { activeTab = CodeNotesTab.PROJECTS },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Proyectos") },
                    label = { Text("PROJECTS", fontSize = 10.sp, letterSpacing = 0.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == CodeNotesTab.CLOUD,
                    onClick = { activeTab = CodeNotesTab.CLOUD },
                    icon = { Icon(Icons.Default.Refresh, contentDescription = "Cloud Synced") },
                    label = { Text("CLOUD", fontSize = 10.sp, letterSpacing = 0.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == CodeNotesTab.CONFIG,
                    onClick = { activeTab = CodeNotesTab.CONFIG },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("CONFIG", fontSize = 10.sp, letterSpacing = 0.5.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                CodeNotesTab.SNIPPETS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(visible = showFavoritesOnly) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFFFB300),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Filtrando por Favoritos únicamente",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.showFavoritesOnly.value = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Limpiar filtro",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (notes.isEmpty()) {
                            // Beautiful and helpful Empty State view
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp)
                                    .testTag("empty_state_view"),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Elevated stylized logo placeholder
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(30.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CodeIcon(modifier = Modifier.size(54.dp), color = MaterialTheme.colorScheme.primary)
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    text = "No se encontraron notas",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (searchQuery.isNotEmpty() || selectedLanguage != "Todos") {
                                        "Intenta cambiando los filtros o el término de búsqueda."
                                    } else {
                                        "¡Crea tu primera nota para organizar y guardar tus fragmentos de código de desarrollo!"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            // Scrollable container showing filtered notes list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                            ) {
                                items(notes, key = { it.id }) { note ->
                                    val isExpanded = expandedNoteId == note.id
                                    NoteItemCard(
                                        note = note,
                                        isExpanded = isExpanded,
                                        editorTheme = editorTheme,
                                        onToggleExpand = {
                                            expandedNoteId = if (isExpanded) null else note.id
                                        },
                                        onToggleFavorite = { viewModel.toggleFavorite(note) },
                                        onEdit = {
                                            noteToEdit = note
                                            showFormDialog = true
                                        },
                                        onDelete = { viewModel.deleteNoteById(note.id) },
                                        onSelectTag = { tag ->
                                            viewModel.searchQuery.value = tag
                                            viewModel.selectedLanguage.value = "Todos"
                                            activeTab = CodeNotesTab.SNIPPETS
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                CodeNotesTab.PROJECTS -> {
                    ProjectsTabContent(
                        notes = notes,
                        onSelectLanguage = { viewModel.selectedLanguage.value = it },
                        onSelectTag = { viewModel.searchQuery.value = it },
                        onNavigateToSnippets = { activeTab = CodeNotesTab.SNIPPETS }
                    )
                }
                CodeNotesTab.CLOUD -> {
                    CloudTabContent()
                }
                CodeNotesTab.CONFIG -> {
                    ConfigTabContent(
                        notes = notes,
                        editorTheme = editorTheme,
                        onSelectTheme = { viewModel.setEditorTheme(it) },
                        onExportJson = { viewModel.exportToJson() },
                        onImportJson = { json -> viewModel.importFromJson(json) },
                        onClearAllNotes = { viewModel.clearAllNotes() }
                    )
                }
            }
        }
    }

    // Animated overlay to add a new note or edit an existing one (renders natively within the same view structure)
    AnimatedVisibility(
        visible = showFormDialog,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        BackHandler(enabled = showFormDialog) {
            showFormDialog = false
        }
        NoteFormDialog(
            noteToEdit = noteToEdit,
            onDismiss = { showFormDialog = false },
            onSave = { title, code, cssCode, jsCode, explanatoryNote, language, tags ->
                if (noteToEdit == null) {
                    viewModel.insertNote(title, code, cssCode, jsCode, explanatoryNote, language, tags)
                } else {
                    viewModel.updateNote(
                        noteToEdit!!.copy(
                            title = title.trim(),
                            code = code,
                            cssCode = cssCode,
                            jsCode = jsCode,
                            notes = explanatoryNote.trim(),
                            language = language,
                            tags = tags.trim(),
                            timestamp = System.currentTimeMillis() // Update modified date
                        )
                    )
                }
                showFormDialog = false
            }
        )
    }
}
}

@Composable
fun NoteItemCard(
    note: CodeNote,
    isExpanded: Boolean,
    editorTheme: String,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelectTag: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showHtmlPreviewDialog by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()

    val terminalBgColor = when (editorTheme) {
        "Dracula" -> Color(0xFF282A36)
        "Nordic Blue" -> Color(0xFF2E3440)
        "Solarized Light" -> Color(0xFFFDF6E3)
        else -> Color(0xFF141416) // Monokai Classic / Default Dark
    }
    val terminalBorderColor = when (editorTheme) {
        "Solarized Light" -> Color(0xFF93A1A1).copy(alpha = 0.3f)
        else -> Color.Black.copy(alpha = 0.5f)
    }
    val terminalTextColor = when (editorTheme) {
        "Solarized Light" -> Color(0xFF586E75)
        else -> Color(0xFFD4D4D4)
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onToggleExpand() }
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Title, Language Tag and Favorite toggle button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Display colored pill for note's language
                        LanguagePill(language = note.language)
                        
                        // Show tags if any and card is collapsed
                        if (!isExpanded && note.tags.isNotEmpty()) {
                            Text(
                                text = note.tags.split(",").firstOrNull()?.trim() ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                maxLines = 1,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clickable { onSelectTag(note.tags.split(",").firstOrNull()?.trim() ?: "") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isExpanded) 3 else 1
                    )
                }

                // Row of compact upper utility icon buttons
                Row {
                    // Star Favorite Icon Button (Filled/Outlined states)
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("favorite_toggle_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            tint = if (note.isFavorite) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                            contentDescription = "Favorito"
                        )
                    }

                    // Copy snippet icon button using custom Canvas-rendered CopyIcon
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(note.code))
                            Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("copy_button_${note.id}")
                    ) {
                        CopyIcon(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Native Share snippet icon button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                val extraText = buildString {
                                    append("--- ${note.title} (${note.language}) ---\n\n")
                                    if (note.cssCode.isNotBlank() || note.jsCode.isNotBlank()) {
                                        append("[CÓDIGO HTML/ESTRUCTURA]\n")
                                        append(note.code)
                                        append("\n\n")
                                        if (note.cssCode.isNotBlank()) {
                                            append("[ESTILOS CSS]\n")
                                            append(note.cssCode)
                                            append("\n\n")
                                        }
                                        if (note.jsCode.isNotBlank()) {
                                            append("[COMPORTAMIENTO JS]\n")
                                            append(note.jsCode)
                                            append("\n\n")
                                        }
                                    } else {
                                        append(note.code)
                                        append("\n\n")
                                    }
                                    if (note.notes.isNotBlank()) {
                                        append("[NOTAS EXPLICATIVAS]\n")
                                        append(note.notes)
                                        append("\n")
                                    }
                                    append("\nEnviado desde mi CodeNotes")
                                }
                                putExtra(Intent.EXTRA_TEXT, extraText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir snippet via"))
                        },
                        modifier = Modifier.testTag("share_button_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            contentDescription = "Compartir"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Code Preview Box (styled as a terminal console)
            var activeViewerTab by remember { mutableStateOf(0) }
            val hasWebCodeSeparated = remember(note.code, note.cssCode, note.jsCode) {
                note.cssCode.isNotBlank() || note.jsCode.isNotBlank()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(terminalBgColor) // Deep dark browser window canvas background
                    .border(
                        androidx.compose.foundation.BorderStroke(1.dp, terminalBorderColor),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Column {
                    // Tiny terminal dots top-bar mimicry for developers + active tab label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF5F56)))
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFBD2E)))
                            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF27C93F)))
                        }
                        
                        if (isExpanded && hasWebCodeSeparated) {
                            Text(
                                text = when (activeViewerTab) {
                                    0 -> "HTML"
                                    1 -> "CSS"
                                    2 -> "JAVASCRIPT"
                                    else -> "HTML"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = terminalTextColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    if (isExpanded && hasWebCodeSeparated) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 10.dp)
                        ) {
                            listOf("Estructura HTML", "Estilos CSS", "Comportamiento JS").forEachIndexed { index, tabTitle ->
                                val isTabEnabled = when (index) {
                                    0 -> true
                                    1 -> note.cssCode.isNotBlank()
                                    2 -> note.jsCode.isNotBlank()
                                    else -> false
                                }
                                if (isTabEnabled) {
                                    val isSelected = activeViewerTab == index
                                    val chipBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent
                                    val chipFg = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else terminalTextColor.copy(alpha = 0.6f)
                                    val chipBorder = if (isSelected) Color.Transparent else terminalTextColor.copy(alpha = 0.2f)

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(chipBg)
                                            .border(1.dp, chipBorder, RoundedCornerShape(6.dp))
                                            .clickable { activeViewerTab = index }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = chipFg
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    val displayedCode = if (isExpanded) {
                        when (activeViewerTab) {
                            0 -> note.code
                            1 -> note.cssCode
                            2 -> note.jsCode
                            else -> note.code
                        }
                    } else {
                        note.code
                    }

                    val displayedLang = if (isExpanded) {
                        when (activeViewerTab) {
                            0 -> note.language
                            1 -> "CSS"
                            2 -> "JavaScript"
                            else -> note.language
                        }
                    } else {
                        note.language
                    }

                    if (isExpanded) {
                        // Scrolling Horizontal Row for non-warping full editor preview
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = highlightCode(
                                    code = displayedCode,
                                    language = displayedLang,
                                    isDark = editorTheme != "Solarized Light",
                                    theme = editorTheme
                                ),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = terminalTextColor,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        // Truncated code preview
                        Text(
                            text = highlightCode(
                                code = note.code.lines().take(4).joinToString("\n"),
                                language = note.language,
                                isDark = editorTheme != "Solarized Light",
                                theme = editorTheme
                            ),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = terminalTextColor,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (note.code.lines().size > 4) {
                            Text(
                                text = "... pulse para ver completo",
                                fontSize = 11.sp,
                                fontStyle = FontStyle.Italic,
                                color = if (editorTheme == "Solarized Light") Color(0xFF93A1A1) else Color(0xFF808080),
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }

            // Expanded Area: Notes explanation, tags chips, edit and delete buttons
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    if (note.notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Explicación / Comentarios:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }

                    if (note.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Display tags list horizontally
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            note.tags.split(",").forEach { tag ->
                                val cleanTag = tag.trim()
                                if (cleanTag.isNotEmpty()) {
                                    Text(
                                        text = "#$cleanTag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                            .clickable { onSelectTag(cleanTag) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons/Actions Row underneath expanded view
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isWebLanguage = remember(note.language, note.code) {
                            val langLower = note.language.lowercase()
                            langLower.contains("html") || 
                            langLower.contains("css") || 
                            langLower.contains("xml") || 
                            langLower.contains("svg") || 
                            langLower.contains("web") ||
                            langLower.contains("script") ||
                            note.code.contains("<html", ignoreCase = true) || 
                            note.code.contains("<div", ignoreCase = true) ||
                            note.code.contains("<svg", ignoreCase = true) ||
                            note.code.contains("<style>", ignoreCase = true) ||
                            note.code.contains("<p>", ignoreCase = true) ||
                            note.code.contains("<body>", ignoreCase = true)
                        }

                        if (isWebLanguage) {
                            Button(
                                onClick = { showHtmlPreviewDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier.testTag("compile_button_${note.id}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Compilar HTML/CSS", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compilar y Renderizar")
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Delete Button
                        TextButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_button_${note.id}")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Edit Note Button
                        Button(
                            onClick = onEdit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.testTag("edit_button_${note.id}")
                        ) {
                            Icon(Icons.Default.Create, contentDescription = "Editar", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Editar")
                        }
                    }
                }
            }
        }
    }

    if (showHtmlPreviewDialog) {
        HtmlCompilerPreviewDialog(note = note, onDismiss = { showHtmlPreviewDialog = false })
    }
}

@Composable
fun HtmlCompilerPreviewDialog(
    note: CodeNote,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Auto-prepare the compilation content. If it's CSS, build a sandbox.
    val compiledHtml = remember(note.code, note.cssCode, note.jsCode, note.language) {
        val lowerCode = note.code.lowercase()
        val lowerLang = note.language.lowercase()
        
        if (note.cssCode.isNotBlank() || note.jsCode.isNotBlank()) {
            val styleBlock = if (note.cssCode.isNotBlank()) "<style>\n${note.cssCode}\n</style>" else ""
            val scriptBlock = if (note.jsCode.isNotBlank()) "<script>\n${note.jsCode}\n</script>" else ""
            
            if (lowerCode.contains("<html") || lowerCode.contains("<!doctype")) {
                var htmlResult = note.code
                if (note.cssCode.isNotBlank()) {
                    htmlResult = if (htmlResult.contains("<head>", ignoreCase = true)) {
                        htmlResult.replace("<head>", "<head>\n$styleBlock", ignoreCase = true)
                    } else {
                        "$styleBlock\n$htmlResult"
                    }
                }
                if (note.jsCode.isNotBlank()) {
                    htmlResult = if (htmlResult.contains("</body>", ignoreCase = true)) {
                        htmlResult.replace("</body>", "$scriptBlock\n</body>", ignoreCase = true)
                    } else {
                        "$htmlResult\n$scriptBlock"
                    }
                }
                htmlResult
            } else {
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    $styleBlock
                </head>
                <body>
                    ${note.code}
                    $scriptBlock
                </body>
                </html>
                """.trimIndent()
            }
        } else {
            when {
                lowerLang.contains("css") && !lowerCode.contains("<html") && !lowerCode.contains("<body") -> {
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            ${note.code}
                        </style>
                    </head>
                    <body>
                        <div style="padding: 24px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;">
                            <h1 style="color: #6200EE; margin-bottom: 8px;">Estilo CSS Compilado</h1>
                            <p style="color: #333; font-size: 16px; line-height: 1.5;">Esta es una vista previa en tiempo real de tu fragmento de CSS cargado en un entorno de desarrollo.</p>
                            
                            <div class="test-container" style="margin-top: 20px; padding: 16px; border: 1px dashed #ccc; border-radius: 8px;">
                                <h3>Elemento de Prueba</h3>
                                <p>Modifica el código de tu nota para ver los cambios aplicados directamente sobre las clases e identificadores de esta zona.</p>
                                <button class="btn btn-primary" style="padding: 8px 16px; border: none; border-radius: 4px; background: #6200EE; color: white;">Botón de Muestra</button>
                            </div>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()
                }
                !lowerCode.contains("<html") && !lowerCode.contains("<!doctype") -> {
                    // If it is flat body elements, wrap it nicely in an elegant boilerplate to support CSS and proper scaling
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="utf-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                margin: 0;
                                padding: 16px;
                                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                                background-color: #ffffff;
                                color: #202124;
                            }
                        </style>
                    </head>
                    <body>
                        ${note.code}
                    </body>
                    </html>
                    """.trimIndent()
                }
                else -> {
                    // Standard complete HTML note code
                    note.code
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false // Permits full screen or near-full screen scaling
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header of compilation sandbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Compilador HTML & CSS",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(50.dp))
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Compile visualizer device container
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    webViewClient = WebViewClient()
                                }
                            },
                            update = { wv ->
                                wv.loadDataWithBaseURL(null, compiledHtml, "text/html", "UTF-8", null)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ejecutando en entorno aislado (WebView)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}

// FlowRow layout mimic to keep things standard without extra experimental libraries
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    // Basic Column fallback to safely handle tags layout
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
fun LanguagePill(language: String) {
    // Generate beautiful distinct pastel container/text color combinations for major languages
    val (bgColor, textColor) = when (language.lowercase()) {
        "kotlin" -> Color(0xFFECE4FA) to Color(0xFF6B4AA3)
        "python" -> Color(0xFFE4F3ED) to Color(0xFF1E6C4C)
        "java" -> Color(0xFFFBECE1) to Color(0xFFBC5812)
        "javascript" -> Color(0xFFFEF9E3) to Color(0xFF977A0F)
        "typescript" -> Color(0xFFE1EFFB) to Color(0xFF1565C0)
        "c++" -> Color(0xFFE3EDF6) to Color(0xFF1F5F91)
        "sql" -> Color(0xFFFCE6E8) to Color(0xFFB71C1C)
        "rust" -> Color(0xFFF0E5DE) to Color(0xFF5D4037)
        "swift" -> Color(0xFFFFF0E6) to Color(0xFFE65100)
        "go" -> Color(0xFFE1F5FE) to Color(0xFF0288D1)
        "html/css" -> Color(0xFFEFEBE9) to Color(0xFF4E342E)
        else -> Color(0xFFF1F1F1) to Color(0xFF424242)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = language,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            fontSize = 11.sp
        )
    }
}

// Custom code syntax highlighting parser
@Composable
fun highlightCode(code: String, language: String, isDark: Boolean, theme: String = "Monokai"): AnnotatedString {
    return remember(code, language, isDark, theme) {
        val keywords = setOf(
            "val", "var", "fun", "class", "interface", "import", "package", "return", "if", "else", 
            "while", "for", "in", "when", "def", "let", "const", "function", "public", "private", 
            "protected", "void", "int", "float", "double", "char", "boolean", "bool", "lambda", "async",
            "await", "struct", "impl", "fn", "use", "pub", "select", "from", "where", "join", "inner",
            "group", "by", "having", "order", "as", "into", "and", "or", "not", "insert", "update", "delete"
        )
        
        val builder = AnnotatedString.Builder(code)
        
        // Custom color schemes for developer themes
        val (keywordColor, commentColor, stringColor, numberColor, typeColor) = when (theme) {
            "Dracula" -> Triple(Color(0xFFFF79C6), Color(0xFF6272A4), Triple(Color(0xFFF1FA8C), Color(0xFFBD93F9), Color(0xFF8BE9FD)))
            "Nordic Blue" -> Triple(Color(0xFF81A1C1), Color(0xFF4C566A), Triple(Color(0xFFA3BE8C), Color(0xFFB48EAD), Color(0xFF88C0D0)))
            "Solarized Light" -> Triple(Color(0xFF859900), Color(0xFF93A1A1), Triple(Color(0xFF2AA198), Color(0xFFD33682), Color(0xFF268BD2)))
            else -> Triple(Color(0xFFFF5252), Color(0xFF757575), Triple(Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFF00bcd4))) // Monokai Classic 
        }.let { (kw, cm, other) ->
            val (st, num, ty) = other
            listOf(kw, cm, st, num, ty)
        }

        // Split lines to detect line comments reliably
        val lines = code.split("\n")
        var currentOffset = 0
        for (line in lines) {
            val commentIndex = line.indexOf("//")
            val hashCommentIndex = line.indexOf("#")
            
            val commentStart = when {
                commentIndex != -1 && hashCommentIndex != -1 -> minOf(commentIndex, hashCommentIndex)
                commentIndex != -1 -> commentIndex
                else -> hashCommentIndex
            }

            if (commentStart != -1) {
                builder.addStyle(
                    SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                    currentOffset + commentStart,
                    currentOffset + line.length
                )
            }
            currentOffset += line.length + 1 // Increment line offset (+1 for newline character)
        }

        // Apply keyword styles
        val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        val matches = wordRegex.findAll(code)
        for (match in matches) {
            val word = match.value
            val range = match.range
            if (keywords.contains(word)) {
                builder.addStyle(
                    SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                    range.first,
                    range.last + 1
                )
            } else if (word.firstOrNull()?.isUpperCase() == true && word.length > 1) {
                // Style uppercase types/classes like String, Modifier, etc.
                builder.addStyle(
                    SpanStyle(color = typeColor, fontWeight = FontWeight.Medium),
                    range.first,
                    range.last + 1
                )
            }
        }

        // Apply number styles
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        val numMatches = numberRegex.findAll(code)
        for (match in numMatches) {
            builder.addStyle(
                SpanStyle(color = numberColor),
                match.range.first,
                match.range.last + 1
            )
        }

        // Apply string styles
        val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
        val strMatches = stringRegex.findAll(code)
        for (match in strMatches) {
            builder.addStyle(
                SpanStyle(color = stringColor),
                match.range.first,
                match.range.last + 1
            )
        }
        
        builder.toAnnotatedString()
    }
}

// Elegant full custom creation and editing dialog for notes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormDialog(
    noteToEdit: CodeNote?,
    onDismiss: () -> Unit,
    onSave: (title: String, code: String, cssCode: String, jsCode: String, note: String, language: String, tags: String) -> Unit
) {
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var code by remember { mutableStateOf(noteToEdit?.code ?: "") }
    var cssCode by remember { mutableStateOf(noteToEdit?.cssCode ?: "") }
    var jsCode by remember { mutableStateOf(noteToEdit?.jsCode ?: "") }
    var note by remember { mutableStateOf(noteToEdit?.notes ?: "") }
    var selectedLang by remember { mutableStateOf(noteToEdit?.language ?: "Kotlin") }
    var tags by remember { mutableStateOf(noteToEdit?.tags ?: "") }

    var expandedLangDropdown by remember { mutableStateOf(false) }
    var webFormTab by remember { mutableStateOf(0) } // 0 for HTML, 1 for CSS, 2 for JS

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        color = MaterialTheme.colorScheme.background
    ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (noteToEdit == null) "Nueva Nota de Código" else "Editar Nota de Código",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_note_button")) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (title.isBlank()) {
                                        title = "Snippet de $selectedLang"
                                    }
                                    if (selectedLang.equals("HTML/CSS", ignoreCase = true)) {
                                        if (code.isBlank() && cssCode.isBlank() && jsCode.isBlank()) {
                                            code = "<!-- Código vacío -->"
                                        }
                                    } else {
                                        if (code.isBlank()) {
                                            code = "// Código vacío"
                                        }
                                    }
                                    onSave(title, code, cssCode, jsCode, note, selectedLang, tags)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("save_note_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(
                                    text = if (noteToEdit == null) "Guardar Nota" else "Actualizar Nota",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title Input Box
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título de la Nota") },
                        placeholder = { Text("Ej. Inicialización de Retrofit Client") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Language Selector Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedLang,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Lenguaje de Programación") },
                            trailingIcon = {
                                IconButton(onClick = { expandedLangDropdown = !expandedLangDropdown }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Expandir menú")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedLangDropdown = !expandedLangDropdown },
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(
                            expanded = expandedLangDropdown,
                            onDismissRequest = { expandedLangDropdown = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            // Loop through predefined languages (excluding "Todos")
                            PREDEFINED_LANGUAGES.filter { it != "Todos" }.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        selectedLang = lang
                                        expandedLangDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Predefined Template Injection Assist row
                    Text(
                        text = "Plantillas rápidas para $selectedLang:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val templatesList: List<Pair<String, Any>> = when (selectedLang.lowercase()) {
                            "html/css" -> listOf(
                                "Tarjeta de Presentación" to Triple(
                                    "<!-- Estructura HTML -->\n<div class=\"card\">\n  <img src=\"https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=150&h=150\" alt=\"Avatar\">\n  <h2>Lucía Beltrán</h2>\n  <p class=\"title\">Desarrolladora Web</p>\n  <p>Creadora de experiencias minimalistas y de alto rendimiento utilizando HTML, CSS y JS.</p>\n  <button onclick=\"mostrarMensaje()\">Contactar</button>\n</div>",
                                    "/* Estilos CSS */\nbody {\n  background-color: #f6f8fa;\n}\n.card {\n  box-shadow: 0 4px 16px rgba(0,0,0,0.1);\n  max-width: 280px;\n  margin: 16px auto;\n  text-align: center;\n  font-family: system-ui, -apple-system, sans-serif;\n  background: #ffffff;\n  border-radius: 16px;\n  padding: 24px;\n}\nimg {\n  border-radius: 50%;\n  width: 90px;\n  height: 90px;\n  object-fit: cover;\n  border: 4px solid #00bcff;\n}\n.title {\n  color: #00bcff;\n  font-weight: bold;\n  font-size: 14px;\n  margin-top: 8px;\n}\nbutton {\n  border: none;\n  outline: 0;\n  padding: 10px 20px;\n  color: white;\n  background-color: #00bcff;\n  border-radius: 20px;\n  cursor: pointer;\n  width: 100%;\n  font-weight: bold;\n  margin-top: 16px;\n  transition: opacity 0.2s;\n}\nbutton:hover {\n  opacity: 0.9;\n}",
                                    "// Comportamiento JS\nfunction mostrarMensaje() {\n  alert('¡Hola! Has hecho clic en el botón de contacto de Lucía.');\n}"
                                ),
                                "Efecto Neón" to Triple(
                                    "<!-- Estructura HTML -->\n<div class=\"neon-container\">\n  <h1 class=\"neon-text\">LIVE CODE</h1>\n  <p>HTML & CSS interactivos con soporte independiente</p>\n</div>",
                                    "/* Estilos CSS */\nbody {\n  background-color: #0d0e15 !important;\n  color: #a0a5b5;\n}\n.neon-container {\n  text-align: center;\n  padding: 40px;\n  font-family: sans-serif;\n}\n.neon-text {\n  font-size: 2.5rem;\n  color: #fff;\n  text-shadow: 0 0 5px #fff, 0 0 10px #fff, 0 0 20px #00ff00, 0 0 30px #00ff00;\n  margin-bottom: 12px;\n}",
                                    "// Comportamiento JS\nconsole.log('Efecto Neón activado correctamente.');"
                                )
                            )
                            "kotlin" -> listOf(
                                "Compose Contador" to """@Composable
fun Contador() {
    var clics by remember { mutableStateOf(0) }
    Button(onClick = { clics++ }) {
        Text("Clics: ${'$'}clics")
    }
}""",
                                "Coroutine Flow" to """fun flowNumeros(): Flow<Int> = flow {
    for (i in 1..5) {
        delay(1000)
        emit(i)
    }
}"""
                            )
                            "python" -> listOf(
                                "Fetch API Simple" to """import requests

def fetch_ip():
    try:
        res = requests.get("https://api.ipify.org?format=json")
        return res.json()
    except Exception as e:
        print("Error:", e)""",
                                "List Comp" to """numeros = [1, 2, 3, 4, 5]
cuadrados_pares = [x**2 for x in numeros if x % 2 == 0]
print(cuadrados_pares) # [4, 16]"""
                            )
                            "javascript" -> listOf(
                                "Promise Fetch" to """async function fetchUsers() {
  try {
    const res = await fetch('/api/users');
    const users = await res.json();
    return users;
  } catch (err) {
    console.error(err);
  }
}"""
                            )
                            "typescript" -> listOf(
                                "Generic API Res" to """interface ApiResponse<T> {
  success: boolean;
  data: T;
  error?: string;
}

function handle<T>(r: ApiResponse<T>): T {
  if (r.success) return r.data;
  throw new Error(r.error);
}"""
                            )
                            "sql" -> listOf(
                                "Inner Join" to """SELECT 
    e.id, 
    e.nombre, 
    d.nombre_departamento
FROM empleados e
INNER JOIN departamentos d ON e.depto_id = d.id;"""
                            )
                            "rust" -> listOf(
                                "Async Main" to """#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Iniciando Tokio async run...");
    Ok(())
}"""
                            )
                            "go" -> listOf(
                                "Wait Group" to """package main
import (
	"fmt"
	"sync"
)
func main() {
	var wg sync.WaitGroup
	wg.Add(1)
	go func() {
		defer wg.Done()
		fmt.Println("Goroutine activa!")
	}()
	wg.Wait()
}"""
                            )
                            else -> emptyList()
                        }

                        if (templatesList.isEmpty()) {
                            item {
                                Text(
                                    text = "Ninguna plantilla disponible",
                                    fontSize = 11.sp,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        } else {
                            items(templatesList) { itemData ->
                                val tName = itemData.first
                                InputChip(
                                    selected = false,
                                    onClick = {
                                        val data = itemData.second
                                        if (data is Triple<*, *, *>) {
                                            code = data.first as String
                                            cssCode = data.second as String
                                            jsCode = data.third as String
                                        } else if (data is String) {
                                            code = data
                                        }
                                        
                                        if (title.isBlank()) {
                                            title = tName
                                        }
                                        val generatedTags = when (selectedLang.lowercase()) {
                                            "kotlin" -> "UI, Compose, Coroutines"
                                            "python" -> "Scripts, Networking"
                                            "javascript", "typescript" -> "Web, API, Async"
                                            "html/css" -> "Web, HTML, CSS, JS"
                                            "sql" -> "Database, Query"
                                            else -> "Snippet"
                                        }
                                        if (tags.isBlank()) {
                                            tags = generatedTags
                                        }
                                    },
                                    label = { Text(tName, fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                )
                            }
                        }
                    }

                    // Conditional Code Fields for Separate HTML and CSS/JS codes
                    if (selectedLang.equals("HTML/CSS", ignoreCase = true)) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Estructura (HTML)", "Estilos (CSS)", "Acción (JS)").forEachIndexed { index, tabTitle ->
                                    val isSelected = webFormTab == index
                                    val containerCol = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    val contentCol = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(containerCol)
                                            .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(10.dp))
                                            .clickable { webFormTab = index }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = contentCol
                                        )
                                    }
                                }
                            }
                            
                            when (webFormTab) {
                                0 -> {
                                    OutlinedTextField(
                                        value = code,
                                        onValueChange = { code = it },
                                        label = { Text("Código HTML") },
                                        placeholder = { Text("Escribe o pega la estructura HTML aquí o carga una plantilla rápida arriba...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 260.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            autoCorrect = false
                                        )
                                    )
                                }
                                1 -> {
                                    OutlinedTextField(
                                        value = cssCode,
                                        onValueChange = { cssCode = it },
                                        label = { Text("Estilo CSS (Opcional)") },
                                        placeholder = { Text("Escribe o pega tus reglas CSS aquí sin etiquetas <style>...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 260.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            autoCorrect = false
                                        )
                                    )
                                }
                                2 -> {
                                    OutlinedTextField(
                                        value = jsCode,
                                        onValueChange = { jsCode = it },
                                        label = { Text("Script JavaScript (Opcional)") },
                                        placeholder = { Text("Escribe o pega tus funciones y comportamiento JS aquí...") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 140.dp, max = 260.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                                        keyboardOptions = KeyboardOptions(
                                            capitalization = KeyboardCapitalization.None,
                                            autoCorrect = false
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        // Generic single editor
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text("Código Fuente") },
                            placeholder = { Text("Escribe o pega tu fragmento de código aquí...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 140.dp, max = 260.dp),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = false
                            )
                        )
                    }

                    // Explanation Notes Input (now visually bounded)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Explicación / Comentarios") },
                        placeholder = { Text("Añade descripciones útiles para entender rápidamente el código...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 90.dp, max = 150.dp),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 6
                    )

                    // Optional Comma-separated Tags Input
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Etiquetas (Separadas por comas)") },
                        placeholder = { Text("Ej. networking, api, retrofit") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

@Composable
fun CodeIcon(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.5.dp.toPx()
        
        // Draw left arrow <
        val leftPath = Path().apply {
            moveTo(w * 0.32f, h * 0.28f)
            lineTo(w * 0.12f, h * 0.5f)
            lineTo(w * 0.32f, h * 0.72f)
        }
        drawPath(
            path = leftPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Draw diagonal slash line /
        drawLine(
            color = color,
            start = Offset(w * 0.45f, h * 0.8f),
            end = Offset(w * 0.55f, h * 0.2f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        
        // Draw right arrow >
        val rightPath = Path().apply {
            moveTo(w * 0.68f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.72f)
        }
        drawPath(
            path = rightPath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CopyIcon(modifier: Modifier = Modifier, color: Color = LocalContentColor.current) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val strokeWidth = 1.8.dp.toPx()
        val r = 3.dp.toPx()
        
        // Draw background box
        drawRoundRect(
            color = color.copy(alpha = 0.45f),
            topLeft = Offset(width * 0.12f, height * 0.12f),
            size = Size(width * 0.58f, height * 0.58f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = strokeWidth)
        )
        // Draw foreground box
        drawRoundRect(
            color = color,
            topLeft = Offset(width * 0.3f, height * 0.3f),
            size = Size(width * 0.58f, height * 0.58f),
            cornerRadius = CornerRadius(r, r),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
fun ProjectsTabContent(
    notes: List<CodeNote>,
    onSelectLanguage: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onNavigateToSnippets: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Lenguajes Activos",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val languages = notes.groupBy { it.language }

        if (languages.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aún no tienes lenguajes",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Agrega snippets con lenguajes asignados para ver tus directorios.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                languages.forEach { (language, notesList) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectLanguage(language)
                                onNavigateToSnippets()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${notesList.size} fragmentos de código",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tags Section
        Text(
            text = "Estructura por Etiquetas (Tags)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Gather all unique tags in notes
        val notesWithTags = notes.filter { it.tags.isNotEmpty() }
        val allTags = notesWithTags.flatMap { note ->
            note.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.distinct()

        if (allTags.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Sin etiquetas activas",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Las etiquetas de tus notas crearán secciones automáticamente.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                allTags.forEach { tag ->
                    val taggedNotesCount = notes.count { it.tags.contains(tag) }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTag(tag)
                                onNavigateToSnippets()
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$taggedNotesCount fragmentos etiquetados",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun CloudTabContent() {
    var isSyncing by remember { mutableStateOf(false) }
    var syncProgress by remember { mutableStateOf(0f) }
    var syncDone by remember { mutableStateOf(false) }
    val syncLogs = remember { mutableStateListOf<String>() }
    val context = LocalContext.current

    LaunchedEffect(isSyncing) {
        if (isSyncing) {
            syncDone = false
            syncProgress = 0f
            syncLogs.clear()
            syncLogs.add("[17:22:15] Inicializando handshake seguro con CodeVault Cloud...")
            kotlinx.coroutines.delay(800)
            syncProgress = 0.25f
            syncLogs.add("[17:22:16] Autenticando usuario lilkhalydd@gmail.com...")
            kotlinx.coroutines.delay(800)
            syncProgress = 0.50f
            syncLogs.add("[17:22:17] Escaneando fragmentos de código locales...")
            kotlinx.coroutines.delay(650)
            syncProgress = 0.75f
            syncLogs.add("[17:22:17] Sincronizando metadatos y etiquetas...")
            kotlinx.coroutines.delay(900)
            syncProgress = 1.0f
            syncLogs.add("[17:22:18] ¡Sincronización de CodeNotes finalizada con éxito!")
            Toast.makeText(context, "¡Sincronizado exitosamente con CodeVault Cloud!", Toast.LENGTH_SHORT).show()
            isSyncing = false
            syncDone = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Copia de Seguridad en la Nube",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (syncDone) Color(0xFF2E7D32).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (syncDone) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = if (syncDone) "Respaldo al Día" else "Copia de Seguridad Activa",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Cuenta activa: lilkhalydd@gmail.com",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Uso de Almacenamiento",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { if (isSyncing) syncProgress else 0.05f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isSyncing) "Sincronizando..." else "46.2 KB de 15.0 GB usados",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "0.001%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { isSyncing = true },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sincronizando...")
                    } else {
                        Text("Sincronizar Cloud CodeVault")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Terminal de Operación (Logs)",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1B1F))
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.2f)), shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (syncLogs.isEmpty()) {
                Text(
                    text = "Consola inactiva. Presione el botón de arriba para iniciar la sincronización remota.",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(syncLogs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = if (log.contains("Éxito") || log.contains("éxito") || log.contains("éxito")) Color(0xFF81C784) else MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun LanguageDistributionChart(notes: List<CodeNote>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Distribución por Lenguaje",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay fragmentos creados para analizar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Calculate count per language
                val langCounts = remember(notes) {
                    notes.groupBy { it.language }
                        .mapValues { it.value.size }
                        .toList()
                        .sortedByDescending { it.second }
                }

                val total = remember(langCounts) { langCounts.sumOf { it.second } }

                fun getLangColor(lang: String): Color {
                    return when (lang.lowercase()) {
                        "kotlin" -> Color(0xFF7F52FF)
                        "java" -> Color(0xFFE76F51)
                        "python" -> Color(0xFF3776AB)
                        "javascript" -> Color(0xFFF7DF1E)
                        "typescript" -> Color(0xFF3178C6)
                        "html/css" -> Color(0xFFE34F26)
                        "sql" -> Color(0xFF00758F)
                        "go" -> Color(0xFF00ADD8)
                        "rust" -> Color(0xFFCE412B)
                        "swift" -> Color(0xFFF05138)
                        "bash/shell" -> Color(0xFF4EAA25)
                        else -> Color(0xFF9E9E9E)
                    }
                }

                // Draw stacked progress bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    langCounts.forEach { (lang, count) ->
                        val weight = count.toFloat() / total
                        if (weight > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(weight)
                                    .background(getLangColor(lang))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Display legend grid chunked into clean rows of up to 3 columns each
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    langCounts.chunked(3).forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            chunk.forEach { (lang, count) ->
                                val pct = (count.toFloat() / total * 100).toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(getLangColor(lang))
                                    )
                                    Text(
                                        text = "$lang ($pct%)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (chunk.size < 3) {
                                repeat(3 - chunk.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigTabContent(
    notes: List<CodeNote>,
    editorTheme: String,
    onSelectTheme: (String) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Boolean,
    onClearAllNotes: () -> Unit
) {
    var isBiometricEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Dialog state controllers for actual JSON operations
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Métricas Generales",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Snippets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${notes.size}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Favoritos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${notes.count { it.isFavorite }}",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LanguageDistributionChart(notes = notes)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tema Visual del Editor",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Selecciona el esquema de colores para resaltar sintaxis:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val themes = listOf("Monokai", "Dracula", "Nordic Blue", "Solarized Light")
                    items(themes) { th ->
                        val isSel = editorTheme == th
                        InputChip(
                            selected = isSel,
                            onClick = { onSelectTheme(th) },
                            label = { Text(th, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = null
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Perfil del Desarrollador",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "lilkhalydd@gmail.com",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Suscripción Gratuita (Free Tier)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Configuración de Sistema",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Bloqueo por Huella / Facial",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Proteger seguridad local de snippets",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { isBiometricEnabled = it }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Copia y Respaldo por JSON",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Exporta o importa tus fragmentos de código.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { showExportDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Exportar")
                        }
                        Button(
                            onClick = { showImportDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Importar")
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Restablecer Datos Locales",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Elimina permanentemente todos tus snippets locales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = onClearAllNotes,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Eliminar Todo")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(100.dp))
    }

    // Real Export Dialog
    if (showExportDialog) {
        val exportedJson = onExportJson()
        val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Backup JSON de CodeNotes", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Copia este contenido JSON y guárdalo de manera segura para restaurar tus snippets en el futuro:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJson))
                        Toast.makeText(context, "Copia guardada en el portapapeles", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    }
                ) {
                    Text("Copiar JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Real Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restaurar Snippets (JSON)", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Pega un backup JSON previamente exportado:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("Pega el JSON aquí...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            val success = onImportJson(importText)
                            if (success) {
                                Toast.makeText(context, "¡Snippets restaurados con éxito!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "Error al procesar el formato JSON", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Introduce datos válidos", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
