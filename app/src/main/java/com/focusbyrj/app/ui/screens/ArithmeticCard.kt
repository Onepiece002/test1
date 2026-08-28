package com.focusbyrj.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.FocusEconomyManager
import org.json.JSONObject

@Composable
fun ArithmeticCard(
    message: ChatMessage, 
    fontSizeSp: Float,
    isActiveDrill: Boolean = false,
    onAnswered: ((Boolean) -> Unit)? = null,
    onEndDrill: (() -> Unit)? = null
) {
    var selectedIndex by rememberSaveable(message.id) { mutableStateOf<Int?>(null) }

    val json = message.arithmeticJson ?: return
    val title: String
    val questionText: String
    val options: List<String>
    val correctIndex: Int
    val explanation: String

    try {
        val obj = JSONObject(json)
        title = obj.getString("title")
        questionText = obj.getString("questionText")
        val arr = obj.getJSONArray("options")
        val parsedOptions = mutableListOf<String>()
        for (i in 0 until arr.length()) parsedOptions.add(arr.getString(i))
        options = parsedOptions
        correctIndex = obj.getInt("correctIndex")
        explanation = obj.getString("explanation")
    } catch (e: Exception) {
        Text("Error loading drill.", modifier = Modifier.padding(16.dp))
        return
    }

    val isDark = isSystemInDarkTheme()
    val greenBg = if (isDark) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
    val redBg = if (isDark) Color(0xFFB71C1C) else Color(0xFFFFEBEE)
    val greenFg = if (isDark) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    val redFg = if (isDark) Color(0xFFEF9A9A) else Color(0xFFC62828)
    val greenBorder = if (isDark) Color(0xFF4CAF50) else Color(0xFF4CAF50)
    val redBorder = if (isDark) Color(0xFFEF5350) else Color(0xFFEF5350)

    Column(modifier = Modifier.padding(16.dp)) {
        // Tag Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }

        // Question
        Text(
            text = questionText,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = (fontSizeSp + 4f).sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Options Grid
        options.forEachIndexed { index, option ->
            val isSelected = selectedIndex == index
            val isCorrect = index == correctIndex
            val showAsCorrect = selectedIndex != null && isCorrect
            val showAsWrong = isSelected && !isCorrect

            val backgroundColor = when {
                showAsCorrect -> greenBg
                showAsWrong -> redBg
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }

            val contentColor = when {
                showAsCorrect -> greenFg
                showAsWrong -> redFg
                else -> MaterialTheme.colorScheme.onSurface
            }

            val borderColor = when {
                showAsCorrect -> greenBorder
                showAsWrong -> redBorder
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clickable(enabled = selectedIndex == null) {
                            selectedIndex = index
                            val isCorrectAnswer = (index == correctIndex)
                            if (isCorrectAnswer) {
                                FocusEconomyManager.addRewards(baseXp = 40, baseGold = 20)
                            }
                            if (isActiveDrill) {
                                onAnswered?.invoke(isCorrectAnswer)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = (fontSizeSp + 1f).sp),
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (showAsCorrect) {
                        Text("✅", fontSize = 18.sp)
                    } else if (showAsWrong) {
                        Text("❌", fontSize = 18.sp)
                    }
                }
            }
        }

        // Explanation & Gamification Reveal
        AnimatedVisibility(visible = selectedIndex != null) {
            Column(modifier = Modifier.padding(top = 12.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 12.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = fontSizeSp.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        if (isActiveDrill) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    text = "End Drill",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clickable { onEndDrill?.invoke() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
