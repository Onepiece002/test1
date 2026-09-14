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

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Paint
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.focusbyrj.app.R
import com.focusbyrj.app.data.note.ChecklistItem
import com.focusbyrj.app.data.note.NoteDatabase
import com.focusbyrj.app.data.note.NoteEntity
import kotlinx.coroutines.runBlocking

class NoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return NoteWidgetRemoteViewsFactory(applicationContext, intent)
    }
}

class NoteWidgetRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var currentNote: NoteEntity? = null
    private var checklistItems: List<ChecklistItem> = emptyList()
    private var widgetConfig: NoteWidgetConfig = NoteWidgetConfig()

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            widgetConfig = NoteWidgetConfigHelper.getConfig(context, appWidgetId)
            val noteDao = NoteDatabase.getInstance(context).noteDao()

            val targetNote = runBlocking {
                if (widgetConfig.filterMode == NoteWidgetFilterMode.SPECIFIC && widgetConfig.specificNoteId != null) {
                    noteDao.getNoteByIdSync(widgetConfig.specificNoteId!!)
                } else {
                    val notes = when (widgetConfig.filterMode) {
                        NoteWidgetFilterMode.CHECKLISTS -> noteDao.getChecklistNotesSync()
                        NoteWidgetFilterMode.PINNED -> noteDao.getPinnedNotesSync()
                        else -> noteDao.getAllActiveNotesSync()
                    }

                    if (notes.isNotEmpty()) {
                        val index = NoteWidgetConfigHelper.getCurrentIndex(context, appWidgetId)
                        val safeIndex = index % notes.size
                        notes[safeIndex]
                    } else {
                        null
                    }
                }
            }

            currentNote = targetNote
            checklistItems = targetNote?.getChecklistItems() ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            checklistItems = emptyList()
            currentNote = null
        }
    }

    override fun onDestroy() {
        checklistItems = emptyList()
        currentNote = null
    }

    override fun getCount(): Int = checklistItems.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= checklistItems.size) return null
        val item = checklistItems[position]
        val note = currentNote ?: return null

        val views = RemoteViews(context.packageName, R.layout.widget_note_item)
        val isSystemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val isDark = if (widgetConfig.matchNoteColor && !note.colorKey.isNullOrEmpty() && note.colorKey != "default") isSystemDark else widgetConfig.theme.isDark

        val bgColors = NoteWidgetDrawableGenerator.createWidgetBackground(
            context = context,
            config = widgetConfig,
            noteColorKey = note.colorKey,
            isSystemDark = isSystemDark
        )

        // Drag Handle color tinting
        views.setInt(R.id.widget_note_item_drag_handle, "setColorFilter", bgColors.secondaryTextColor)

        // Remove X button color tinting
        views.setInt(R.id.widget_note_item_remove, "setColorFilter", bgColors.secondaryTextColor)

        // Render Checkbox
        val checkBitmap = NoteWidgetDrawableGenerator.createCheckboxBitmap(
            isChecked = item.isChecked,
            accentColorInt = bgColors.accentColor,
            secondaryTextColorInt = bgColors.secondaryTextColor,
            isDark = isDark
        )
        views.setImageViewBitmap(R.id.widget_note_item_checkbox, checkBitmap)

        // Render Text & Strike-through
        views.setTextViewText(R.id.widget_note_item_text, item.text)
        views.setFloat(R.id.widget_note_item_text, "setTextSize", widgetConfig.textSize.spValue)
        if (item.isChecked) {
            views.setTextColor(R.id.widget_note_item_text, bgColors.secondaryTextColor)
            views.setInt(
                R.id.widget_note_item_text,
                "setPaintFlags",
                Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG
            )
        } else {
            views.setTextColor(R.id.widget_note_item_text, bgColors.primaryTextColor)
            views.setInt(
                R.id.widget_note_item_text,
                "setPaintFlags",
                Paint.ANTI_ALIAS_FLAG
            )
        }

        // Checkbox click intent (toggles item state)
        val toggleIntent = Intent().apply {
            putExtra(NoteWidgetProvider.EXTRA_ACTION_TYPE, NoteWidgetProvider.ACTION_TYPE_TOGGLE)
            putExtra(NoteWidgetProvider.EXTRA_NOTE_ID, note.id)
            putExtra(NoteWidgetProvider.EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_note_item_checkbox, toggleIntent)

        // Remove X click intent (deletes item directly from widget)
        val removeIntent = Intent().apply {
            putExtra(NoteWidgetProvider.EXTRA_ACTION_TYPE, NoteWidgetProvider.ACTION_TYPE_REMOVE)
            putExtra(NoteWidgetProvider.EXTRA_NOTE_ID, note.id)
            putExtra(NoteWidgetProvider.EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_note_item_remove, removeIntent)

        // Drag handle & root text click intent (opens editor / reorder view)
        val editIntent = Intent().apply {
            putExtra(NoteWidgetProvider.EXTRA_ACTION_TYPE, NoteWidgetProvider.ACTION_TYPE_EDIT)
            putExtra(NoteWidgetProvider.EXTRA_NOTE_ID, note.id)
            putExtra(NoteWidgetProvider.EXTRA_ITEM_ID, item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_note_item_drag_handle, editIntent)
        views.setOnClickFillInIntent(R.id.widget_note_item_text, editIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
