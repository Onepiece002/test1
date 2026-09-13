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
}

object KeepColorPalette {
    val Default = KeepColorTheme(
        key = "default",
        name = "Default",
        lightBg = Color(0xFFFFFFFF),
        darkBg = Color(0xFF1E1F22),
        lightBorder = Color(0xFFE0E0E0),
        darkBorder = Color(0xFF2E3138),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE8EAED),
        swatchColor = Color(0xFF9AA0A6)
    )

    val Coral = KeepColorTheme(
        key = "coral",
        name = "Coral",
        lightBg = Color(0xFFF28B82),
        darkBg = Color(0xFF77172E),
        lightBorder = Color(0xFFEA4335).copy(alpha = 0.4f),
        darkBorder = Color(0xFF9E233E),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFFCE8E6),
        swatchColor = Color(0xFFF28B82)
    )

    val Peach = KeepColorTheme(
        key = "peach",
        name = "Peach",
        lightBg = Color(0xFFFBBC04),
        darkBg = Color(0xFF692B17),
        lightBorder = Color(0xFFF29900).copy(alpha = 0.4f),
        darkBorder = Color(0xFF8C3E22),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFFEF7E0),
        swatchColor = Color(0xFFFBBC04)
    )

    val Sand = KeepColorTheme(
        key = "sand",
        name = "Sand",
        lightBg = Color(0xFFFFF475),
        darkBg = Color(0xFF7C4A03),
        lightBorder = Color(0xFFF9AB00).copy(alpha = 0.4f),
        darkBorder = Color(0xFF9E650C),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFFEF7E0),
        swatchColor = Color(0xFFFFF475)
    )

    val Mint = KeepColorTheme(
        key = "mint",
        name = "Mint",
        lightBg = Color(0xFFCCFF90),
        darkBg = Color(0xFF264D3B),
        lightBorder = Color(0xFF5BB974).copy(alpha = 0.4f),
        darkBorder = Color(0xFF336850),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE6F4EA),
        swatchColor = Color(0xFFCCFF90)
    )

    val Sage = KeepColorTheme(
        key = "sage",
        name = "Sage",
        lightBg = Color(0xFFA7FFEB),
        darkBg = Color(0xFF0C625D),
        lightBorder = Color(0xFF12B5CB).copy(alpha = 0.4f),
        darkBorder = Color(0xFF137E77),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE4F7FB),
        swatchColor = Color(0xFFA7FFEB)
    )

    val Fog = KeepColorTheme(
        key = "fog",
        name = "Fog",
        lightBg = Color(0xFFCBF0F8),
        darkBg = Color(0xFF256377),
        lightBorder = Color(0xFF24A1DE).copy(alpha = 0.4f),
        darkBorder = Color(0xFF327C94),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE8F0FE),
        swatchColor = Color(0xFFCBF0F8)
    )

    val Storm = KeepColorTheme(
        key = "storm",
        name = "Storm",
        lightBg = Color(0xFFAECBFA),
        darkBg = Color(0xFF284255),
        lightBorder = Color(0xFF4285F4).copy(alpha = 0.4f),
        darkBorder = Color(0xFF38576E),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE8F0FE),
        swatchColor = Color(0xFFAECBFA)
    )

    val Dusk = KeepColorTheme(
        key = "dusk",
        name = "Dusk",
        lightBg = Color(0xFFD7AEFB),
        darkBg = Color(0xFF472E5B),
        lightBorder = Color(0xFFA142F4).copy(alpha = 0.4f),
        darkBorder = Color(0xFF5E3F78),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFF3E8FD),
        swatchColor = Color(0xFFD7AEFB)
    )

    val Blossom = KeepColorTheme(
        key = "blossom",
        name = "Blossom",
        lightBg = Color(0xFFFDCFE8),
        darkBg = Color(0xFF6C394F),
        lightBorder = Color(0xFFE52592).copy(alpha = 0.4f),
        darkBorder = Color(0xFF884B65),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFFCE8E6),
        swatchColor = Color(0xFFFDCFE8)
    )

    val Clay = KeepColorTheme(
        key = "clay",
        name = "Clay",
        lightBg = Color(0xFFE6C9A8),
        darkBg = Color(0xFF4B443A),
        lightBorder = Color(0xFF935D25).copy(alpha = 0.4f),
        darkBorder = Color(0xFF625B4E),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFF1EFEA),
        swatchColor = Color(0xFFE6C9A8)
    )

    val Chalk = KeepColorTheme(
        key = "chalk",
        name = "Chalk",
        lightBg = Color(0xFFE8EAED),
        darkBg = Color(0xFF232427),
        lightBorder = Color(0xFFBDC1C6).copy(alpha = 0.5f),
        darkBorder = Color(0xFF3C4043),
        lightText = Color(0xFF202124),
        darkText = Color(0xFFE8EAED),
        swatchColor = Color(0xFFE8EAED)
    )

    val allColors: List<KeepColorTheme> = listOf(
        Default, Coral, Peach, Sand, Mint, Sage, Fog, Storm, Dusk, Blossom, Clay, Chalk
    )

    fun getColor(key: String?): KeepColorTheme {
        return allColors.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: Default
    }
}
