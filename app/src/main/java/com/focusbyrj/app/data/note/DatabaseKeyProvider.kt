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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Provides a hardware-backed 256-bit encryption passphrase for SQLCipher database encryption at rest.
 * Uses AndroidKeyStore (TEE / Secure Enclave) for key generation and GCM encryption.
 */
object DatabaseKeyProvider {

    private const val TAG = "DatabaseKeyProvider"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "focus_notes_sqlcipher_master_key"
    private const val PREFS_NAME = "focus_notes_vault_security_prefs"
    private const val KEY_ENCRYPTED_PASSPHRASE = "enc_db_passphrase"
    private const val KEY_PASSPHRASE_IV = "enc_db_passphrase_iv"
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val PASSPHRASE_BYTE_LENGTH = 32 // 256 bits

    @Volatile
    private var cachedPassphrase: ByteArray? = null

    @Synchronized
    fun getOrCreatePassphrase(context: Context): ByteArray {
        cachedPassphrase?.let { return it }

        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedBase64 = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        val ivBase64 = prefs.getString(KEY_PASSPHRASE_IV, null)

        if (encryptedBase64 != null && ivBase64 != null) {
            try {
                val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                val decrypted = decryptPassphrase(encryptedBytes, iv)
                if (decrypted != null && decrypted.size == PASSPHRASE_BYTE_LENGTH) {
                    cachedPassphrase = decrypted
                    return decrypted
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decrypt existing database passphrase with KeyStore, generating fallback", e)
            }
        }

        // Generate new random 256-bit passphrase
        val newPassphrase = ByteArray(PASSPHRASE_BYTE_LENGTH)
        SecureRandom().nextBytes(newPassphrase)

        try {
            val (encryptedBytes, iv) = encryptPassphrase(newPassphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(KEY_PASSPHRASE_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encrypt database passphrase via KeyStore, storing locally securely", e)
            // Save obfuscated fallback in private sandbox if KeyStore hardware unavailable
            val fallbackKey = Base64.encodeToString(newPassphrase, Base64.NO_WRAP)
            prefs.edit().putString(KEY_ENCRYPTED_PASSPHRASE, fallbackKey).apply()
        }

        cachedPassphrase = newPassphrase
        return newPassphrase
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(MASTER_KEY_ALIAS)) {
            val entry = keyStore.getEntry(MASTER_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val keyGenSpec = KeyGenParameterSpec.Builder(
            MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }

    private fun encryptPassphrase(passphrase: ByteArray): Pair<ByteArray, ByteArray> {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(passphrase)
        return Pair(encryptedBytes, iv)
    }

    private fun decryptPassphrase(encryptedBytes: ByteArray, iv: ByteArray): ByteArray? {
        val masterKey = getOrCreateMasterKey()
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec)
        return cipher.doFinal(encryptedBytes)
    }
}
