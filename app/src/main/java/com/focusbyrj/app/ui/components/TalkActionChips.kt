package com.focusbyrj.app.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusbyrj.app.util.TalkAction
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TalkActionChips(
    talkActionJson: String?,
    fontSizeSp: Float = 15f,
    onActionApplied: ((String) -> Unit)? = null
) {
    if (talkActionJson.isNullOrBlank()) return

    val context = LocalContext.current
    var appliedActionKey by remember { mutableStateOf<String?>(null) }

    val parsedActions = remember(talkActionJson) {
        val list = mutableListOf<TalkAction>()
        try {
            val root = JSONObject(talkActionJson)
            val arr = root.optJSONArray("actions") ?: org.json.JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val type = obj.optString("type")
                val label = obj.optString("label")
                val emoji = obj.optString("emoji", "⚡")
                when (type) {
                    "navigate" -> {
                        val route = obj.optString("route")
                        list.add(TalkAction.NavigateAppScreen(route, label, emoji))
                    }
                    "ask_query" -> {
                        val query = obj.optString("query")
                        list.add(TalkAction.AskQuery(query, label, emoji))
                    }
                    "system_setting" -> {
                        val action = obj.optString("action")
                        val packageUri = obj.optBoolean("packageUri", false)
                        list.add(TalkAction.OpenSystemSetting(action, label, emoji, packageUri))
                    }
                    "pref_update" -> {
                        val key = obj.optString("prefKey")
                        val prefType = obj.optString("prefType")
                        val targetVal = obj.optString("targetValue")
                        val dispVal = obj.optString("displayValue")
                        list.add(TalkAction.DirectPrefUpdate(key, prefType, targetVal, dispVal, label, emoji))
                    }
                }
            }
        } catch (_: Exception) {}
        list
    }

    if (parsedActions.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            parsedActions.forEach { action ->
                val isSelected = when (action) {
                    is TalkAction.DirectPrefUpdate -> appliedActionKey == "${action.prefKey}_${action.targetValue}"
                    else -> false
                }

                Surface(
                    onClick = {
                        val success = action.execute(context)
                        if (success) {
                            if (action is TalkAction.DirectPrefUpdate) {
                                appliedActionKey = "${action.prefKey}_${action.targetValue}"
                                Toast.makeText(context, "Updated: ${action.label} ✅", Toast.LENGTH_SHORT).show()
                                onActionApplied?.invoke("Updated: ${action.label}")
                            } else {
                                Toast.makeText(context, "Opening ${action.label}...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = (fontSizeSp * 0.85f).coerceIn(11f, 14f).sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Text(text = "✓", fontSize = (fontSizeSp * 0.8f).sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
