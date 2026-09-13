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

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DrawingPath(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepSketchDialog(
    onDismiss: () -> Unit,
    onSaveDrawing: (Bitmap) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val paths = remember { mutableStateListOf<DrawingPath>() }
    var currentPoints = remember { mutableStateListOf<Offset>() }

    val palette = remember {
        listOf(
            Color(0xFF202124), // Black/Dark
            Color(0xFFD93025), // Red
            Color(0xFFEA4335), // Coral
            Color(0xFFFBBC04), // Yellow
            Color(0xFF34A853), // Green
            Color(0xFF1A73E8), // Google Blue
            Color(0xFF9334E6), // Purple
            Color(0xFFE37400), // Orange
            Color(0xFF00ACC1), // Cyan
            Color(0xFF795548), // Brown
            Color(0xFFFFFFFF)  // White
        )
    }

    var selectedColor by remember { mutableStateOf(if (isDark) Color(0xFFE8EAED) else Color(0xFF202124)) }
    var selectedStrokeWidth by remember { mutableFloatStateOf(8f) }
    var canvasSize by remember { mutableStateOf(IntSize(1080, 1440)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isDark) Color(0xFF1E1F22) else Color(0xFFF8F9FA)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "Drawing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("sketch_back_button")) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Discard drawing"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (paths.isNotEmpty()) paths.removeAt(paths.size - 1)
                            },
                            enabled = paths.isNotEmpty(),
                            modifier = Modifier.testTag("sketch_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (paths.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        IconButton(
                            onClick = { paths.clear() },
                            enabled = paths.isNotEmpty(),
                            modifier = Modifier.testTag("sketch_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Clear all",
                                tint = if (paths.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }

                        IconButton(
                            onClick = {
                                val w = if (canvasSize.width > 0) canvasSize.width else 1080
                                val h = if (canvasSize.height > 0) canvasSize.height else 1440
                                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                val canvas = android.graphics.Canvas(bitmap)
                                // Background
                                canvas.drawColor(if (isDark) android.graphics.Color.rgb(32, 33, 36) else android.graphics.Color.WHITE)

                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    style = Paint.Style.STROKE
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                }

                                for (item in paths) {
                                    if (item.points.size > 1) {
                                        paint.color = item.color.toArgb()
                                        paint.strokeWidth = item.strokeWidth
                                        val androidPath = AndroidPath()
                                        androidPath.moveTo(item.points[0].x, item.points[0].y)
                                        for (i in 1 until item.points.size) {
                                            androidPath.lineTo(item.points[i].x, item.points[i].y)
                                        }
                                        canvas.drawPath(androidPath, paint)
                                    }
                                }

                                onSaveDrawing(bitmap)
                            },
                            enabled = paths.isNotEmpty(),
                            modifier = Modifier.testTag("sketch_save_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Save drawing",
                                tint = if (paths.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (isDark) Color(0xFF282A2E) else Color(0xFFFFFFFF)
                    )
                )

                // White/Dark Drawing Sheet Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp))
                        .background(if (isDark) Color(0xFF202124) else Color(0xFFFFFFFF))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(selectedColor, selectedStrokeWidth) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentPoints.clear()
                                    currentPoints.add(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentPoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (currentPoints.isNotEmpty()) {
                                        paths.add(
                                            DrawingPath(
                                                points = currentPoints.toList(),
                                                color = selectedColor,
                                                strokeWidth = selectedStrokeWidth
                                            )
                                        )
                                        currentPoints.clear()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw completed paths
                        for (pathData in paths) {
                            if (pathData.points.size > 1) {
                                val composePath = Path().apply {
                                    moveTo(pathData.points[0].x, pathData.points[0].y)
                                    for (i in 1 until pathData.points.size) {
                                        lineTo(pathData.points[i].x, pathData.points[i].y)
                                    }
                                }
                                drawPath(
                                    path = composePath,
                                    color = pathData.color,
                                    style = Stroke(
                                        width = pathData.strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // Draw active current stroke
                        if (currentPoints.size > 1) {
                            val activePath = Path().apply {
                                moveTo(currentPoints[0].x, currentPoints[0].y)
                                for (i in 1 until currentPoints.size) {
                                    lineTo(currentPoints[i].x, currentPoints[i].y)
                                }
                            }
                            drawPath(
                                path = activePath,
                                color = selectedColor,
                                style = Stroke(
                                    width = selectedStrokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    if (paths.isEmpty() && currentPoints.isEmpty()) {
                        Text(
                            text = "Draw anywhere with your finger or stylus",
                            style = MaterialTheme.typography.bodyMedium,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.25f),
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                // Bottom Tool Bar: Stroke Widths + Color Swatches
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDark) Color(0xFF282A2E) else Color(0xFFFFFFFF),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // Stroke Width Pickers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Stroke",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val strokeOptions = listOf(
                                Pair(4f, "Fine"),
                                Pair(9f, "Medium"),
                                Pair(18f, "Bold"),
                                Pair(32f, "Chisel")
                            )

                            strokeOptions.forEach { (width, label) ->
                                val isSelected = selectedStrokeWidth == width
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { selectedStrokeWidth = width }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size((width / 2).coerceIn(4f, 14f).dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Color Palette Swatches
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(palette) { color ->
                                val isSelected = selectedColor == color
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected color",
                                            tint = if (color == Color.White || color == Color(0xFFFBBC04)) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
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
