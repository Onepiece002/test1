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

package com.focusbyrj.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.focusbyrj.app.MainActivity
import com.focusbyrj.app.R
import com.focusbyrj.app.data.note.ChecklistItem
import com.focusbyrj.app.data.note.NoteDatabase
import com.focusbyrj.app.data.note.NoteEntity
import com.focusbyrj.app.ui.screens.notes.NotesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject

class NoteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_NOTE = "com.focusbyrj.app.widget.ACTION_PREV_NOTE"
        const val ACTION_NEXT_NOTE = "com.focusbyrj.app.widget.ACTION_NEXT_NOTE"
        const val ACTION_CYCLE_FILTER = "com.focusbyrj.app.widget.ACTION_CYCLE_FILTER"
        const val ACTION_TOGGLE_ITEM = "com.focusbyrj.app.widget.ACTION_TOGGLE_ITEM"

        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val ACTION_TYPE_TOGGLE = "toggle"
        const val ACTION_TYPE_REMOVE = "remove"
        const val ACTION_TYPE_EDIT = "edit"

        fun updateAllWidgets(context: Context) {
            kotlin.runCatching {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, NoteWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    for (widgetId in appWidgetIds) {
                        updateWidget(context, appWidgetManager, widgetId)
                    }
                    kotlin.runCatching {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_note_list_view)
                    }
                }
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            kotlin.runCatching {
                val config = NoteWidgetConfigHelper.getConfig(context, appWidgetId)
                val noteDao = NoteDatabase.getInstance(context).noteDao()
                val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

                val allMatchingNotes: List<NoteEntity> = runBlocking {
                    val rawNotes = if (config.filterMode == NoteWidgetFilterMode.SPECIFIC && config.specificNoteId != null) {
                        val single = noteDao.getNoteByIdSync(config.specificNoteId!!)
                        if (single != null) listOf(single) else emptyList()
                    } else {
                        when (config.filterMode) {
                            NoteWidgetFilterMode.NOTES -> noteDao.getTextNotesSync()
                            NoteWidgetFilterMode.CHECKLISTS -> noteDao.getChecklistNotesSync()
                            NoteWidgetFilterMode.PINNED -> noteDao.getPinnedNotesSync()
                            else -> noteDao.getAllActiveNotesSync()
                        }
                    }

                    // Apply configured sort order
                    when (config.sortBy) {
                        NoteWidgetSortBy.RECENTLY_UPDATED -> rawNotes.sortedByDescending { it.updatedAt }
                        NoteWidgetSortBy.RECENTLY_CREATED -> rawNotes.sortedByDescending { it.createdAt }
                        NoteWidgetSortBy.PINNED_FIRST -> rawNotes.sortedWith(compareByDescending<NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })
                        NoteWidgetSortBy.ALPHABETICAL -> rawNotes.sortedBy { it.title.lowercase() }
                    }
                }

                val currentIndex = NoteWidgetConfigHelper.getCurrentIndex(context, appWidgetId)
                val currentNote = if (allMatchingNotes.isNotEmpty()) {
                    val safeIndex = currentIndex % allMatchingNotes.size
                    allMatchingNotes[safeIndex]
                } else {
                    null
                }

                // Query runtime widget dimensions for responsive resizing
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0) ?: 0
                val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0

                val targetW = if (minWidth > 0) minWidth else 320
                val targetH = if (minHeight > 0) minHeight else 200

                val isNarrow = config.adaptiveLayout && minWidth in 1..220
                val isVeryNarrow = config.adaptiveLayout && minWidth in 1..155
                val isShort = config.adaptiveLayout && minHeight in 1..130
                val isVeryShort = config.adaptiveLayout && minHeight in 1..95

                val views = RemoteViews(context.packageName, R.layout.widget_note_layout)

                // Background & theme colors
                val bgColors = NoteWidgetDrawableGenerator.createWidgetBackground(
                    context = context,
                    config = config,
                    noteColorKey = currentNote?.colorKey,
                    isSystemDark = isSystemDark,
                    targetWidthDp = targetW,
                    targetHeightDp = targetH
                )
                views.setImageViewBitmap(R.id.widget_note_bg_image, bgColors.bitmap)
                views.setTextColor(R.id.widget_note_title, bgColors.primaryTextColor)
                views.setTextColor(R.id.widget_note_page_indicator, bgColors.secondaryTextColor)
                views.setInt(R.id.widget_note_btn_prev, "setColorFilter", bgColors.secondaryTextColor)
                views.setInt(R.id.widget_note_btn_next, "setColorFilter", bgColors.secondaryTextColor)
                views.setInt(R.id.widget_note_btn_settings, "setColorFilter", bgColors.secondaryTextColor)
                views.setInt(R.id.widget_note_divider, "setColorFilter", bgColors.borderColor)

                // Title visibility
                views.setViewVisibility(R.id.widget_note_title, if (config.showTitle) View.VISIBLE else View.GONE)

                // Visibility of Navigation / Header Controls
                val showCarousel = if (!config.showNavHeader) {
                    false
                } else if (isVeryNarrow) {
                    allMatchingNotes.size > 1
                } else {
                    allMatchingNotes.size > 1
                }
                views.setViewVisibility(R.id.widget_note_carousel_pill, if (showCarousel) View.VISIBLE else View.GONE)

                val showFilterMode = config.showNavHeader && !isNarrow && !isVeryNarrow
                views.setViewVisibility(R.id.widget_note_btn_filter_mode, if (showFilterMode) View.VISIBLE else View.GONE)

                // Bottom bar and action buttons visibility (adapt to widget height / width)
                val showBottomBar = config.showQuickAddBar && !isShort && !isVeryShort
                views.setViewVisibility(R.id.widget_note_bottom_bar, if (showBottomBar) View.VISIBLE else View.GONE)

                val showActionBtns = config.showActionButtons && !isNarrow && !isVeryNarrow
                views.setViewVisibility(R.id.widget_note_btn_voice, if (showActionBtns) View.VISIBLE else View.GONE)
                views.setViewVisibility(R.id.widget_note_btn_new_note, if (showActionBtns) View.VISIBLE else View.GONE)

                // Add item hint and icon styling
                val addHintText = if (currentNote == null) "Take a note..." else if (currentNote.isChecklist) "Tap to edit checklist..." else "Tap to edit note..."
                views.setTextViewText(R.id.widget_note_add_hint, addHintText)
                views.setTextColor(R.id.widget_note_add_hint, bgColors.secondaryTextColor)
                views.setInt(R.id.widget_note_add_icon, "setColorFilter", bgColors.accentColor)
                views.setInt(R.id.widget_note_btn_voice, "setColorFilter", bgColors.primaryTextColor)
                views.setInt(R.id.widget_note_btn_new_note, "setColorFilter", bgColors.primaryTextColor)

                // Mode filter pill text & color
                views.setTextViewText(R.id.widget_note_btn_filter_mode, config.filterMode.shortLabel)
                views.setTextColor(R.id.widget_note_btn_filter_mode, bgColors.secondaryTextColor)

                if (currentNote != null) {
                    val displayTitle = if (currentNote.title.isNotBlank()) currentNote.title else if (currentNote.isChecklist) "Checklist" else "Note"
                    views.setTextViewText(R.id.widget_note_title, displayTitle)
                    views.setTextViewTextSize(R.id.widget_note_title, TypedValue.COMPLEX_UNIT_SP, (config.textSize.spValue + 2f).coerceAtLeast(14f))

                    // Pin badge
                    if (currentNote.isPinned) {
                        views.setViewVisibility(R.id.widget_note_pinned_badge, View.VISIBLE)
                        views.setInt(R.id.widget_note_pinned_badge, "setColorFilter", bgColors.accentColor)
                    } else {
                        views.setViewVisibility(R.id.widget_note_pinned_badge, View.GONE)
                    }

                    // Page indicator: e.g. 2/5
                    val currentDisplayIndex = (currentIndex % allMatchingNotes.size) + 1
                    views.setTextViewText(
                        R.id.widget_note_page_indicator,
                        "$currentDisplayIndex/${allMatchingNotes.size}"
                    )

                    // Type Icon
                    views.setImageViewResource(
                        R.id.widget_note_type_icon,
                        if (currentNote.isChecklist) R.drawable.ic_widget_checklist else R.drawable.ic_widget_notes
                    )
                    views.setInt(R.id.widget_note_type_icon, "setColorFilter", bgColors.primaryTextColor)

                    // Empty View GONE
                    views.setViewVisibility(R.id.widget_note_empty_view, View.GONE)

                    if (currentNote.isChecklist) {
                        // Checklist: show ListView
                        views.setViewVisibility(R.id.widget_note_list_view, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_note_text_content, View.GONE)

                        val serviceIntent = Intent(context, NoteWidgetService::class.java).apply {
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            data = Uri.parse("widget://$appWidgetId/${currentNote.id}/checklist/${System.currentTimeMillis()}")
                        }
                        views.setRemoteAdapter(R.id.widget_note_list_view, serviceIntent)

                        // PendingIntent template for clicking on items (toggle checkbox!)
                        val itemToggleIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                            action = ACTION_TOGGLE_ITEM
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        }
                        val itemTogglePendingIntent = PendingIntent.getBroadcast(
                            context,
                            appWidgetId,
                            itemToggleIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                        )
                        views.setPendingIntentTemplate(R.id.widget_note_list_view, itemTogglePendingIntent)
                    } else {
                        // Text Note: show TextView
                        views.setViewVisibility(R.id.widget_note_list_view, View.GONE)
                        views.setViewVisibility(R.id.widget_note_text_content, View.VISIBLE)
                        views.setTextColor(R.id.widget_note_text_content, bgColors.primaryTextColor)
                        views.setTextViewTextSize(R.id.widget_note_text_content, TypedValue.COMPLEX_UNIT_SP, config.textSize.spValue)
                        val text = if (currentNote.content.isNotBlank()) currentNote.content else "(Empty note - tap to write)"
                        views.setTextViewText(R.id.widget_note_text_content, text)

                        // Direct edit note click on text body -> opens QuickEditNoteActivity floating dialog right on home screen!
                        val textClickIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                            putExtra(QuickEditNoteActivity.EXTRA_NOTE_ID, currentNote.id)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            data = Uri.parse("widget://$appWidgetId/text_body/${currentNote.id}")
                        }
                        views.setOnClickPendingIntent(
                            R.id.widget_note_text_content,
                            PendingIntent.getActivity(context, 5500 + appWidgetId, textClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        )
                    }
                } else {
                    // Empty state
                    views.setTextViewText(R.id.widget_note_title, "No Notes")
                    views.setViewVisibility(R.id.widget_note_pinned_badge, View.GONE)
                    views.setTextViewText(R.id.widget_note_page_indicator, "0/0")
                    views.setViewVisibility(R.id.widget_note_list_view, View.GONE)
                    views.setViewVisibility(R.id.widget_note_text_content, View.GONE)
                    views.setViewVisibility(R.id.widget_note_empty_view, View.VISIBLE)
                    views.setTextColor(R.id.widget_note_empty_title, bgColors.primaryTextColor)
                    views.setTextColor(R.id.widget_note_empty_subtext, bgColors.secondaryTextColor)

                    // Empty view click -> create new note directly on home screen
                    val emptyClickIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                        putExtra(QuickEditNoteActivity.EXTRA_CREATE_NEW, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        data = Uri.parse("widget://$appWidgetId/empty_click")
                    }
                    views.setOnClickPendingIntent(
                        R.id.widget_note_empty_view,
                        PendingIntent.getActivity(context, 5600 + appWidgetId, emptyClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    )
                }

                // Prev note button
                val prevIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                    action = ACTION_PREV_NOTE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://$appWidgetId/prev")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_prev,
                    PendingIntent.getBroadcast(context, 1000 + appWidgetId, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Next note button
                val nextIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_NOTE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://$appWidgetId/next")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_next,
                    PendingIntent.getBroadcast(context, 2000 + appWidgetId, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Filter mode cycle button
                val modeIntent = Intent(context, NoteWidgetProvider::class.java).apply {
                    action = ACTION_CYCLE_FILTER
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    data = Uri.parse("widget://$appWidgetId/mode")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_filter_mode,
                    PendingIntent.getBroadcast(context, 3000 + appWidgetId, modeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Settings button (opens NoteWidgetConfigureActivity)
                val settingsIntent = Intent(context, NoteWidgetConfigureActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    data = Uri.parse("widget://$appWidgetId/settings")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_settings,
                    PendingIntent.getActivity(context, 4000 + appWidgetId, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Header click -> open note editor floating dialog directly on home screen
                // Click note icon -> opens MainActivity directly at Notes tab
                val openNotesTabIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("navigate_to", com.focusbyrj.app.ui.navigation.Screen.Empty.route)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("widget://$appWidgetId/open_notes_tab")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_type_icon,
                    PendingIntent.getActivity(context, 7000 + appWidgetId, openNotesTabIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                val openNoteIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                    if (currentNote != null) {
                        putExtra(QuickEditNoteActivity.EXTRA_NOTE_ID, currentNote.id)
                    } else {
                        putExtra(QuickEditNoteActivity.EXTRA_CREATE_NEW, true)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("widget://$appWidgetId/open_header/${currentNote?.id ?: 0}")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_header_clickable,
                    PendingIntent.getActivity(context, 5000 + appWidgetId, openNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Direct note editing / add item bar -> opens QuickEditNoteActivity floating dialog right on home screen!
                val editNoteIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                    if (currentNote != null) {
                        putExtra(QuickEditNoteActivity.EXTRA_NOTE_ID, currentNote.id)
                    } else {
                        putExtra(QuickEditNoteActivity.EXTRA_CREATE_NEW, true)
                        putExtra(QuickEditNoteActivity.EXTRA_IS_CHECKLIST, true)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("widget://$appWidgetId/edit_note_direct/${currentNote?.id ?: 0}")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_add_item,
                    PendingIntent.getActivity(context, 6000 + appWidgetId, editNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // Voice note button -> opens QuickEditNoteActivity with voice recording directly on home screen
                val voiceIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                    if (currentNote != null) {
                        putExtra(QuickEditNoteActivity.EXTRA_NOTE_ID, currentNote.id)
                    } else {
                        putExtra(QuickEditNoteActivity.EXTRA_CREATE_NEW, true)
                    }
                    putExtra(QuickEditNoteActivity.EXTRA_AUTO_VOICE, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("widget://$appWidgetId/voice_direct")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_voice,
                    PendingIntent.getActivity(context, 7000 + appWidgetId, voiceIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                // New note button -> opens QuickEditNoteActivity for a fresh note directly on home screen
                val newNoteIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                    putExtra(QuickEditNoteActivity.EXTRA_CREATE_NEW, true)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("widget://$appWidgetId/new_note_direct")
                }
                views.setOnClickPendingIntent(
                    R.id.widget_note_btn_new_note,
                    PendingIntent.getActivity(context, 8000 + appWidgetId, newNoteIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                )

                appWidgetManager.updateAppWidget(appWidgetId, views)
                kotlin.runCatching {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_note_list_view)
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        when (intent.action) {
            ACTION_TOGGLE_ITEM -> {
                val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_TYPE_TOGGLE
                val noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)

                if (actionType == ACTION_TYPE_EDIT) {
                    if (noteId != -1L) {
                        val editIntent = Intent(context, QuickEditNoteActivity::class.java).apply {
                            putExtra(QuickEditNoteActivity.EXTRA_NOTE_ID, noteId)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        context.startActivity(editIntent)
                    }
                } else if (actionType == ACTION_TYPE_REMOVE) {
                    if (noteId != -1L && !itemId.isNullOrBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val noteDao = NoteDatabase.getInstance(context).noteDao()
                            val cached = NotesViewModel.latestNotesCache[noteId]
                            val dbNote = noteDao.getNoteByIdSync(noteId)
                            val note = when {
                                cached != null && dbNote != null -> if (cached.updatedAt >= dbNote.updatedAt) cached else dbNote
                                dbNote != null -> dbNote
                                cached != null -> cached
                                else -> null
                            }
                            if (note != null && note.isChecklist) {
                                val remainingItems = note.getChecklistItems().filterNot { it.id == itemId }
                                val (uncompleted, completed) = remainingItems.partition { !it.isChecked }
                                val updatedNote = note.copy(
                                    checklistJson = ChecklistItem.listToJson(uncompleted + completed),
                                    updatedAt = System.currentTimeMillis()
                                )
                                noteDao.updateNote(updatedNote)
                                NotesViewModel.latestNotesCache[updatedNote.id] = updatedNote

                                // Refresh widgets immediately
                                val componentName = ComponentName(context, NoteWidgetProvider::class.java)
                                val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                                if (widgetIds != null && widgetIds.isNotEmpty()) {
                                    for (wid in widgetIds) {
                                        updateWidget(context, appWidgetManager, wid)
                                    }
                                    kotlin.runCatching {
                                        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_note_list_view)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (noteId != -1L && !itemId.isNullOrBlank()) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val noteDao = NoteDatabase.getInstance(context).noteDao()
                            val cached = NotesViewModel.latestNotesCache[noteId]
                            val dbNote = noteDao.getNoteByIdSync(noteId)
                            val note = when {
                                cached != null && dbNote != null -> if (cached.updatedAt >= dbNote.updatedAt) cached else dbNote
                                dbNote != null -> dbNote
                                cached != null -> cached
                                else -> null
                            }
                            if (note != null && note.isChecklist) {
                                val items = note.getChecklistItems().toMutableList()
                                val idx = items.indexOfFirst { it.id == itemId }
                                if (idx != -1) {
                                    val current = items[idx]
                                    items[idx] = current.copy(isChecked = !current.isChecked)
                                    val (uncompleted, completed) = items.partition { !it.isChecked }
                                    val updatedNote = note.copy(
                                        checklistJson = ChecklistItem.listToJson(uncompleted + completed),
                                        updatedAt = System.currentTimeMillis()
                                    )
                                    noteDao.updateNote(updatedNote)
                                    NotesViewModel.latestNotesCache[updatedNote.id] = updatedNote

                                    // Refresh widgets immediately
                                    val componentName = ComponentName(context, NoteWidgetProvider::class.java)
                                    val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
                                    if (widgetIds != null && widgetIds.isNotEmpty()) {
                                        for (wid in widgetIds) {
                                            updateWidget(context, appWidgetManager, wid)
                                        }
                                        kotlin.runCatching {
                                            appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.widget_note_list_view)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ACTION_PREV_NOTE -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val config = NoteWidgetConfigHelper.getConfig(context, appWidgetId)
                        val noteDao = NoteDatabase.getInstance(context).noteDao()
                        val totalNotes = when (config.filterMode) {
                            NoteWidgetFilterMode.NOTES -> noteDao.getTextNotesSync().size
                            NoteWidgetFilterMode.CHECKLISTS -> noteDao.getChecklistNotesSync().size
                            NoteWidgetFilterMode.PINNED -> noteDao.getPinnedNotesSync().size
                            else -> noteDao.getAllActiveNotesSync().size
                        }
                        val currentIdx = NoteWidgetConfigHelper.getCurrentIndex(context, appWidgetId)
                        val newIdx = if (totalNotes > 0) {
                            (currentIdx - 1 + totalNotes) % totalNotes
                        } else {
                            0
                        }
                        NoteWidgetConfigHelper.setCurrentIndex(context, appWidgetId, newIdx)
                        updateWidget(context, appWidgetManager, appWidgetId)
                        kotlin.runCatching {
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_note_list_view)
                        }
                    }
                }
            }

            ACTION_NEXT_NOTE -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val config = NoteWidgetConfigHelper.getConfig(context, appWidgetId)
                        val noteDao = NoteDatabase.getInstance(context).noteDao()
                        val totalNotes = when (config.filterMode) {
                            NoteWidgetFilterMode.NOTES -> noteDao.getTextNotesSync().size
                            NoteWidgetFilterMode.CHECKLISTS -> noteDao.getChecklistNotesSync().size
                            NoteWidgetFilterMode.PINNED -> noteDao.getPinnedNotesSync().size
                            else -> noteDao.getAllActiveNotesSync().size
                        }
                        val currentIdx = NoteWidgetConfigHelper.getCurrentIndex(context, appWidgetId)
                        val newIdx = if (totalNotes > 0) {
                            (currentIdx + 1) % totalNotes
                        } else {
                            0
                        }
                        NoteWidgetConfigHelper.setCurrentIndex(context, appWidgetId, newIdx)
                        updateWidget(context, appWidgetManager, appWidgetId)
                        kotlin.runCatching {
                            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_note_list_view)
                        }
                    }
                }
            }

            ACTION_CYCLE_FILTER -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val config = NoteWidgetConfigHelper.getConfig(context, appWidgetId)
                    val nextMode = config.filterMode.next()
                    NoteWidgetConfigHelper.setFilterMode(context, appWidgetId, nextMode)
                    updateWidget(context, appWidgetManager, appWidgetId)
                    kotlin.runCatching {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_note_list_view)
                    }
                }
            }
        }
    }
}
