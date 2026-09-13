/*
 * Copyright (C) 2024-2026 Focus by Rj
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.focusbyrj.app.ui.screens.notes

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.focusbyrj.app.data.note.ChecklistItem
import com.focusbyrj.app.data.note.NoteDatabase
import com.focusbyrj.app.data.note.NoteEntity
import com.focusbyrj.app.data.note.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.UUID

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.widget.Toast
import com.focusbyrj.app.receiver.NoteReminderReceiver

enum class NoteFolder(val title: String) {
    NOTES("Notes"),
    REMINDERS("Reminders"),
    ARCHIVE("Archive"),
    TRASH("Trash")
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NoteRepository
    private val prefs = application.getSharedPreferences("keep_notes_prefs", Context.MODE_PRIVATE)
    private val alarmManager = application.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        val db = NoteDatabase.getInstance(application)
        repository = NoteRepository(db.noteDao())
    }

    // Current Active Folder
    private val _currentFolder = MutableStateFlow(NoteFolder.NOTES)
    val currentFolder: StateFlow<NoteFolder> = _currentFolder.asStateFlow()

    fun setFolder(folder: NoteFolder) {
        _currentFolder.value = folder
        _selectedColorFilter.value = null
        _selectedLabelFilter.value = null
        clearSelection()
    }

    // Multi-Select Mode (Google Keep selection action bar)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedNoteIds: StateFlow<Set<Long>> = _selectedNoteIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedNoteIds.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleNoteSelection(noteId: Long) {
        val current = _selectedNoteIds.value
        _selectedNoteIds.value = if (current.contains(noteId)) current - noteId else current + noteId
    }

    fun selectAllNotes(notes: List<NoteEntity>) {
        _selectedNoteIds.value = notes.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun reorderNotes(reorderedNotes: List<NoteEntity>) {
        if (reorderedNotes.size <= 1) return
        viewModelScope.launch(Dispatchers.IO) {
            val baseTime = System.currentTimeMillis()
            reorderedNotes.forEachIndexed { index, note ->
                val newTimestamp = baseTime - (index * 1000L)
                if (note.updatedAt != newTimestamp) {
                    repository.saveNote(note.copy(updatedAt = newTimestamp))
                }
            }
        }
    }

    fun pinSelectedNotes() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val notes = displayedNotes.value.filter { it.id in ids }
            val anyUnpinned = notes.any { !it.isPinned }
            notes.forEach { note ->
                repository.togglePin(note.id, !anyUnpinned)
            }
        }
        clearSelection()
    }

    fun archiveSelectedNotes() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> repository.setArchived(id, true) }
        }
        clearSelection()
    }

    fun trashSelectedNotes() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> repository.moveToTrash(id) }
        }
        clearSelection()
    }

    fun setSelectedNotesColor(colorKey: String) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id -> repository.setColor(id, colorKey) }
        }
        clearSelection()
    }

    fun setSelectedNotesReminder(timestamp: Long?) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                val note = displayedNotes.value.find { it.id == id }
                if (note != null) {
                    val updated = note.copy(reminderTimestamp = timestamp, updatedAt = System.currentTimeMillis())
                    repository.saveNote(updated)
                    if (timestamp != null && timestamp > System.currentTimeMillis()) {
                        scheduleReminderNotification(id, note.title, note.content, timestamp)
                    } else {
                        cancelReminderNotification(id)
                    }
                }
            }
        }
        clearSelection()
    }

    // Grid Layout Mode (2-column masonry grid vs 1-column list)
    private val _isGridView = MutableStateFlow(prefs.getBoolean("is_grid_view", true))
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    fun toggleLayoutMode() {
        val next = !_isGridView.value
        _isGridView.value = next
        prefs.edit().putBoolean("is_grid_view", next).apply()
    }

    // Search and Filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedColorFilter = MutableStateFlow<String?>(null)
    val selectedColorFilter: StateFlow<String?> = _selectedColorFilter.asStateFlow()

    private val _selectedLabelFilter = MutableStateFlow<String?>(null)
    val selectedLabelFilter: StateFlow<String?> = _selectedLabelFilter.asStateFlow()

    private val _customLabels = MutableStateFlow<Set<String>>(
        prefs.getStringSet("custom_labels", emptySet()) ?: emptySet()
    )

    fun addCustomLabel(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        val updated = _customLabels.value + trimmed
        _customLabels.value = updated
        prefs.edit().putStringSet("custom_labels", updated).apply()
    }

    fun renameCustomLabel(oldLabel: String, newLabel: String) {
        val trimmedOld = oldLabel.trim()
        val trimmedNew = newLabel.trim()
        if (trimmedNew.isBlank()) return
        val updated = _customLabels.value.map { if (it.equals(trimmedOld, ignoreCase = true)) trimmedNew else it }.toSet()
        _customLabels.value = updated
        prefs.edit().putStringSet("custom_labels", updated).apply()
        if (_selectedLabelFilter.value.equals(trimmedOld, ignoreCase = true)) {
            _selectedLabelFilter.value = trimmedNew
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.renameLabel(trimmedOld, trimmedNew)
        }
    }

    fun deleteCustomLabel(label: String) {
        val trimmed = label.trim()
        val updated = _customLabels.value.filter { !it.equals(trimmed, ignoreCase = true) }.toSet()
        _customLabels.value = updated
        prefs.edit().putStringSet("custom_labels", updated).apply()
        if (_selectedLabelFilter.value.equals(trimmed, ignoreCase = true)) {
            _selectedLabelFilter.value = null
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLabel(trimmed)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setColorFilter(colorKey: String?) {
        _selectedColorFilter.value = if (_selectedColorFilter.value == colorKey) null else colorKey
    }

    fun setLabelFilter(label: String?) {
        _selectedLabelFilter.value = if (_selectedLabelFilter.value == label) null else label
    }

    // Notes stream based on folder and multi-token ranked search query
    private val rawNotesFlow = combine(_currentFolder, _searchQuery) { folder, query ->
        Pair(folder, query)
    }.flatMapLatest { (folder, query) ->
        val folderFlow = when (folder) {
            NoteFolder.NOTES -> repository.getActiveNotes()
            NoteFolder.REMINDERS -> repository.getActiveNotes() // will filter reminderTimestamp != null
            NoteFolder.ARCHIVE -> repository.getArchivedNotes()
            NoteFolder.TRASH -> repository.getTrashedNotes()
        }
        if (query.isBlank()) {
            folderFlow
        } else {
            folderFlow.map { notes ->
                val tokens = query.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.isEmpty()) return@map notes

                notes.mapNotNull { note ->
                    val titleLower = note.title.lowercase()
                    val contentLower = note.content.lowercase()
                    val labels = note.getLabels().map { it.lowercase() }
                    val checklistItems = if (note.isChecklist) note.getChecklistItems().map { it.text.lowercase() } else emptyList()

                    // Check if all search tokens match somewhere in the note (title, content, labels, checklist)
                    val allTokensMatch = tokens.all { token ->
                        titleLower.contains(token) ||
                        contentLower.contains(token) ||
                        labels.any { it.contains(token) } ||
                        checklistItems.any { it.contains(token) }
                    }

                    if (!allTokensMatch) null
                    else {
                        // Compute smart relevance score
                        var score = 0
                        tokens.forEach { token ->
                            if (titleLower.contains(token)) score += 12
                            if (titleLower.startsWith(token)) score += 8
                            if (labels.any { it.equals(token, ignoreCase = true) }) score += 10
                            if (labels.any { it.contains(token) }) score += 5
                            if (checklistItems.any { it.contains(token) }) score += 4
                            if (contentLower.contains(token)) score += 2
                        }
                        Pair(note, score)
                    }
                }
                .sortedWith(
                    compareByDescending<Pair<NoteEntity, Int>> { it.first.isPinned }
                        .thenByDescending { it.second }
                        .thenByDescending { it.first.updatedAt }
                )
                .map { it.first }
            }
        }
    }

    val displayedNotes: StateFlow<List<NoteEntity>> = combine(
        rawNotesFlow,
        _currentFolder,
        _selectedColorFilter,
        _selectedLabelFilter
    ) { notes, folder, colorFilter, labelFilter ->
        notes.filter { note ->
            val matchesFolder = if (folder == NoteFolder.REMINDERS) {
                note.reminderTimestamp != null && !note.isTrashed && !note.isArchived
            } else true
            val matchesColor = colorFilter == null || note.colorKey.equals(colorFilter, ignoreCase = true)
            val matchesLabel = labelFilter == null || note.getLabels().any { it.equals(labelFilter, ignoreCase = true) }
            matchesFolder && matchesColor && matchesLabel
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // All available labels across all notes & custom labels
    val allLabels: StateFlow<List<String>> = combine(
        repository.getActiveNotes(),
        repository.getArchivedNotes(),
        _customLabels
    ) { active, archived, custom ->
        val set = linkedSetOf<String>()
        custom.forEach { if (it.isNotBlank()) set.add(it) }
        active.forEach { note -> set.addAll(note.getLabels()) }
        archived.forEach { note -> set.addAll(note.getLabels()) }
        set.toList().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ==========================================
    // EDITOR STATE (Live editing in full-screen)
    // ==========================================
    data class EditingNoteState(
        val originalId: Long = 0L,
        val title: String = "",
        val content: String = "",
        val isChecklist: Boolean = false,
        val checklistItems: List<ChecklistItem> = emptyList(),
        val colorKey: String = "default",
        val isPinned: Boolean = false,
        val isArchived: Boolean = false,
        val labels: List<String> = emptyList(),
        val reminderTimestamp: Long? = null,
        val imageUris: List<String> = emptyList(),
        val audioUris: List<String> = emptyList(),
        val createdAt: Long = System.currentTimeMillis(),
        val updatedAt: Long = System.currentTimeMillis()
    )

    private data class UndoSnapshot(
        val title: String,
        val content: String,
        val isChecklist: Boolean,
        val checklistItems: List<ChecklistItem>
    )

    private val undoStack = java.util.ArrayDeque<UndoSnapshot>()
    private val redoStack = java.util.ArrayDeque<UndoSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private fun resetUndoHistory() {
        undoStack.clear()
        redoStack.clear()
        _canUndo.value = false
        _canRedo.value = false
    }

    private fun pushUndoSnapshot() {
        val current = _editingState.value ?: return
        undoStack.push(
            UndoSnapshot(
                title = current.title,
                content = current.content,
                isChecklist = current.isChecklist,
                checklistItems = current.checklistItems.map { it.copy() }
            )
        )
        if (undoStack.size > 25) {
            undoStack.removeLast()
        }
        redoStack.clear()
        _canUndo.value = true
        _canRedo.value = false
    }

    fun undo() {
        val current = _editingState.value ?: return
        if (undoStack.isEmpty()) return
        redoStack.push(
            UndoSnapshot(
                title = current.title,
                content = current.content,
                isChecklist = current.isChecklist,
                checklistItems = current.checklistItems.map { it.copy() }
            )
        )
        val snap = undoStack.pop()
        _editingState.value = current.copy(
            title = snap.title,
            content = snap.content,
            isChecklist = snap.isChecklist,
            checklistItems = snap.checklistItems,
            updatedAt = System.currentTimeMillis()
        )
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = true
        persistCurrentEditorState()
    }

    fun redo() {
        val current = _editingState.value ?: return
        if (redoStack.isEmpty()) return
        undoStack.push(
            UndoSnapshot(
                title = current.title,
                content = current.content,
                isChecklist = current.isChecklist,
                checklistItems = current.checklistItems.map { it.copy() }
            )
        )
        val snap = redoStack.pop()
        _editingState.value = current.copy(
            title = snap.title,
            content = snap.content,
            isChecklist = snap.isChecklist,
            checklistItems = snap.checklistItems,
            updatedAt = System.currentTimeMillis()
        )
        _canUndo.value = true
        _canRedo.value = redoStack.isNotEmpty()
        persistCurrentEditorState()
    }

    private val _editingState = MutableStateFlow<EditingNoteState?>(null)
    val editingState: StateFlow<EditingNoteState?> = _editingState.asStateFlow()

    fun openNewNote(asChecklist: Boolean = false) {
        resetUndoHistory()
        val initialItems = if (asChecklist) listOf(ChecklistItem(text = "", isChecked = false)) else emptyList()
        _editingState.value = EditingNoteState(
            originalId = 0L,
            title = "",
            content = "",
            isChecklist = asChecklist,
            checklistItems = initialItems,
            colorKey = "default",
            isPinned = false
        )
    }

    fun openExistingNote(note: NoteEntity) {
        resetUndoHistory()
        _editingState.value = EditingNoteState(
            originalId = note.id,
            title = note.title,
            content = note.content,
            isChecklist = note.isChecklist,
            checklistItems = note.getChecklistItems(),
            colorKey = note.colorKey,
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            labels = note.getLabels(),
            reminderTimestamp = note.reminderTimestamp,
            imageUris = note.getImageUris(),
            audioUris = note.getAudioUris(),
            createdAt = note.createdAt,
            updatedAt = note.updatedAt
        )
    }

    private fun processAndSaveImage(context: Context, contentUri: android.net.Uri): String? {
        return try {
            val imagesDir = java.io.File(context.filesDir, "keep_images").apply { if (!exists()) mkdirs() }
            val outputFile = java.io.File(imagesDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")

            // 1. Decode bounds only to calculate sample size
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(contentUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            }

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            val maxDim = 2048

            var sampleSize = 1
            if (origWidth > maxDim || origHeight > maxDim) {
                val halfHeight = origHeight / 2
                val halfWidth = origWidth / 2
                while ((halfHeight / sampleSize) >= maxDim && (halfWidth / sampleSize) >= maxDim) {
                    sampleSize *= 2
                }
            }

            // 2. Decode bitmap with inSampleSize
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(contentUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 3. Save as high-quality compressed JPEG (85%)
            java.io.FileOutputStream(outputFile).use { out ->
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun addImageUriToEditor(contentUri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val localPath = processAndSaveImage(context, contentUri) ?: return@launch
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                val current = _editingState.value ?: return@withContext
                val updated = current.imageUris + localPath
                _editingState.value = current.copy(imageUris = updated, updatedAt = System.currentTimeMillis())
                persistCurrentEditorState()
            }
        }
    }

    fun addDrawingToEditor(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val imagesDir = java.io.File(context.filesDir, "keep_images").apply { if (!exists()) mkdirs() }
                val file = java.io.File(imagesDir, "sketch_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.png")
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                }
                val localPath = file.absolutePath
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    val current = _editingState.value ?: return@withContext
                    val updated = current.imageUris + localPath
                    _editingState.value = current.copy(imageUris = updated, updatedAt = System.currentTimeMillis())
                    persistCurrentEditorState()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeImageFromEditor(imageUri: String) {
        val current = _editingState.value ?: return
        val updated = current.imageUris.filter { it != imageUri }
        _editingState.value = current.copy(imageUris = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun updateEditorTitle(title: String) {
        pushUndoSnapshot()
        _editingState.value = _editingState.value?.copy(title = title, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun updateEditorContent(content: String) {
        pushUndoSnapshot()
        _editingState.value = _editingState.value?.copy(content = content, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun toggleEditorPin() {
        val current = _editingState.value ?: return
        _editingState.value = current.copy(isPinned = !current.isPinned, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun updateEditorColor(colorKey: String) {
        _editingState.value = _editingState.value?.copy(colorKey = colorKey, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun toggleChecklistItem(index: Int) {
        val current = _editingState.value ?: return
        if (index !in current.checklistItems.indices) return
        pushUndoSnapshot()
        val updated = current.checklistItems.toMutableList()
        val item = updated[index]
        updated[index] = item.copy(isChecked = !item.isChecked)
        _editingState.value = current.copy(checklistItems = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun updateChecklistItemText(index: Int, text: String) {
        val current = _editingState.value ?: return
        if (index !in current.checklistItems.indices) return
        val updated = current.checklistItems.toMutableList()
        updated[index] = updated[index].copy(text = text)
        _editingState.value = current.copy(checklistItems = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun addChecklistItem(afterIndex: Int? = null, initialText: String = "", customId: String? = null): String {
        val current = _editingState.value ?: return ""
        pushUndoSnapshot()
        val updated = current.checklistItems.toMutableList()
        val itemId = customId ?: UUID.randomUUID().toString()
        val newItem = ChecklistItem(id = itemId, text = initialText, isChecked = false)
        if (afterIndex != null && afterIndex in updated.indices) {
            updated.add(afterIndex + 1, newItem)
        } else {
            updated.add(newItem)
        }
        _editingState.value = current.copy(checklistItems = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
        return itemId
    }

    fun removeChecklistItem(index: Int) {
        val current = _editingState.value ?: return
        if (index !in current.checklistItems.indices) return
        pushUndoSnapshot()
        val updated = current.checklistItems.toMutableList()
        updated.removeAt(index)
        if (updated.isEmpty()) {
            updated.add(ChecklistItem(text = "", isChecked = false))
        }
        _editingState.value = current.copy(checklistItems = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun toggleChecklistMode() {
        val current = _editingState.value ?: return
        pushUndoSnapshot()
        if (current.isChecklist) {
            // Convert checklist to plain text
            val text = current.checklistItems.joinToString("\n") { it.text }
            _editingState.value = current.copy(
                isChecklist = false,
                content = text,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            // Convert plain text to checklist
            val lines = current.content.lines().filter { it.isNotBlank() }
            val items = if (lines.isEmpty()) {
                listOf(ChecklistItem(text = "", isChecked = false))
            } else {
                lines.map { ChecklistItem(text = it, isChecked = false) }
            }
            _editingState.value = current.copy(
                isChecklist = true,
                checklistItems = items,
                updatedAt = System.currentTimeMillis()
            )
        }
        persistCurrentEditorState()
    }

    fun duplicateCurrentNote() {
        val current = _editingState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val copyTitle = if (current.title.isBlank()) "Note (copy)" else "${current.title} (copy)"
            val newNote = NoteEntity(
                title = copyTitle,
                content = current.content,
                isChecklist = current.isChecklist,
                checklistJson = current.checklistItems.map { it.copy(id = UUID.randomUUID().toString()) }.let { items ->
                    val arr = JSONArray()
                    items.forEach { item ->
                        val obj = org.json.JSONObject()
                        obj.put("id", item.id)
                        obj.put("text", item.text)
                        obj.put("isChecked", item.isChecked)
                        arr.put(obj)
                    }
                    arr.toString()
                },
                colorKey = current.colorKey,
                isPinned = false,
                isArchived = false,
                isTrashed = false,
                labelsJson = JSONArray(current.labels).toString(),
                reminderTimestamp = null,
                imageUrisJson = JSONArray(current.imageUris).toString(),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.saveNote(newNote)
            _editingState.value = current.copy(
                originalId = newId,
                title = copyTitle,
                isPinned = false,
                isArchived = false,
                reminderTimestamp = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            resetUndoHistory()
        }
    }

    fun shareCurrentNote(context: Context) {
        val current = _editingState.value ?: return
        val title = current.title.trim()
        val body = if (current.isChecklist) {
            current.checklistItems.joinToString("\n") { item ->
                val box = if (item.isChecked) "☑" else "☐"
                "$box ${item.text}"
            }
        } else {
            current.content.trim()
        }
        val fullText = buildString {
            if (title.isNotEmpty()) {
                appendLine(title)
                appendLine()
            }
            append(body)
        }.trim()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title.ifEmpty { "Shared Note" })
            putExtra(Intent.EXTRA_TEXT, fullText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Share note via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyCurrentNoteToClipboard(context: Context) {
        val current = _editingState.value ?: return
        val title = current.title.trim()
        val body = if (current.isChecklist) {
            current.checklistItems.joinToString("\n") { item ->
                val box = if (item.isChecked) "☑" else "☐"
                "$box ${item.text}"
            }
        } else {
            current.content.trim()
        }
        val fullText = buildString {
            if (title.isNotEmpty()) {
                appendLine(title)
                appendLine()
            }
            append(body)
        }.trim()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Note", fullText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun shareSelectedNotes(context: Context) {
        val selectedIds = _selectedNoteIds.value
        if (selectedIds.isEmpty()) return
        val notes = displayedNotes.value.filter { it.id in selectedIds }
        if (notes.isEmpty()) return

        val fullText = notes.joinToString("\n\n---\n\n") { note ->
            buildString {
                if (note.title.isNotBlank()) {
                    appendLine("📌 ${note.title.trim()}")
                }
                if (note.isChecklist) {
                    val items = note.getChecklistItems()
                    items.forEach { item ->
                        val box = if (item.isChecked) "☑" else "☐"
                        appendLine("$box ${item.text}")
                    }
                } else if (note.content.isNotBlank()) {
                    appendLine(note.content.trim())
                }
            }.trim()
        }.trim()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, if (notes.size == 1) notes.first().title.ifBlank { "Shared Note" } else "${notes.size} Notes")
            putExtra(Intent.EXTRA_TEXT, fullText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Share notes via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copySelectedNotesToClipboard(context: Context) {
        val selectedIds = _selectedNoteIds.value
        if (selectedIds.isEmpty()) return
        val notes = displayedNotes.value.filter { it.id in selectedIds }
        if (notes.isEmpty()) return

        val fullText = notes.joinToString("\n\n---\n\n") { note ->
            buildString {
                if (note.title.isNotBlank()) {
                    appendLine("📌 ${note.title.trim()}")
                }
                if (note.isChecklist) {
                    val items = note.getChecklistItems()
                    items.forEach { item ->
                        val box = if (item.isChecked) "☑" else "☐"
                        appendLine("$box ${item.text}")
                    }
                } else if (note.content.isNotBlank()) {
                    appendLine(note.content.trim())
                }
            }.trim()
        }.trim()

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Notes", fullText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "${notes.size} note${if (notes.size > 1) "s" else ""} copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    // ==========================================
    // AUDIO RECORDING & PLAYBACK
    // ==========================================
    val audioMemoManager = AudioMemoManager(getApplication<Application>())
    val recordingState = audioMemoManager.recordingState
    val playbackState = audioMemoManager.playbackState

    fun startVoiceRecording() {
        audioMemoManager.startRecording()
    }

    fun stopVoiceRecordingAndAttach() {
        val (audioPath, transcript) = audioMemoManager.stopRecording()
        val current = _editingState.value
        if (current == null) {
            // Started voice memo from main screen FAB
            openNewNote(asChecklist = false)
            val updated = _editingState.value ?: return
            val audios = if (audioPath != null) listOf(audioPath) else emptyList()
            _editingState.value = updated.copy(
                content = transcript.trim(),
                audioUris = audios,
                updatedAt = System.currentTimeMillis()
            )
            persistCurrentEditorState()
        } else {
            // In note editor
            val updatedAudios = if (audioPath != null) current.audioUris + audioPath else current.audioUris
            if (current.isChecklist && transcript.isNotBlank()) {
                val newItems = current.checklistItems.toMutableList()
                newItems.add(ChecklistItem(id = UUID.randomUUID().toString(), text = transcript.trim(), isChecked = false))
                _editingState.value = current.copy(
                    checklistItems = newItems,
                    audioUris = updatedAudios,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                val newContent = if (current.content.isBlank()) {
                    transcript.trim()
                } else if (transcript.isNotBlank()) {
                    "${current.content}\n${transcript.trim()}"
                } else {
                    current.content
                }
                _editingState.value = current.copy(
                    content = newContent,
                    audioUris = updatedAudios,
                    updatedAt = System.currentTimeMillis()
                )
            }
            persistCurrentEditorState()
        }
    }

    fun cancelVoiceRecording() {
        audioMemoManager.cancelRecording()
    }

    fun removeAudioFromEditor(audioUri: String) {
        val current = _editingState.value ?: return
        val updated = current.audioUris.filter { it != audioUri }
        _editingState.value = current.copy(audioUris = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
        if (playbackState.value.currentPath == audioUri) {
            audioMemoManager.stopPlayback()
        }
        try {
            val file = java.io.File(audioUri)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }

    fun toggleAudioPlayback(audioUri: String) {
        audioMemoManager.playOrToggle(audioUri)
    }

    fun seekAudio(positionMs: Int) {
        audioMemoManager.seekTo(positionMs)
    }

    fun stopAudioPlayback() {
        audioMemoManager.stopPlayback()
    }

    fun appendVoiceTranscription(transcription: String) {
        val current = _editingState.value ?: return
        if (transcription.isBlank()) return
        pushUndoSnapshot()
        if (current.isChecklist) {
            val updated = current.checklistItems.toMutableList()
            updated.add(ChecklistItem(id = UUID.randomUUID().toString(), text = transcription.trim(), isChecked = false))
            _editingState.value = current.copy(checklistItems = updated, updatedAt = System.currentTimeMillis())
        } else {
            val newContent = if (current.content.isBlank()) {
                transcription.trim()
            } else {
                "${current.content}\n${transcription.trim()}"
            }
            _editingState.value = current.copy(content = newContent, updatedAt = System.currentTimeMillis())
        }
        persistCurrentEditorState()
    }

    fun openNewNoteWithVoice(transcription: String) {
        resetUndoHistory()
        _editingState.value = EditingNoteState(
            originalId = 0L,
            title = "",
            content = transcription.trim(),
            isChecklist = false,
            colorKey = "default",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    fun addCapturedPhoto(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val imagesDir = java.io.File(context.filesDir, "keep_images").apply { if (!exists()) mkdirs() }
                val file = java.io.File(imagesDir, "photo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                val localPath = file.absolutePath
                val current = _editingState.value
                if (current != null) {
                    val updated = current.imageUris + localPath
                    _editingState.value = current.copy(imageUris = updated, updatedAt = System.currentTimeMillis())
                    persistCurrentEditorState()
                } else {
                    _editingState.value = EditingNoteState(
                        originalId = 0L,
                        title = "",
                        content = "",
                        imageUris = listOf(localPath),
                        colorKey = "default",
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun openNewNoteWithPhoto(bitmap: android.graphics.Bitmap) {
        openNewNote(asChecklist = false)
        addCapturedPhoto(bitmap)
    }

    fun addLabelToEditor(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return
        val current = _editingState.value ?: return
        if (!current.labels.contains(trimmed)) {
            val updated = current.labels + trimmed
            _editingState.value = current.copy(labels = updated, updatedAt = System.currentTimeMillis())
            persistCurrentEditorState()
        }
    }

    fun removeLabelFromEditor(label: String) {
        val current = _editingState.value ?: return
        val updated = current.labels.filter { it != label }
        _editingState.value = current.copy(labels = updated, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    fun setEditorReminder(timestamp: Long?) {
        val current = _editingState.value ?: return
        _editingState.value = current.copy(reminderTimestamp = timestamp, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()

        if (current.originalId != 0L) {
            if (timestamp != null && timestamp > System.currentTimeMillis()) {
                scheduleReminderNotification(current.originalId, current.title, current.content, timestamp)
            } else {
                cancelReminderNotification(current.originalId)
            }
        }
    }

    fun moveChecklistItem(fromIndex: Int, toIndex: Int) {
        val current = _editingState.value ?: return
        val items = current.checklistItems.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) return
        val item = items.removeAt(fromIndex)
        items.add(toIndex, item)
        _editingState.value = current.copy(checklistItems = items, updatedAt = System.currentTimeMillis())
        persistCurrentEditorState()
    }

    private fun scheduleReminderNotification(id: Long, title: String, content: String, timeMillis: Long) {
        try {
            val intent = Intent(getApplication(), NoteReminderReceiver::class.java).apply {
                putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, id)
                putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, title)
                putExtra(NoteReminderReceiver.EXTRA_NOTE_CONTENT, content)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent)
            }
        } catch (_: Exception) {
        }
    }

    private fun cancelReminderNotification(id: Long) {
        try {
            val intent = Intent(getApplication(), NoteReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                getApplication(),
                id.toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    fun restoreNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFromTrash(note.id)
        }
    }

    fun unarchiveNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setArchived(note.id, false)
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePermanently(note)
            cancelReminderNotification(note.id)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash()
        }
    }

    fun deleteCurrentNote() {
        val current = _editingState.value ?: return
        if (current.originalId != 0L) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.moveToTrash(current.originalId)
            }
        }
        _editingState.value = null
    }

    fun archiveCurrentNote() {
        val current = _editingState.value ?: return
        if (current.originalId != 0L) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.setArchived(current.originalId, true)
            }
        }
        _editingState.value = null
    }

    fun openNewNoteWithDrawing(bitmap: android.graphics.Bitmap) {
        openNewNote(asChecklist = false)
        addDrawingToEditor(bitmap)
    }

    fun openNewNoteWithImage(uri: android.net.Uri) {
        openNewNote(asChecklist = false)
        addImageUriToEditor(uri)
    }

    fun closeEditor() {
        val current = _editingState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val isEmpty = current.title.isBlank() &&
                    current.imageUris.isEmpty() &&
                    current.audioUris.isEmpty() &&
                    ((!current.isChecklist && current.content.isBlank()) ||
                            (current.isChecklist && current.checklistItems.none { it.text.isNotBlank() }))

            if (isEmpty) {
                if (current.originalId != 0L) {
                    val entity = buildEntityFromState(current)
                    repository.deletePermanently(entity)
                }
            } else {
                val entity = buildEntityFromState(current)
                repository.saveNote(entity)
            }
        }
        _editingState.value = null
    }

    private fun persistCurrentEditorState() {
        val current = _editingState.value ?: return
        val isEmpty = current.title.isBlank() &&
                current.imageUris.isEmpty() &&
                current.audioUris.isEmpty() &&
                ((!current.isChecklist && current.content.isBlank()) ||
                        (current.isChecklist && current.checklistItems.none { it.text.isNotBlank() }))
        if (isEmpty && current.originalId == 0L) return

        viewModelScope.launch(Dispatchers.IO) {
            val entity = buildEntityFromState(current)
            val savedId = repository.saveNote(entity)
            if (current.originalId == 0L && savedId != 0L) {
                _editingState.value = _editingState.value?.copy(originalId = savedId)
            }
        }
    }

    private fun buildEntityFromState(state: EditingNoteState): NoteEntity {
        val labelsArray = JSONArray()
        state.labels.forEach { labelsArray.put(it) }

        val imagesArray = JSONArray()
        state.imageUris.forEach { imagesArray.put(it) }

        val audiosArray = JSONArray()
        state.audioUris.forEach { audiosArray.put(it) }

        return NoteEntity(
            id = state.originalId,
            title = state.title.trim(),
            content = state.content,
            isChecklist = state.isChecklist,
            checklistJson = if (state.isChecklist) ChecklistItem.listToJson(state.checklistItems) else "[]",
            colorKey = state.colorKey,
            isPinned = state.isPinned,
            isArchived = state.isArchived,
            isTrashed = false,
            labelsJson = labelsArray.toString(),
            reminderTimestamp = state.reminderTimestamp,
            imageUrisJson = imagesArray.toString(),
            audioUrisJson = audiosArray.toString(),
            createdAt = state.createdAt,
            updatedAt = System.currentTimeMillis()
        )
    }

    // Direct card quick actions
    fun togglePin(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.togglePin(note.id, note.isPinned)
        }
    }

    fun archiveNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setArchived(note.id, true)
        }
    }

    fun trashNote(note: NoteEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrash(note.id)
        }
    }

    fun setNoteColor(note: NoteEntity, colorKey: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setColor(note.id, colorKey)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioMemoManager.release()
    }
}
