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

package com.focusbyrj.app.util

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeColor(
    val id: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val glowColor: Color,
    val description: String,
    val swatch: List<Color>
) {
    EMERALD(
        id = "emerald",
        displayName = "Emerald Aurora",
        primary = Color(0xFF10B981),
        secondary = Color(0xFF34D399),
        tertiary = Color(0xFF059669),
        glowColor = Color(0x3310B981),
        description = "Vibrant mint & emerald radiance",
        swatch = listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFF34D399))
    ),
    CYAN(
        id = "cyan",
        displayName = "Cyber Cyan",
        primary = Color(0xFF06B6D4),
        secondary = Color(0xFF22D3EE),
        tertiary = Color(0xFF0891B2),
        glowColor = Color(0x3306B6D4),
        description = "Electric cyan & aqua crystal",
        swatch = listOf(Color(0xFF0891B2), Color(0xFF06B6D4), Color(0xFF22D3EE))
    ),
    VIOLET(
        id = "violet",
        displayName = "Electric Violet",
        primary = Color(0xFF8B5CF6),
        secondary = Color(0xFFA78BFA),
        tertiary = Color(0xFF7C3AED),
        glowColor = Color(0x338B5CF6),
        description = "Ultraviolet & neon purple glow",
        swatch = listOf(Color(0xFF7C3AED), Color(0xFF8B5CF6), Color(0xFFA78BFA))
    ),
    ROSE(
        id = "rose",
        displayName = "Sunset Rose",
        primary = Color(0xFFF43F5E),
        secondary = Color(0xFFFB7185),
        tertiary = Color(0xFFE11D48),
        glowColor = Color(0x33F43F5E),
        description = "Vivid crimson & dusk rose",
        swatch = listOf(Color(0xFFE11D48), Color(0xFFF43F5E), Color(0xFFFB7185))
    ),
    AMBER(
        id = "amber",
        displayName = "Solar Amber",
        primary = Color(0xFFF59E0B),
        secondary = Color(0xFFFBBF24),
        tertiary = Color(0xFFD97706),
        glowColor = Color(0x33F59E0B),
        description = "Golden ray & sunburst flare",
        swatch = listOf(Color(0xFFD97706), Color(0xFFF59E0B), Color(0xFFFBBF24))
    ),
    COBALT(
        id = "cobalt",
        displayName = "Cobalt Blue",
        primary = Color(0xFF3B82F6),
        secondary = Color(0xFF60A5FA),
        tertiary = Color(0xFF2563EB),
        glowColor = Color(0x333B82F6),
        description = "Deep oceanic royal sapphire",
        swatch = listOf(Color(0xFF2563EB), Color(0xFF3B82F6), Color(0xFF60A5FA))
    ),
    LIME(
        id = "lime",
        displayName = "Neon Lime",
        primary = Color(0xFF84CC16),
        secondary = Color(0xFFA3E635),
        tertiary = Color(0xFF65A30D),
        glowColor = Color(0x3384CC16),
        description = "Acid lime & cyber energy",
        swatch = listOf(Color(0xFF65A30D), Color(0xFF84CC16), Color(0xFFA3E635))
    ),
    RUBY(
        id = "ruby",
        displayName = "Crimson Ruby",
        primary = Color(0xFFEF4444),
        secondary = Color(0xFFF87171),
        tertiary = Color(0xFFDC2626),
        glowColor = Color(0x33EF4444),
        description = "Bold scarlet & intense ruby",
        swatch = listOf(Color(0xFFDC2626), Color(0xFFEF4444), Color(0xFFF87171))
    ),
    CORAL(
        id = "coral",
        displayName = "Warm Coral",
        primary = Color(0xFFFB923C),
        secondary = Color(0xFFFDBA74),
        tertiary = Color(0xFFEA580C),
        glowColor = Color(0x33FB923C),
        description = "Tropical coral & tangerine warmth",
        swatch = listOf(Color(0xFFEA580C), Color(0xFFFB923C), Color(0xFFFDBA74))
    ),
    LAVENDER(
        id = "lavender",
        displayName = "Amethyst Orchid",
        primary = Color(0xFFA855F7),
        secondary = Color(0xFFC084FC),
        tertiary = Color(0xFF9333EA),
        glowColor = Color(0x33A855F7),
        description = "Mystic amethyst & royal orchid",
        swatch = listOf(Color(0xFF9333EA), Color(0xFFA855F7), Color(0xFFC084FC))
    ),
    TEAL(
        id = "teal",
        displayName = "Matrix Teal",
        primary = Color(0xFF14B8A6),
        secondary = Color(0xFF2DD4BF),
        tertiary = Color(0xFF0D9488),
        glowColor = Color(0x3314B8A6),
        description = "Deep cybernetic sea turquoise",
        swatch = listOf(Color(0xFF0D9488), Color(0xFF14B8A6), Color(0xFF2DD4BF))
    ),
    PLATINUM(
        id = "platinum",
        displayName = "Titanium Silver",
        primary = Color(0xFFE2E8F0),
        secondary = Color(0xFFF8FAFC),
        tertiary = Color(0xFF94A3B8),
        glowColor = Color(0x33E2E8F0),
        description = "Sleek metallic monochrome",
        swatch = listOf(Color(0xFF94A3B8), Color(0xFFE2E8F0), Color(0xFFF8FAFC))
    );

    companion object {
        fun fromId(id: String): AppThemeColor = entries.find { it.id == id } ?: EMERALD
    }
}

enum class ThemeMode(val id: String, val displayName: String) {
    SYSTEM("system", "System Default"),
    DARK("dark", "Dark Mode"),
    LIGHT("light", "Light Mode");

    companion object {
        fun fromId(id: String): ThemeMode = entries.find { it.id == id } ?: SYSTEM
    }
}

object AppThemeManager {
    private val _themeFlow = MutableStateFlow(AppThemeColor.EMERALD)
    val themeFlow: StateFlow<AppThemeColor> = _themeFlow.asStateFlow()

    private val _themeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    val themeModeFlow: StateFlow<ThemeMode> = _themeModeFlow.asStateFlow()
    private val _overlayThemeModeFlow = MutableStateFlow(ThemeMode.SYSTEM)
    val overlayThemeModeFlow: StateFlow<ThemeMode> = _overlayThemeModeFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val savedId = prefs.getString("app_theme_color", AppThemeColor.EMERALD.id) ?: AppThemeColor.EMERALD.id
        _themeFlow.value = AppThemeColor.fromId(savedId)
        val savedModeId = prefs.getString("app_theme_mode", ThemeMode.SYSTEM.id) ?: ThemeMode.SYSTEM.id
        _themeModeFlow.value = ThemeMode.fromId(savedModeId)
        val savedOverlayModeId = prefs.getString("overlay_theme_mode", ThemeMode.SYSTEM.id) ?: ThemeMode.SYSTEM.id
        _overlayThemeModeFlow.value = ThemeMode.fromId(savedOverlayModeId)
    }

    fun setTheme(context: Context, theme: AppThemeColor) {
        _themeFlow.value = theme
        val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme_color", theme.id).apply()
    }

    fun setOverlayThemeMode(context: Context, mode: ThemeMode) {
        _overlayThemeModeFlow.value = mode
        val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("overlay_theme_mode", mode.id).apply()
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeModeFlow.value = mode
        val prefs = context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme_mode", mode.id).apply()
    }
}
