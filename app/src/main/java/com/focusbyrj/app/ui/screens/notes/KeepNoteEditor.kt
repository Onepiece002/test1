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

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeepNoteEditor(
    state: NotesViewModel.EditingNoteState,
    allLabels: List<String>,
    canUndo: Boolean = false,
    canRedo: Boolean = false,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onTogglePin: () -> Unit,
    onColorChange: (String) -> Unit,
    onToggleChecklistMode: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onUpdateChecklistItemText: (Int, String) -> Unit,
    onAddChecklistItem: (Int?, String, String?) -> Unit = { _, _, _ -> },
    onRemoveChecklistItem: (Int) -> Unit,
    onMoveChecklistItem: (Int, Int) -> Unit,
    onSetReminder: (Long?) -> Unit,
    onAddImageUri: (Uri) -> Unit,
    onAddDrawing: (Bitmap) -> Unit,
    onRemoveImage: (String) -> Unit,
    onAddLabel: (String) -> Unit,
    onRemoveLabel: (String) -> Unit,
    onToggleLabel: (String) -> Unit,
    onCreateAndAddLabel: (String) -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onUndo: () -> Unit = {},
    onRedo: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    onShare: () -> Unit = {},
    onCopyText: () -> Unit = {},
    onVoiceInput: (String) -> Unit = {},
    onPhotoTaken: (Bitmap) -> Unit = {},
    onStartVoiceRecording: () -> Unit = {},
    activePlayingAudioPath: String? = null,
    isAudioPlaying: Boolean = false,
    audioPositionMs: Int = 0,
    audioDurationMs: Int = 0,
    audioPlaybackSpeed: Float = 1.0f,
    onToggleAudioPlay: (String) -> Unit = {},
    onSeekAudio: (Int) -> Unit = {},
    onSetAudioPlaybackSpeed: (Float) -> Unit = {},
    onSkipAudio: (Int) -> Unit = {},
    onRemoveAudio: (String) -> Unit = {},
    onClose: () -> Unit
) {
    val context = LocalContext.current

    BackHandler {
        onClose()
    }

    val isDark = isSystemInDarkTheme()
    val theme = KeepColorPalette.getColor(state.colorKey)
    val bgColor = theme.resolveBackgroundColor(isDark)
    val textColor = theme.resolveTextColor(isDark)
    val borderColor = theme.resolveBorderColor(isDark)

    var showColorPicker by remember { mutableStateOf(false) }
    var showLabelDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showSketchDialog by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var viewingImageUri by remember { mutableStateOf<String?>(null) }
    var completedExpanded by remember { mutableStateOf(true) }
    var targetFocusItemId by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                onAddImageUri(uri)
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                onPhotoTaken(bitmap)
            }
        }
    )

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!matches.isNullOrEmpty()) {
                    onVoiceInput(matches[0])
                }
            }
        }
    )

    fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to note...")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Speech recognition unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    val scrollState = rememberScrollState()

    val formattedTime = remember(state.updatedAt) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(state.updatedAt))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Google Keep style background illustration theme
        if (theme.isIllustratedTheme) {
            KeepThemeIllustration(
                themeType = theme.themeType,
                isDark = isDark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 60.dp, end = 12.dp)
                    .size(width = 175.dp, height = 145.dp)
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // TOP ACTION BAR (Google Keep Style)
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("editor_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Save and Back",
                        tint = textColor
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin Note
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.testTag("editor_pin_button")
                    ) {
                        Icon(
                            imageVector = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (state.isPinned) "Unpin" else "Pin",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary else textColor
                        )
                    }

                    // Reminder
                    IconButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.testTag("editor_reminder_button")
                    ) {
                        Icon(
                            imageVector = if (state.reminderTimestamp != null) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = if (state.reminderTimestamp != null) "Edit reminder" else "Add reminder",
                            tint = if (state.reminderTimestamp != null) MaterialTheme.colorScheme.primary else textColor
                        )
                    }

                    // Archive
                    IconButton(
                        onClick = onArchive,
                        modifier = Modifier.testTag("editor_archive_button")
                    ) {
                        Icon(
                            imageVector = if (state.isArchived) Icons.Filled.Archive else Icons.Outlined.Archive,
                            contentDescription = "Archive",
                            tint = textColor
                        )
                    }
                }
            }

            // ==========================================
            // SCROLLABLE NOTE BODY
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // ATTACHED IMAGES (Google Keep style image collage)
                if (state.imageUris.isNotEmpty()) {
                    KeepEditorImageCollage(
                        imageUris = state.imageUris,
                        onImageClick = { uri -> viewingImageUri = uri },
                        onRemoveImage = onRemoveImage
                    )
                }

                // AUDIO ATTACHMENTS
                if (state.audioUris.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        state.audioUris.forEach { audioUri ->
                            val isPlaying = isAudioPlaying && activePlayingAudioPath == audioUri
                            val curPos = if (isPlaying) audioPositionMs else 0
                            val dur = if (isPlaying) audioDurationMs else 0
                            AudioPlayerEditorItem(
                                audioUri = audioUri,
                                isPlaying = isPlaying,
                                currentPositionMs = curPos,
                                durationMs = dur,
                                playbackSpeed = audioPlaybackSpeed,
                                onTogglePlay = { onToggleAudioPlay(audioUri) },
                                onSeek = onSeekAudio,
                                onSpeedChange = onSetAudioPlaybackSpeed,
                                onSkip = onSkipAudio,
                                onDelete = { onRemoveAudio(audioUri) },
                                textColor = textColor
                            )
                        }
                    }
                }

                // TITLE INPUT
                BasicTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_title_input"),
                    textStyle = TextStyle(
                        color = textColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    cursorBrush = SolidColor(textColor),
                    decorationBox = { innerTextField ->
                        if (state.title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = TextStyle(
                                    color = textColor.copy(alpha = 0.40f),
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // NOTE CONTENT OR CHECKLIST
                if (!state.isChecklist) {
                    // Plain Text Note
                    BasicTextField(
                        value = state.content,
                        onValueChange = onContentChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .testTag("editor_content_input"),
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 16.sp,
                            lineHeight = 24.sp
                        ),
                        cursorBrush = SolidColor(textColor),
                        decorationBox = { innerTextField ->
                            if (state.content.isEmpty()) {
                                Text(
                                    text = "Note",
                                    style = TextStyle(
                                        color = textColor.copy(alpha = 0.40f),
                                        fontSize = 16.sp,
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                } else {
                    // Checklist Items
                    val uncompletedItems = remember(state.checklistItems) {
                        state.checklistItems.mapIndexedNotNull { index, item ->
                            if (!item.isChecked) Pair(index, item) else null
                        }
                    }
                    val completedItems = remember(state.checklistItems) {
                        state.checklistItems.mapIndexedNotNull { index, item ->
                            if (item.isChecked) Pair(index, item) else null
                        }
                    }
                    val completedCount = completedItems.size

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Uncompleted items
                        uncompletedItems.forEachIndexed { posInList, (globalIndex, item) ->
                            ChecklistRow(
                                item = item,
                                textColor = textColor,
                                canMoveUp = posInList > 0,
                                canMoveDown = posInList < uncompletedItems.size - 1,
                                isTargetFocus = item.id == targetFocusItemId,
                                onFocused = { if (targetFocusItemId == item.id) targetFocusItemId = null },
                                onToggle = { onToggleChecklistItem(globalIndex) },
                                onTextChange = { onUpdateChecklistItemText(globalIndex, it) },
                                onEnterPressed = { extraText ->
                                    val newId = java.util.UUID.randomUUID().toString()
                                    targetFocusItemId = newId
                                    onAddChecklistItem(globalIndex, extraText, newId)
                                },
                                onDelete = { onRemoveChecklistItem(globalIndex) },
                                onMoveUp = {
                                    if (posInList > 0) {
                                        val targetGlobalIndex = uncompletedItems[posInList - 1].first
                                        onMoveChecklistItem(globalIndex, targetGlobalIndex)
                                    }
                                },
                                onMoveDown = {
                                    if (posInList < uncompletedItems.size - 1) {
                                        val targetGlobalIndex = uncompletedItems[posInList + 1].first
                                        onMoveChecklistItem(globalIndex, targetGlobalIndex)
                                    }
                                }
                            )
                        }

                        // Add new list item button row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val newId = java.util.UUID.randomUUID().toString()
                                    targetFocusItemId = newId
                                    val lastUncompletedGlobalIndex = uncompletedItems.lastOrNull()?.first
                                    onAddChecklistItem(lastUncompletedGlobalIndex, "", newId)
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add item",
                                tint = textColor.copy(alpha = 0.55f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "List item",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = textColor.copy(alpha = 0.55f)
                            )
                        }

                        // Collapsible Completed Items Section
                        if (completedCount > 0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = textColor.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { completedExpanded = !completedExpanded }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (completedExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = textColor.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$completedCount Completed items",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = textColor.copy(alpha = 0.8f)
                                )
                            }

                            AnimatedVisibility(
                                visible = completedExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    completedItems.forEachIndexed { posInList, (globalIndex, item) ->
                                        ChecklistRow(
                                            item = item,
                                            textColor = textColor,
                                            canMoveUp = posInList > 0,
                                            canMoveDown = posInList < completedItems.size - 1,
                                            isTargetFocus = item.id == targetFocusItemId,
                                            onFocused = { if (targetFocusItemId == item.id) targetFocusItemId = null },
                                            onToggle = { onToggleChecklistItem(globalIndex) },
                                            onTextChange = { onUpdateChecklistItemText(globalIndex, it) },
                                            onEnterPressed = { extraText ->
                                                val newId = java.util.UUID.randomUUID().toString()
                                                targetFocusItemId = newId
                                                onAddChecklistItem(globalIndex, extraText, newId)
                                            },
                                            onDelete = { onRemoveChecklistItem(globalIndex) },
                                            onMoveUp = {
                                                if (posInList > 0) {
                                                    val targetGlobalIndex = completedItems[posInList - 1].first
                                                    onMoveChecklistItem(globalIndex, targetGlobalIndex)
                                                }
                                            },
                                            onMoveDown = {
                                                if (posInList < completedItems.size - 1) {
                                                    val targetGlobalIndex = completedItems[posInList + 1].first
                                                    onMoveChecklistItem(globalIndex, targetGlobalIndex)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // BOTTOM CHIPS: REMINDER & LABELS (Google Keep Layout)
                // ==========================================
                if (state.reminderTimestamp != null || state.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // REMINDER BADGE DISPLAY
                        if (state.reminderTimestamp != null) {
                            val reminderFormatted = remember(state.reminderTimestamp) {
                                val sdf = SimpleDateFormat("EEE, MMM d, h:mm a", Locale.getDefault())
                                sdf.format(Date(state.reminderTimestamp))
                            }
                            val isOverdue = state.reminderTimestamp < System.currentTimeMillis()

                            Surface(
                                shape = CircleShape,
                                color = if (isOverdue) textColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, if (isOverdue) textColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Notifications,
                                        contentDescription = null,
                                        tint = if (isOverdue) textColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = reminderFormatted,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isOverdue) textColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable { showReminderDialog = true }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove reminder",
                                        tint = textColor.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { onSetReminder(null) }
                                    )
                                }
                            }
                        }

                        // LABELS DISPLAY
                        state.labels.forEach { label ->
                            Surface(
                                shape = CircleShape,
                                color = textColor.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, textColor.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp, end = 6.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Remove label",
                                        tint = textColor.copy(alpha = 0.6f),
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .clickable { onRemoveLabel(label) }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // ==========================================
            // COLOR PALETTE DRAWER (IF OPEN)
            // ==========================================
            AnimatedVisibility(
                visible = showColorPicker,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("editor_theme_palette_drawer")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // SECTION 1: COLOUR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "COLOUR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(KeepColorPalette.allColors) { colorTheme ->
                                val isSelected = colorTheme.key.equals(state.colorKey, ignoreCase = true)
                                val isDefault = colorTheme.key.equals("default", ignoreCase = true)
                                val swatchBg = if (isDefault) MaterialTheme.colorScheme.surfaceVariant else colorTheme.swatchColor
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(swatchBg)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.2.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                            shape = CircleShape
                                        )
                                        .clickable { onColorChange(colorTheme.key) }
                                        .testTag("color_picker_${colorTheme.key}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (isDefault) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (isDefault) {
                                        Icon(
                                            imageVector = Icons.Filled.Block,
                                            contentDescription = "No color",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // SECTION 2: BACKGROUND THEMES
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BACKGROUND",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // "None" option to reset theme to default
                            item {
                                val isNoneSelected = state.colorKey.equals("default", ignoreCase = true)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onColorChange("default") }
                                        .testTag("theme_picker_none")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = if (isNoneSelected) 3.dp else 1.2.dp,
                                                color = if (isNoneSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isNoneSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "None selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.Block,
                                                contentDescription = "None",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "None",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(KeepColorPalette.allThemes) { themeItem ->
                                val isSelected = themeItem.key.equals(state.colorKey, ignoreCase = true)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onColorChange(themeItem.key) }
                                        .testTag("theme_picker_${themeItem.key}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(themeItem.swatchColor)
                                            .border(
                                                width = if (isSelected) 3.dp else 1.2.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = "${themeItem.name} selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else if (themeItem.icon != null) {
                                            Icon(
                                                imageVector = themeItem.icon,
                                                contentDescription = themeItem.name,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = themeItem.name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BOTTOM TOOLBAR (Authentic Google Keep Layout)
            // ==========================================
            Surface(
                color = bgColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left tools: [+] Add sheet and [Palette] Color
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showAddSheet = true },
                            modifier = Modifier.testTag("editor_plus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AddBox,
                                contentDescription = "Add options",
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        IconButton(
                            onClick = { showColorPicker = !showColorPicker },
                            modifier = Modifier.testTag("editor_palette_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Palette,
                                contentDescription = "Color palette",
                                tint = textColor.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Center: Edited time
                    Text(
                        text = "Edited $formattedTime",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                        color = textColor.copy(alpha = 0.65f)
                    )

                    // Right action: Undo, Redo, 3-dots Overflow Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onUndo,
                            enabled = canUndo,
                            modifier = Modifier.size(36.dp).testTag("editor_undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) textColor else textColor.copy(alpha = 0.28f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = onRedo,
                            enabled = canRedo,
                            modifier = Modifier.size(36.dp).testTag("editor_redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) textColor else textColor.copy(alpha = 0.28f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.size(36.dp).testTag("editor_more_options_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = textColor.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onDelete()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Make a copy") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onDuplicate()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Send") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onShare()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Copy text") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        onCopyText()
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Labels") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    },
                                    onClick = {
                                        showMoreMenu = false
                                        showLabelDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // GOOGLE KEEP "+" ADD SHEET
        // ==========================================
        if (showAddSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddSheet = false },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp, top = 6.dp)
                ) {
                    KeepAddOptionRow(
                        icon = Icons.Outlined.PhotoCamera,
                        title = "Take photo",
                        onClick = {
                            showAddSheet = false
                            cameraLauncher.launch(null)
                        }
                    )

                    KeepAddOptionRow(
                        icon = Icons.Outlined.Image,
                        title = "Add image",
                        onClick = {
                            showAddSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    KeepAddOptionRow(
                        icon = Icons.Outlined.Brush,
                        title = "Drawing",
                        onClick = {
                            showAddSheet = false
                            showSketchDialog = true
                        }
                    )

                    KeepAddOptionRow(
                        icon = Icons.Outlined.Mic,
                        title = "Recording",
                        onClick = {
                            showAddSheet = false
                            onStartVoiceRecording()
                        }
                    )

                    KeepAddOptionRow(
                        icon = Icons.Outlined.CheckBox,
                        title = if (state.isChecklist) "Hide tick boxes" else "Tick boxes",
                        onClick = {
                            showAddSheet = false
                            onToggleChecklistMode()
                        }
                    )
                }
            }
        }

        // ==========================================
        // NOTE LABELS DIALOG
        // ==========================================
        if (showLabelDialog) {
            NoteLabelsDialog(
                allLabels = allLabels,
                selectedLabels = state.labels,
                onToggleLabel = onToggleLabel,
                onCreateAndAddLabel = onCreateAndAddLabel,
                onDismiss = { showLabelDialog = false }
            )
        }

        // ==========================================
        // DRAWING / SKETCH CANVAS DIALOG
        // ==========================================
        if (showSketchDialog) {
            KeepSketchDialog(
                onDismiss = { showSketchDialog = false },
                onSaveDrawing = { bitmap ->
                    onAddDrawing(bitmap)
                    showSketchDialog = false
                }
            )
        }

        // ==========================================
        // FULL SCREEN IMAGE VIEWER
        // ==========================================
        if (viewingImageUri != null) {
            Dialog(
                onDismissRequest = { viewingImageUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(viewingImageUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Full view image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { viewingImageUri = null },
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close preview",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // ==========================================
        // REMINDER PICKER DIALOG (Keep Style)
        // ==========================================
        if (showReminderDialog) {
            Dialog(onDismissRequest = { showReminderDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Add reminder",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val now = Calendar.getInstance()
                        val laterToday = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 18)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
                        }

                        val tomorrowMorning = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 8)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }

                        val nextWeek = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 8)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }

                        ReminderPresetRow(
                            title = "Later today",
                            subtitle = SimpleDateFormat("h:mm a", Locale.getDefault()).format(laterToday.time),
                            onClick = {
                                onSetReminder(laterToday.timeInMillis)
                                showReminderDialog = false
                            }
                        )

                        ReminderPresetRow(
                            title = "Tomorrow morning",
                            subtitle = SimpleDateFormat("EEE, 8:00 AM", Locale.getDefault()).format(tomorrowMorning.time),
                            onClick = {
                                onSetReminder(tomorrowMorning.timeInMillis)
                                showReminderDialog = false
                            }
                        )

                        ReminderPresetRow(
                            title = "Next week",
                            subtitle = SimpleDateFormat("EEE, MMM d, 8:00 AM", Locale.getDefault()).format(nextWeek.time),
                            onClick = {
                                onSetReminder(nextWeek.timeInMillis)
                                showReminderDialog = false
                            }
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        if (state.reminderTimestamp != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onSetReminder(null)
                                        showReminderDialog = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Delete reminder",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showReminderDialog = false }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepAddOptionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ReminderPresetRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Alarm,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun ChecklistRow(
    item: com.focusbyrj.app.data.note.ChecklistItem,
    textColor: Color,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isTargetFocus: Boolean,
    onFocused: () -> Unit,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onEnterPressed: (String) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val rowThresholdPx = remember(density) { with(density) { 44.dp.toPx() } }

    LaunchedEffect(isTargetFocus) {
        if (isTargetFocus) {
            try {
                focusRequester.requestFocus()
                onFocused()
            } catch (e: Exception) {
                // Ignore if not attached yet
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .offset { IntOffset(0, dragOffsetY.roundToInt()) }
            .background(
                color = if (isDragging) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 2.dp)
    ) {
        // Drag Handle with Touch / Pointer Drag Gesture
        Box(
            modifier = Modifier
                .size(34.dp)
                .pointerInput(item.id, canMoveUp, canMoveDown) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            isDragging = false
                            dragOffsetY = 0f
                        },
                        onDragCancel = {
                            isDragging = false
                            dragOffsetY = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount
                            if (dragOffsetY > rowThresholdPx && canMoveDown) {
                                onMoveDown()
                                dragOffsetY -= rowThresholdPx
                            } else if (dragOffsetY < -rowThresholdPx && canMoveUp) {
                                onMoveUp()
                                dragOffsetY += rowThresholdPx
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = if (isDragging) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Square Checkbox
        IconButton(
            onClick = onToggle,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (item.isChecked) "Completed" else "Incomplete",
                tint = if (item.isChecked) MaterialTheme.colorScheme.primary else textColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Checklist Text Field (Single line like Google Keep)
        BasicTextField(
            value = item.text,
            onValueChange = { newText ->
                if (newText.contains('\n')) {
                    val split = newText.split('\n', limit = 2)
                    onTextChange(split[0])
                    val nextItemText = if (split.size > 1) split[1] else ""
                    onEnterPressed(nextItemText)
                } else {
                    onTextChange(newText)
                }
            },
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onKeyEvent { keyEvent ->
                    if (keyEvent.type == KeyEventType.KeyDown) {
                        if (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) {
                            onEnterPressed("")
                            true
                        } else if (keyEvent.key == Key.Backspace && item.text.isEmpty()) {
                            onDelete()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                },
            textStyle = TextStyle(
                color = if (item.isChecked) textColor.copy(alpha = 0.45f) else textColor,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(textColor),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { onEnterPressed("") },
                onDone = { onEnterPressed("") }
            ),
            decorationBox = { innerTextField ->
                if (item.text.isEmpty()) {
                    Text(
                        text = "List item",
                        style = TextStyle(color = textColor.copy(alpha = 0.35f), fontSize = 16.sp)
                    )
                }
                innerTextField()
            }
        )

        // Delete 'x' icon
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Delete item",
                tint = textColor.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
