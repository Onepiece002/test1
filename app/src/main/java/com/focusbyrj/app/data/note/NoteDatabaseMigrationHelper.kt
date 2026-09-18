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

import android.content.Context
import android.util.Log
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream

/**
 * Handles seamless one-time migration from an unencrypted Room database to an encrypted SQLCipher database.
 */
object NoteDatabaseMigrationHelper {

    private const val TAG = "NoteDbMigration"
    private const val PLAINTEXT_DB_NAME = "keep_notes.db"
    private const val ENCRYPTED_DB_NAME = "keep_notes_vault.db"
    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun getEncryptedDatabaseName(): String = ENCRYPTED_DB_NAME

    /**
     * Checks if a legacy unencrypted database exists and migrates its notes to the encrypted database.
     */
    fun checkAndMigrateIfLegacyPlaintextExists(context: Context, encryptedDb: NoteDatabase) {
        try {
            val dbFile = context.getDatabasePath(PLAINTEXT_DB_NAME)
            if (!dbFile.exists() || dbFile.length() == 0L) {
                return
            }

            // Check if file starts with plaintext SQLite header
            if (!isPlaintextSqliteFile(dbFile)) {
                return
            }

            Log.i(TAG, "Legacy unencrypted notes database detected. Starting migration to SQLCipher vault...")

            // Open temporary unencrypted Room instance to extract notes
            val legacyDb = Room.databaseBuilder(
                context.applicationContext,
                NoteDatabase::class.java,
                PLAINTEXT_DB_NAME
            ).build()

            // Read legacy notes on IO dispatcher safely
            val allLegacyNotes = runBlocking(Dispatchers.IO) {
                try {
                    legacyDb.noteDao().getAllNotesList()
                } catch (_: Exception) {
                    val activeNotes = legacyDb.noteDao().getAllActiveNotes().first()
                    val archivedNotes = legacyDb.noteDao().getArchivedNotes().first()
                    val trashedNotes = legacyDb.noteDao().getTrashedNotes().first()
                    (activeNotes + archivedNotes + trashedNotes).distinctBy { it.id }
                }
            }

            if (allLegacyNotes.isNotEmpty()) {
                runBlocking(Dispatchers.IO) {
                    for (note in allLegacyNotes) {
                        encryptedDb.noteDao().insertNote(note)
                    }
                }
                Log.i(TAG, "Successfully migrated ${allLegacyNotes.size} legacy notes into encrypted SQLCipher database.")
            }

            legacyDb.close()

            // Securely wipe and delete legacy plaintext db files
            val walFile = context.getDatabasePath("${PLAINTEXT_DB_NAME}-wal")
            val shmFile = context.getDatabasePath("${PLAINTEXT_DB_NAME}-shm")
            secureWipeAndDelete(dbFile)
            secureWipeAndDelete(walFile)
            secureWipeAndDelete(shmFile)

            Log.i(TAG, "Plaintext database secure zero-wipe and cleanup completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during plaintext to encrypted notes migration", e)
        }
    }

    /**
     * Overwrites file contents with zeros before unlinking/deleting, preventing forensic recovery.
     */
    private fun secureWipeAndDelete(file: File) {
        if (!file.exists()) return
        try {
            val length = file.length()
            if (length > 0) {
                java.io.RandomAccessFile(file, "rws").use { raf ->
                    val buffer = ByteArray(4096)
                    var remaining = length
                    while (remaining > 0) {
                        val toWrite = minOf(remaining, buffer.size.toLong()).toInt()
                        raf.write(buffer, 0, toWrite)
                        remaining -= toWrite
                    }
                    raf.fd.sync()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to zero-overwrite file ${file.name} before deletion", e)
        } finally {
            file.delete()
        }
    }

    private fun isPlaintextSqliteFile(file: File): Boolean {
        if (!file.exists() || file.length() < 16) return false
        try {
            FileInputStream(file).use { input ->
                val buffer = ByteArray(16)
                val read = input.read(buffer)
                if (read == 16) {
                    return buffer.contentEquals(SQLITE_HEADER)
                }
            }
        } catch (_: Exception) {}
        return false
    }
}
