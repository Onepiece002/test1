import java.io.File

fun main() {
    val lines = File("app/src/main/java/com/focusbyrj/app/ui/screens/BubbleChatActivity.kt").readLines()
    var openCount = 0
    var closeCount = 0
    var inString = false
    var inBlockComment = false
    
    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trim()
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (inBlockComment) {
                if (c == '*' && i + 1 < line.length && line[i+1] == '/') {
                    inBlockComment = false
                    i += 2
                    continue
                }
                i++
                continue
            }
            if (inString) {
                if (c == '\\') {
                    i += 2
                    continue
                }
                if (c == '"') {
                    inString = false
                    i++
                    continue
                }
                i++
                continue
            }
            if (c == '/' && i + 1 < line.length && line[i+1] == '/') {
                break
            }
            if (c == '/' && i + 1 < line.length && line[i+1] == '*') {
                inBlockComment = true
                i += 2
                continue
            }
            if (c == '"') {
                if (i + 2 < line.length && line[i+1] == '"' && line[i+2] == '"') {
                    // raw string not handled precisely, but let's check
                    i += 3
                    continue
                }
                inString = true
                i++
                continue
            }
            if (c == '{') {
                openCount++
            } else if (c == '}') {
                closeCount++
            }
            i++
        }
        if (openCount != closeCount) {
            // print some milestones
            if (index in listOf(240, 1100, 1110, 1200, 1475, 1490, 1635, 1700, 1705, 1710, 1711, 2748)) {
                println("Line ${index + 1}: opens=$openCount closes=$closeCount diff=${openCount - closeCount}")
            }
        }
    }
    println("FINAL: opens=$openCount, closes=$closeCount, diff=${openCount - closeCount}")
}
