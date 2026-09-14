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

import android.content.Context
import android.graphics.Color

enum class NoteWidgetFilterMode(val displayName: String, val shortLabel: String) {
    ALL("All Notes", "All"),
    CHECKLISTS("Checklists", "Lists"),
    PINNED("Pinned", "Pin"),
    SPECIFIC("Single Note", "Lock");

    fun next(): NoteWidgetFilterMode {
        return when (this) {
            ALL -> CHECKLISTS
            CHECKLISTS -> PINNED
            PINNED -> ALL
            SPECIFIC -> ALL
        }
    }
}

enum class NoteWidgetSortBy(val displayName: String) {
    RECENTLY_UPDATED("Recently Updated"),
    RECENTLY_CREATED("Date Created"),
    PINNED_FIRST("Pinned First"),
    ALPHABETICAL("Alphabetical (A-Z)")
}

enum class NoteWidgetTextSize(val spValue: Float, val displayName: String) {
    TINY(11.5f, "Tiny"),
    COMPACT(13f, "Compact"),
    STANDARD(15f, "Standard"),
    LARGE(17.5f, "Large"),
    EXTRA_LARGE(20.5f, "Huge")
}

enum class NoteWidgetPadding(val dpValue: Int, val displayName: String) {
    TIGHT(6, "Tight"),
    COMPACT(10, "Compact"),
    STANDARD(14, "Standard"),
    SPACIOUS(18, "Spacious")
}

data class NoteWidgetConfig(
    val theme: WidgetTheme = WidgetTheme.DARK,
    val accent: WidgetAccent = WidgetAccent.BLUE,
    val opacityPercent: Int = 95,
    val cornerRadiusDp: Int = 16,
    val matchNoteColor: Boolean = true,
    val filterMode: NoteWidgetFilterMode = NoteWidgetFilterMode.ALL,
    val specificNoteId: Long? = null,
    val sortBy: NoteWidgetSortBy = NoteWidgetSortBy.RECENTLY_UPDATED,
    val textSize: NoteWidgetTextSize = NoteWidgetTextSize.STANDARD,
    val padding: NoteWidgetPadding = NoteWidgetPadding.STANDARD,
    val showTitle: Boolean = true,
    val showQuickAddBar: Boolean = true,
    val showActionButtons: Boolean = true,
    val showNavHeader: Boolean = true,
    val adaptiveLayout: Boolean = true
) {
    val backgroundColorInt: Int
        get() {
            val base = Color.parseColor(theme.baseColorHex)
            val alpha = ((opacityPercent / 100f) * 255).toInt().coerceIn(0, 255)
            return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
        }

    val accentColorInt: Int
        get() = Color.parseColor(accent.hex)

    val primaryTextColorInt: Int
        get() = if (theme.isDark) Color.parseColor("#FFFFFF") else Color.parseColor("#121516")

    val secondaryTextColorInt: Int
        get() = if (theme.isDark) Color.parseColor("#9AA0A6") else Color.parseColor("#6B7280")
}

object NoteWidgetConfigHelper {
    private const val PREFS_NAME = "note_widget_prefs"
    private const val KEY_THEME = "theme_"
    private const val KEY_ACCENT = "accent_"
    private const val KEY_OPACITY = "opacity_"
    private const val KEY_CORNER = "corner_"
    private const val KEY_MATCH_NOTE = "match_note_color_"
    private const val KEY_FILTER_MODE_PREFIX = "note_filter_mode_"
    private const val KEY_SPECIFIC_ID_PREFIX = "note_specific_id_"
    private const val KEY_INDEX_PREFIX = "note_index_"
    private const val KEY_SORT_BY = "sort_by_"
    private const val KEY_TEXT_SIZE = "text_size_"
    private const val KEY_PADDING = "padding_"
    private const val KEY_SHOW_TITLE = "show_title_"
    private const val KEY_SHOW_QUICK_ADD = "show_quick_add_"
    private const val KEY_SHOW_ACTION_BUTTONS = "show_action_buttons_"
    private const val KEY_SHOW_NAV_HEADER = "show_nav_header_"
    private const val KEY_ADAPTIVE_LAYOUT = "adaptive_layout_"

    fun getConfig(context: Context, appWidgetId: Int): NoteWidgetConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val modeStr = prefs.getString(KEY_FILTER_MODE_PREFIX + appWidgetId, NoteWidgetFilterMode.ALL.name)
        val filterMode = try {
            NoteWidgetFilterMode.valueOf(modeStr ?: NoteWidgetFilterMode.ALL.name)
        } catch (_: Exception) {
            NoteWidgetFilterMode.ALL
        }

        val specificIdRaw = prefs.getLong(KEY_SPECIFIC_ID_PREFIX + appWidgetId, -1L)
        val specificNoteId = if (specificIdRaw != -1L) specificIdRaw else null

        val themeName = prefs.getString(KEY_THEME + appWidgetId, WidgetTheme.DARK.name) ?: WidgetTheme.DARK.name
        val accentName = prefs.getString(KEY_ACCENT + appWidgetId, WidgetAccent.BLUE.name) ?: WidgetAccent.BLUE.name
        val opacity = prefs.getInt(KEY_OPACITY + appWidgetId, 95)
        val corner = prefs.getInt(KEY_CORNER + appWidgetId, 16)
        val matchNote = prefs.getBoolean(KEY_MATCH_NOTE + appWidgetId, true)

        val sortStr = prefs.getString(KEY_SORT_BY + appWidgetId, NoteWidgetSortBy.RECENTLY_UPDATED.name)
        val sortBy = runCatching { NoteWidgetSortBy.valueOf(sortStr ?: "") }.getOrDefault(NoteWidgetSortBy.RECENTLY_UPDATED)

        val textStr = prefs.getString(KEY_TEXT_SIZE + appWidgetId, NoteWidgetTextSize.STANDARD.name)
        val textSize = runCatching { NoteWidgetTextSize.valueOf(textStr ?: "") }.getOrDefault(NoteWidgetTextSize.STANDARD)

        val padStr = prefs.getString(KEY_PADDING + appWidgetId, NoteWidgetPadding.STANDARD.name)
        val padding = runCatching { NoteWidgetPadding.valueOf(padStr ?: "") }.getOrDefault(NoteWidgetPadding.STANDARD)

        val showTitle = prefs.getBoolean(KEY_SHOW_TITLE + appWidgetId, true)
        val showQuickAdd = prefs.getBoolean(KEY_SHOW_QUICK_ADD + appWidgetId, true)
        val showActionButtons = prefs.getBoolean(KEY_SHOW_ACTION_BUTTONS + appWidgetId, true)
        val showNavHeader = prefs.getBoolean(KEY_SHOW_NAV_HEADER + appWidgetId, true)
        val adaptiveLayout = prefs.getBoolean(KEY_ADAPTIVE_LAYOUT + appWidgetId, true)

        val theme = runCatching { WidgetTheme.valueOf(themeName) }.getOrDefault(WidgetTheme.DARK)
        val accent = runCatching { WidgetAccent.valueOf(accentName) }.getOrDefault(WidgetAccent.BLUE)

        return NoteWidgetConfig(
            theme = theme,
            accent = accent,
            opacityPercent = opacity,
            cornerRadiusDp = corner,
            matchNoteColor = matchNote,
            filterMode = filterMode,
            specificNoteId = specificNoteId,
            sortBy = sortBy,
            textSize = textSize,
            padding = padding,
            showTitle = showTitle,
            showQuickAddBar = showQuickAdd,
            showActionButtons = showActionButtons,
            showNavHeader = showNavHeader,
            adaptiveLayout = adaptiveLayout
        )
    }

    fun saveConfig(context: Context, appWidgetId: Int, config: NoteWidgetConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_THEME + appWidgetId, config.theme.name)
            .putString(KEY_ACCENT + appWidgetId, config.accent.name)
            .putInt(KEY_OPACITY + appWidgetId, config.opacityPercent)
            .putInt(KEY_CORNER + appWidgetId, config.cornerRadiusDp)
            .putBoolean(KEY_MATCH_NOTE + appWidgetId, config.matchNoteColor)
            .putString(KEY_FILTER_MODE_PREFIX + appWidgetId, config.filterMode.name)
            .putLong(KEY_SPECIFIC_ID_PREFIX + appWidgetId, config.specificNoteId ?: -1L)
            .putString(KEY_SORT_BY + appWidgetId, config.sortBy.name)
            .putString(KEY_TEXT_SIZE + appWidgetId, config.textSize.name)
            .putString(KEY_PADDING + appWidgetId, config.padding.name)
            .putBoolean(KEY_SHOW_TITLE + appWidgetId, config.showTitle)
            .putBoolean(KEY_SHOW_QUICK_ADD + appWidgetId, config.showQuickAddBar)
            .putBoolean(KEY_SHOW_ACTION_BUTTONS + appWidgetId, config.showActionButtons)
            .putBoolean(KEY_SHOW_NAV_HEADER + appWidgetId, config.showNavHeader)
            .putBoolean(KEY_ADAPTIVE_LAYOUT + appWidgetId, config.adaptiveLayout)
            .apply()
    }

    fun getCurrentIndex(context: Context, appWidgetId: Int): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INDEX_PREFIX + appWidgetId, 0)
    }

    fun setCurrentIndex(context: Context, appWidgetId: Int, index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_INDEX_PREFIX + appWidgetId, index.coerceAtLeast(0)).apply()
    }

    fun setFilterMode(context: Context, appWidgetId: Int, mode: NoteWidgetFilterMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FILTER_MODE_PREFIX + appWidgetId, mode.name)
            .putInt(KEY_INDEX_PREFIX + appWidgetId, 0)
            .apply()
    }
}
