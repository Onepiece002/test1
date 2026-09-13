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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeepBottomDock(
    onTakeNoteClick: () -> Unit,
    onNewChecklistClick: () -> Unit,
    onNewDrawingClick: () -> Unit,
    onNewImageClick: () -> Unit,
    modifier: Modifier = Modifier,
    onNewAudioClick: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    // Modern Google Keep Material 3 surface colors
    val dockBg = if (isDark) Color(0xFF1E2023) else Color(0xFFFFFFFF)
    val borderColor = if (isDark) Color(0xFF32353A) else Color(0xFFE0E2E6)
    val placeholderColor = if (isDark) Color(0xFF9AA0A6) else Color(0xFF5F6368)
    val iconColor = if (isDark) Color(0xFFE2E3E5) else Color(0xFF444746)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.25f),
                spotColor = Color.Black.copy(alpha = 0.18f)
            ),
        shape = CircleShape,
        color = dockBg,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // "Take a note..." clickable pill area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTakeNoteClick)
                    .padding(start = 18.dp, end = 8.dp)
                    .testTag("keep_take_note_button"),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Take a note...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    ),
                    color = placeholderColor
                )
            }

            // Quick Checklist Action (☑)
            IconButton(
                onClick = onNewChecklistClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("keep_new_checklist_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckBox,
                    contentDescription = "New checklist",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Quick Drawing Action (🖌️)
            IconButton(
                onClick = onNewDrawingClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("keep_new_drawing_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Brush,
                    contentDescription = "New drawing",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Quick Image Note Action (🖼️)
            IconButton(
                onClick = onNewImageClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("keep_new_image_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "New image note",
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
