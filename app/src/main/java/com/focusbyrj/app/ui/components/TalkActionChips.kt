package com.focusbyrj.app.ui.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
    onQueryClick: ((String) -> Unit)? = null,
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

    Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            parsedActions.forEach { action ->
                val isSelected = when (action) {
                    is TalkAction.DirectPrefUpdate -> appliedActionKey == "${action.prefKey}_${action.targetValue}"
                    else -> false
                }

                Surface(
                    onClick = {
                        if (action is TalkAction.AskQuery && onQueryClick != null) {
                            onQueryClick(action.query)
                        } else {
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
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    },
                    border = BorderStroke(
                        0.75.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) 
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                    ),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = action.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (fontSizeSp * 0.76f).coerceIn(10.5f, 13f).sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                letterSpacing = 0.1.sp
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                            }
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
