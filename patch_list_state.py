with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'r') as f:
    content = f.read()

replacement = """    val updateFontSize = { newSize: Float ->
        val clamped = newSize.coerceIn(12f, 24f)
        chatFontSizeSp = clamped
        prefs.edit().putFloat("chat_font_size_sp", clamped).apply()
    }
    
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }"""

content = content.replace("""    val updateFontSize = { newSize: Float ->
        val clamped = newSize.coerceIn(12f, 24f)
        chatFontSizeSp = clamped
        prefs.edit().putFloat("chat_font_size_sp", clamped).apply()
    }""", replacement)

with open('app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt', 'w') as f:
    f.write(content)
