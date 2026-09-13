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

            runBlocking {
                val activeNotes = legacyDb.noteDao().getAllActiveNotes().first()
                val archivedNotes = legacyDb.noteDao().getArchivedNotes().first()
                val trashedNotes = legacyDb.noteDao().getTrashedNotes().first()

                val allLegacyNotes = (activeNotes + archivedNotes + trashedNotes).distinctBy { it.id }

                if (allLegacyNotes.isNotEmpty()) {
                    for (note in allLegacyNotes) {
                        encryptedDb.noteDao().insertNote(note)
                    }
                    Log.i(TAG, "Successfully migrated ${allLegacyNotes.size} legacy notes into encrypted SQLCipher database.")
                }
            }

            legacyDb.close()

            // Rename or delete plaintext db files safely
            val backupFile = context.getDatabasePath("${PLAINTEXT_DB_NAME}.migrated")
            if (backupFile.exists()) backupFile.delete()
            dbFile.renameTo(backupFile)
            context.getDatabasePath("${PLAINTEXT_DB_NAME}-wal").delete()
            context.getDatabasePath("${PLAINTEXT_DB_NAME}-shm").delete()
            backupFile.delete()

            Log.i(TAG, "Plaintext database cleanup completed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error during plaintext to encrypted notes migration", e)
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
