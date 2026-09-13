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

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

data class KeepColorTheme(
    val key: String,
    val name: String,
    val lightBg: Color,
    val darkBg: Color,
    val lightBorder: Color,
    val darkBorder: Color,
    val lightText: Color,
    val darkText: Color,
    val swatchColor: Color
) {
    fun getBackgroundColor(isDark: Boolean): Color = if (isDark) darkBg else lightBg
    fun getBorderColor(isDark: Boolean): Color = if (isDark) darkBorder else lightBorder
    fun getTextColor(isDark: Boolean): Color = if (isDark) darkText else lightText

    @Composable
    @ReadOnlyComposable
    fun resolveBackgroundColor(isDark: Boolean): Color {
        return if (key.equals("default", ignoreCase = true)) {
            MaterialTheme.colorScheme.surface
        } else {
            getBackgroundColor(isDark)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun resolveBorderColor(isDark: Boolean): Color {
        return if (key.equals("default", ignoreCase = true)) {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
        } else {
            getBorderColor(isDark)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun resolveTextColor(isDark: Boolean): Color {
        return if (key.equals("default", ignoreCase = true)) {
            MaterialTheme.colorScheme.onSurface
        } else {
            getTextColor(isDark)
        }
    }

    @Composable
    @ReadOnlyComposable
    fun resolveSecondaryTextColor(isDark: Boolean): Color {
        return if (key.equals("default", ignoreCase = true)) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            getTextColor(isDark).copy(alpha = 0.75f)
        }
    }
}

object KeepColorPalette {
    val Default = KeepColorTheme(
        key = "default",
        name = "Default",
        lightBg = Color(0xFFFFFFFF),
        darkBg = Color(0xFF151921),
        lightBorder = Color(0xFFE0E0E0),
        darkBorder = Color(0xFF2E3138),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE8EAED),
        swatchColor = Color(0xFF9AA0A6)
    )

    val Coral = KeepColorTheme(
        key = "coral",
        name = "Coral",
        lightBg = Color(0xFFFFEBEE),
        darkBg = Color(0xFF4C1D24),
        lightBorder = Color(0xFFE57373).copy(alpha = 0.45f),
        darkBorder = Color(0xFF7A2E3A),
        lightText = Color(0xFF371E22),
        darkText = Color(0xFFFCE8E6),
        swatchColor = Color(0xFFEF9A9A)
    )

    val Peach = KeepColorTheme(
        key = "peach",
        name = "Peach",
        lightBg = Color(0xFFFFF3E0),
        darkBg = Color(0xFF4E2A18),
        lightBorder = Color(0xFFFFB74D).copy(alpha = 0.45f),
        darkBorder = Color(0xFF7E4224),
        lightText = Color(0xFF382316),
        darkText = Color(0xFFFEF0E4),
        swatchColor = Color(0xFFFFCC80)
    )

    val Sand = KeepColorTheme(
        key = "sand",
        name = "Sand",
        lightBg = Color(0xFFFFFDE7),
        darkBg = Color(0xFF483E15),
        lightBorder = Color(0xFFFFF176).copy(alpha = 0.5f),
        darkBorder = Color(0xFF74641E),
        lightText = Color(0xFF363212),
        darkText = Color(0xFFFEFBE8),
        swatchColor = Color(0xFFFFF59D)
    )

    val Mint = KeepColorTheme(
        key = "mint",
        name = "Mint",
        lightBg = Color(0xFFE8F8F0),
        darkBg = Color(0xFF1B3D2F),
        lightBorder = Color(0xFF81C784).copy(alpha = 0.45f),
        darkBorder = Color(0xFF2C5E4A),
        lightText = Color(0xFF142F24),
        darkText = Color(0xFFE6F4EA),
        swatchColor = Color(0xFFA5D6A7)
    )

    val Sage = KeepColorTheme(
        key = "sage",
        name = "Sage",
        lightBg = Color(0xFFE0F2F1),
        darkBg = Color(0xFF133E3B),
        lightBorder = Color(0xFF4DB6AC).copy(alpha = 0.45f),
        darkBorder = Color(0xFF205E59),
        lightText = Color(0xFF102E2B),
        darkText = Color(0xFFE4F7FB),
        swatchColor = Color(0xFF80CBC4)
    )

    val Fog = KeepColorTheme(
        key = "fog",
        name = "Fog",
        lightBg = Color(0xFFE1F5FE),
        darkBg = Color(0xFF17384A),
        lightBorder = Color(0xFF4FC3F7).copy(alpha = 0.45f),
        darkBorder = Color(0xFF255773),
        lightText = Color(0xFF122C3A),
        darkText = Color(0xFFE8F4FD),
        swatchColor = Color(0xFF90CAF9)
    )

    val Storm = KeepColorTheme(
        key = "storm",
        name = "Storm",
        lightBg = Color(0xFFE8EAF6),
        darkBg = Color(0xFF212B47),
        lightBorder = Color(0xFF7986CB).copy(alpha = 0.45f),
        darkBorder = Color(0xFF33436F),
        lightText = Color(0xFF1A223B),
        darkText = Color(0xFFE8EAF6),
        swatchColor = Color(0xFF9FA8DA)
    )

    val Dusk = KeepColorTheme(
        key = "dusk",
        name = "Dusk",
        lightBg = Color(0xFFF3E5F5),
        darkBg = Color(0xFF382346),
        lightBorder = Color(0xFFBA68C8).copy(alpha = 0.45f),
        darkBorder = Color(0xFF5A3970),
        lightText = Color(0xFF2B1838),
        darkText = Color(0xFFF3E8FD),
        swatchColor = Color(0xFFCE93D8)
    )

    val Blossom = KeepColorTheme(
        key = "blossom",
        name = "Blossom",
        lightBg = Color(0xFFFCE4EC),
        darkBg = Color(0xFF491E32),
        lightBorder = Color(0xFFF06292).copy(alpha = 0.45f),
        darkBorder = Color(0xFF752F50),
        lightText = Color(0xFF381525),
        darkText = Color(0xFFFCE8F0),
        swatchColor = Color(0xFFF48FB1)
    )

    val Clay = KeepColorTheme(
        key = "clay",
        name = "Clay",
        lightBg = Color(0xFFEFEBE9),
        darkBg = Color(0xFF3E322E),
        lightBorder = Color(0xFFA1887F).copy(alpha = 0.45f),
        darkBorder = Color(0xFF614E48),
        lightText = Color(0xFF332723),
        darkText = Color(0xFFF1EFEA),
        swatchColor = Color(0xFFBCAAA4)
    )

    val Chalk = KeepColorTheme(
        key = "chalk",
        name = "Chalk",
        lightBg = Color(0xFFECEFF1),
        darkBg = Color(0xFF263238),
        lightBorder = Color(0xFF90A4AE).copy(alpha = 0.45f),
        darkBorder = Color(0xFF3E4E56),
        lightText = Color(0xFF1E282C),
        darkText = Color(0xFFECEFF1),
        swatchColor = Color(0xFFB0BEC5)
    )

    val allColors: List<KeepColorTheme> = listOf(
        Default, Coral, Peach, Sand, Mint, Sage, Fog, Storm, Dusk, Blossom, Clay, Chalk
    )

    fun getColor(key: String?): KeepColorTheme {
        return allColors.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Default
    }
}
