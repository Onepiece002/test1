package com.focusbyrj.app.util

import android.content.Context

object AyvaHelpEngine {
    data class HelpTopic(
        val id: String,
        val keywords: List<String>,
        val title: String,
        val description: String,
        val steps: List<String> = emptyList(),
        val troubleshooting: String? = null,
        val fullFormattedText: String? = null
    ) {
        fun formatResponse(): String {
            if (!fullFormattedText.isNullOrBlank()) {
                return fullFormattedText
            }
            val sb = StringBuilder()
            sb.append("💡 *__${title}__*\n")
            sb.append("_").append(description).append("_\n\n")
            
            if (steps.isNotEmpty()) {
                steps.forEachIndexed { index, step ->
                    sb.append("${index + 1}. $step\n")
                }
            }
            
            if (troubleshooting != null) {
                sb.append("\n⚠️ *Troubleshooting:*\n$troubleshooting")
            }
            return sb.toString().trimEnd()
        }
    }

    fun searchHelp(query: String, context: Context? = null): HelpTopic? {
        val answer = kotlinx.coroutines.runBlocking { AyvaTalkEngine.answerTalkQuery(query, context) }
        return HelpTopic(
            id = "talk_result",
            keywords = emptyList(),
            title = "Help & Guide",
            description = "",
            fullFormattedText = answer
        )
    }
}
