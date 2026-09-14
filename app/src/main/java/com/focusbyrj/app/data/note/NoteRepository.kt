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

package com.focusbyrj.app.data.note

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val noteDao: NoteDao) {

    fun getActiveNotes(): Flow<List<NoteEntity>> = noteDao.getAllActiveNotes()

    fun getArchivedNotes(): Flow<List<NoteEntity>> = noteDao.getArchivedNotes()

    fun getTrashedNotes(): Flow<List<NoteEntity>> = noteDao.getTrashedNotes()

    fun searchNotes(query: String): Flow<List<NoteEntity>> = noteDao.searchNotes(query)

    fun getNoteById(id: Long): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteByIdSync(id: Long): NoteEntity? = noteDao.getNoteByIdSync(id)

    suspend fun saveNote(note: NoteEntity): Long {
        return if (note.id == 0L) {
            noteDao.insertNote(note)
        } else {
            noteDao.updateNote(note)
            note.id
        }
    }

    suspend fun updateNoteOrder(id: Long, updatedAt: Long) {
        noteDao.updateNoteOrder(id, updatedAt)
    }

    suspend fun deletePermanently(note: NoteEntity) {
        noteDao.deleteNote(note)
    }

    suspend fun togglePin(id: Long, currentPinned: Boolean) {
        noteDao.updatePinStatus(id, !currentPinned)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        noteDao.updateArchiveStatus(id, isArchived)
    }

    suspend fun moveToTrash(id: Long) {
        noteDao.updateTrashStatus(id, true)
    }

    suspend fun restoreFromTrash(id: Long) {
        noteDao.updateTrashStatus(id, false)
    }

    suspend fun setColor(id: Long, colorKey: String) {
        noteDao.updateColor(id, colorKey)
    }

    suspend fun getTrashedNotesSync(): List<NoteEntity> = noteDao.getTrashedNotesSync()

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun renameLabel(oldLabel: String, newLabel: String) {
        val allNotes = noteDao.getAllNotesList()
        allNotes.forEach { note ->
            val labels = note.getLabels().toMutableList()
            val index = labels.indexOfFirst { it.equals(oldLabel, ignoreCase = true) }
            if (index != -1) {
                labels[index] = newLabel.trim()
                val array = org.json.JSONArray()
                labels.distinct().forEach { array.put(it) }
                noteDao.updateNote(note.copy(labelsJson = array.toString(), updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun deleteLabel(label: String) {
        val allNotes = noteDao.getAllNotesList()
        allNotes.forEach { note ->
            val labels = note.getLabels().toMutableList()
            val removed = labels.removeAll { it.equals(label, ignoreCase = true) }
            if (removed) {
                val array = org.json.JSONArray()
                labels.distinct().forEach { array.put(it) }
                noteDao.updateNote(note.copy(labelsJson = array.toString(), updatedAt = System.currentTimeMillis()))
            }
        }
    }
}
