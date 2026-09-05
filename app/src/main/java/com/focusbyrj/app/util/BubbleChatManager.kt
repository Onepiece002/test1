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
import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class PersistedChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isArithmetic: Boolean = false,
    val arithmeticJson: String? = null,
    val isDrillSummary: Boolean = false,
    val drillSummaryJson: String? = null,
    val isAptitudeProfile: Boolean = false,
    val isStreakPrompt: Boolean = false,
    val streakPromptJson: String? = null,
    val isTaskSummary: Boolean = false,
    val taskSummaryJson: String? = null,
    val isTalkAction: Boolean = false,
    val talkActionJson: String? = null,
    val pendingActionJson: String? = null
)

object BubbleChatManager {
    private const val PREFS_NAME = "bubble_chat_prefs"
    private const val KEY_MESSAGES = "chat_messages_json"
    private const val KEY_UNREAD_COUNT = "unread_message_count"
    private const val KEY_LAST_ACTIVITY = "last_chat_activity_timestamp"
    private const val INACTIVITY_TIMEOUT_MS = 10 * 60 * 1000L // 10 minutes

    const val ACTION_UNREAD_COUNT_CHANGED = "com.focusbyrj.app.UNREAD_COUNT_CHANGED"
    const val ACTION_MESSAGES_CHANGED = "com.focusbyrj.app.CHAT_MESSAGES_CHANGED"

    private val _unreadCountFlow = MutableStateFlow(0)
    val unreadCountFlow: StateFlow<Int> = _unreadCountFlow.asStateFlow()

    private val _messagesFlow = MutableStateFlow<List<PersistedChatMessage>>(emptyList())
    val messagesFlow: StateFlow<List<PersistedChatMessage>> = _messagesFlow.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _unreadCountFlow.value = prefs.getInt(KEY_UNREAD_COUNT, 0)
        _messagesFlow.value = getMessages(context)
    }

    fun getUnreadCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_UNREAD_COUNT, 0)
        _unreadCountFlow.value = count
        return count
    }

    fun setUnreadCount(context: Context, count: Int) {
        val safeCount = count.coerceAtLeast(0)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_UNREAD_COUNT, safeCount).apply()
        _unreadCountFlow.value = safeCount
        context.sendBroadcast(Intent(ACTION_UNREAD_COUNT_CHANGED))
    }

    fun incrementUnread(context: Context) {
        val current = getUnreadCount(context)
        setUnreadCount(context, current + 1)
    }

    fun clearUnread(context: Context) {
        setUnreadCount(context, 0)
    }

    fun updateLastActivityTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
    }

    fun isInactiveTimeout(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
        if (lastActivity == 0L) return false
        if (getUnreadCount(context) > 0) return false
        return (System.currentTimeMillis() - lastActivity) > INACTIVITY_TIMEOUT_MS
    }

    fun checkAndClearIfInactive(context: Context): Boolean {
        if (isInactiveTimeout(context)) {
            clearMessages(context)
            return true
        }
        return false
    }

    fun getMessages(context: Context): List<PersistedChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        val list = mutableListOf<PersistedChatMessage>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    PersistedChatMessage(
                        id = obj.optString("id", System.currentTimeMillis().toString()),
                        text = obj.optString("text", ""),
                        isUser = obj.optBoolean("isUser", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        isArithmetic = obj.optBoolean("isArithmetic", false),
                        arithmeticJson = if (obj.has("arithmeticJson") && !obj.isNull("arithmeticJson")) obj.optString("arithmeticJson", null) else null,
                        isDrillSummary = obj.optBoolean("isDrillSummary", false),
                        drillSummaryJson = if (obj.has("drillSummaryJson") && !obj.isNull("drillSummaryJson")) obj.optString("drillSummaryJson", null) else null,
                        isAptitudeProfile = obj.optBoolean("isAptitudeProfile", false),
                        isStreakPrompt = obj.optBoolean("isStreakPrompt", false),
                        streakPromptJson = if (obj.has("streakPromptJson") && !obj.isNull("streakPromptJson")) obj.optString("streakPromptJson", null) else null,
                        isTaskSummary = obj.optBoolean("isTaskSummary", false),
                        taskSummaryJson = if (obj.has("taskSummaryJson") && !obj.isNull("taskSummaryJson")) obj.optString("taskSummaryJson", null) else null,
                        isTalkAction = obj.optBoolean("isTalkAction", false),
                        talkActionJson = if (obj.has("talkActionJson") && !obj.isNull("talkActionJson")) obj.optString("talkActionJson", null) else null,
                        pendingActionJson = if (obj.has("pendingActionJson") && !obj.isNull("pendingActionJson")) obj.optString("pendingActionJson", null) else null
                    )
                )
            }
        } catch (_: Exception) {}
        _messagesFlow.value = list
        return list
    }

    fun saveMessages(context: Context, messages: List<PersistedChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val jsonArray = JSONArray()
            // Keep last 50 messages to keep storage small and snappy
            val trimmed = if (messages.size > 50) messages.takeLast(50) else messages
            trimmed.forEach { msg ->
                val obj = JSONObject().apply {
                    put("id", msg.id)
                    put("text", msg.text)
                    put("isUser", msg.isUser)
                    put("timestamp", msg.timestamp)
                    put("isArithmetic", msg.isArithmetic)
                    put("arithmeticJson", msg.arithmeticJson)
                    put("isDrillSummary", msg.isDrillSummary)
                    put("drillSummaryJson", msg.drillSummaryJson)
                    put("isAptitudeProfile", msg.isAptitudeProfile)
                    put("isStreakPrompt", msg.isStreakPrompt)
                    put("streakPromptJson", msg.streakPromptJson)
                    put("isTaskSummary", msg.isTaskSummary)
                    put("taskSummaryJson", msg.taskSummaryJson)
                    put("isTalkAction", msg.isTalkAction)
                    put("talkActionJson", msg.talkActionJson)
                    put("pendingActionJson", msg.pendingActionJson)
                }
                jsonArray.put(obj)
            }
            prefs.edit()
                .putString(KEY_MESSAGES, jsonArray.toString())
                .putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis())
                .apply()
            _messagesFlow.value = trimmed
            context.sendBroadcast(Intent(ACTION_MESSAGES_CHANGED))
        } catch (_: Exception) {}
    }

    fun addMessage(context: Context, message: PersistedChatMessage, incrementBadge: Boolean = false) {
        val current = getMessages(context).toMutableList()
        current.add(message)
        saveMessages(context, current)
        if (incrementBadge) {
            incrementUnread(context)
        }
    }

    fun updateMessage(context: Context, updatedMessage: PersistedChatMessage) {
        val current = getMessages(context).toMutableList()
        val index = current.indexOfFirst { it.id == updatedMessage.id }
        if (index != -1) {
            current[index] = updatedMessage
            saveMessages(context, current)
        }
    }

    fun clearMessages(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_MESSAGES).putLong(KEY_LAST_ACTIVITY, System.currentTimeMillis()).apply()
        _messagesFlow.value = emptyList()
        clearUnread(context)
        context.sendBroadcast(Intent(ACTION_MESSAGES_CHANGED))
    }
}
