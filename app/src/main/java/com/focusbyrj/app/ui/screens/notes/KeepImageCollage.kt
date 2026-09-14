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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Google Keep Image Collage for Note Cards
 * - 1 image: full-width hero header (height 180dp)
 * - 2 images: 2 columns side by side (height 130dp, 2dp gap)
 * - 3 images: top 1 full width (height 130dp), bottom 2 columns (height 95dp, 2dp gap)
 * - 4 images: 2x2 grid (height 100dp each row, 2dp gap)
 * - 5+ images: 2x2 grid with +N badge overlay on the 4th item
 */
@Composable
fun KeepCardImageCollage(
    imageUris: List<String>,
    modifier: Modifier = Modifier
) {
    if (imageUris.isEmpty()) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
    ) {
        when (imageUris.size) {
            1 -> {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(imageUris[0])
                        .crossfade(true)
                        .build(),
                    contentDescription = "Note attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 220.dp),
                    contentScale = ContentScale.Crop
                )
            }
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUris[0])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Note attachment 1",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUris[1])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Note attachment 2",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            3 -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(125.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUris[0])
                                .crossfade(true)
                                .build(),
                            contentDescription = "Note attachment 1",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[1])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 2",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[2])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 3",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }
            else -> {
                // 4 or more images: 2x2 grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[0])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 1",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[1])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 2",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(95.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[2])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 3",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUris[3])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Note attachment 4",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            if (imageUris.size > 4) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "+${imageUris.size - 3}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Google Keep Image Collage for Note Editor
 * Shows all attached images at top of note with full interactive preview and remove buttons
 */
@Composable
fun KeepEditorImageCollage(
    imageUris: List<String>,
    onImageClick: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageUris.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (imageUris.size) {
            1 -> {
                EditorImageTile(
                    uri = imageUris[0],
                    height = 220.dp,
                    onImageClick = onImageClick,
                    onRemoveImage = onRemoveImage,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            2 -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EditorImageTile(
                        uri = imageUris[0],
                        height = 160.dp,
                        onImageClick = onImageClick,
                        onRemoveImage = onRemoveImage,
                        modifier = Modifier.weight(1f)
                    )
                    EditorImageTile(
                        uri = imageUris[1],
                        height = 160.dp,
                        onImageClick = onImageClick,
                        onRemoveImage = onRemoveImage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            3 -> {
                EditorImageTile(
                    uri = imageUris[0],
                    height = 175.dp,
                    onImageClick = onImageClick,
                    onRemoveImage = onRemoveImage,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EditorImageTile(
                        uri = imageUris[1],
                        height = 135.dp,
                        onImageClick = onImageClick,
                        onRemoveImage = onRemoveImage,
                        modifier = Modifier.weight(1f)
                    )
                    EditorImageTile(
                        uri = imageUris[2],
                        height = 135.dp,
                        onImageClick = onImageClick,
                        onRemoveImage = onRemoveImage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                // Multiples of 2
                val pairs = imageUris.chunked(2)
                pairs.forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EditorImageTile(
                            uri = chunk[0],
                            height = 140.dp,
                            onImageClick = onImageClick,
                            onRemoveImage = onRemoveImage,
                            modifier = Modifier.weight(1f)
                        )
                        if (chunk.size > 1) {
                            EditorImageTile(
                                uri = chunk[1],
                                height = 140.dp,
                                onImageClick = onImageClick,
                                onRemoveImage = onRemoveImage,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorImageTile(
    uri: String,
    height: androidx.compose.ui.unit.Dp,
    onImageClick: (String) -> Unit,
    onRemoveImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onImageClick(uri) }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri)
                .crossfade(true)
                .build(),
            contentDescription = "Attached image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Google Keep Translucent Remove Action Button
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onRemoveImage(uri) },
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.65f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Remove image",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}
