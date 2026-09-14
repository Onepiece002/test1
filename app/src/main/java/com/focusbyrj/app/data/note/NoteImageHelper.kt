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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object NoteImageHelper {

    fun processAndSaveImage(context: Context, contentUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "keep_images").apply { if (!exists()) mkdirs() }
            val outputFile = File(imagesDir, "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")

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
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.contentResolver.openInputStream(contentUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 3. Save as high-quality compressed JPEG (85%)
            FileOutputStream(outputFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            bitmap.recycle()

            outputFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun processAndSaveMultipleImages(context: Context, uris: List<Uri>): List<String> {
        val paths = mutableListOf<String>()
        for (uri in uris) {
            val savedPath = processAndSaveImage(context, uri)
            if (savedPath != null) {
                paths.add(savedPath)
            }
        }
        return paths
    }
}
