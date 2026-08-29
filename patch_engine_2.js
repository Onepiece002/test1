const fs = require('fs');
const path = './app/src/main/java/com/focusbyrj/app/util/AyvaTalkEngine.kt';
let code = fs.readFileSync(path, 'utf8');

const targetStr = `        // --- 4. INTENT EXTRACTION (e.g. "set wait timer to 15") ---
        if (bestTopic != null && (cleanQuery.startsWith("set ") || cleanQuery.startsWith("change ") || cleanQuery.startsWith("turn ") || cleanQuery.startsWith("toggle ") || cleanQuery.contains("set ") || cleanQuery.contains("turn ")) && context != null) {
            val isBooleanTurnOn = cleanQuery.contains(" on") || cleanQuery.contains("enable") || cleanQuery.contains("true")
            val isBooleanTurnOff = cleanQuery.contains(" off") || cleanQuery.contains("disable") || cleanQuery.contains("false")
            
            var num = cleanQuery.replace(Regex("[^0-9]"), "").toIntOrNull()
            var stringVal: String? = null
            
            // Extract potential string values based on typical settings
            if (bestTopic.prefType == "string") {
                if (cleanQuery.contains("dark")) stringVal = "dark"
                else if (cleanQuery.contains("light")) stringVal = "light"
                else if (cleanQuery.contains("system")) stringVal = "system"
                else if (cleanQuery.contains("banner") || cleanQuery.contains("pop")) stringVal = "Banner"
                else if (cleanQuery.contains("silent")) stringVal = "Silent"
                else if (cleanQuery.contains("both")) stringVal = "Both"
                else if (cleanQuery.contains("dashboard")) stringVal = "dashboard"
                else if (cleanQuery.contains("time") || cleanQuery.contains("analytics")) stringVal = "time"
                else if (cleanQuery.contains("account") || cleanQuery.contains("profile")) stringVal = "account"
                else if (cleanQuery.contains("schedule")) stringVal = "schedules"
            }
            
            if (num != null && bestTopic.prefKey != null && bestTopic.prefType == "int") {
                if ((cleanQuery.contains("hour") || cleanQuery.contains("hr")) && num < 24) {
                    num *= 60
                }
                context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                    .edit().putInt(bestTopic.prefKey!!, num).apply()
                
                val displayVal = if (num >= 60 && num % 60 == 0) "${num / 60} hour(s)" else if (bestTopic.id.contains("minute")) "$num minute(s)" else if (bestTopic.id.contains("hour")) "$num hour(s)" else "$num"
                return TalkResponse("✅ Direct Action: Changed ${bestTopic.title} to $displayVal.")
            } else if ((isBooleanTurnOn || isBooleanTurnOff) && bestTopic.prefKey != null && bestTopic.prefType == "boolean") {
                val finalBool = isBooleanTurnOn
                context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                    .edit().putBoolean(bestTopic.prefKey!!, finalBool).apply()
                
                return TalkResponse("✅ Direct Action: ${if(finalBool) "Enabled" else "Disabled"} ${bestTopic.title}.")
            } else if (stringVal != null && bestTopic.prefKey != null && bestTopic.prefType == "string") {
                context.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
                    .edit().putString(bestTopic.prefKey!!, stringVal).apply()
                return TalkResponse("✅ Direct Action: Changed ${bestTopic.title} to $stringVal.")
            }
        }`;

const replacementStr = `        // --- 4. INTENT EXTRACTION (e.g. "set wait timer to 15") ---
        if (bestTopic != null && (cleanQuery.startsWith("set ") || cleanQuery.startsWith("change ") || cleanQuery.startsWith("turn ") || cleanQuery.startsWith("toggle ") || cleanQuery.contains("set ") || cleanQuery.contains("turn ") || cleanQuery.contains("change ")) && context != null) {
            val isBooleanTurnOn = cleanQuery.contains(" on") || cleanQuery.contains("enable") || cleanQuery.contains("true")
            val isBooleanTurnOff = cleanQuery.contains(" off") || cleanQuery.contains("disable") || cleanQuery.contains("false")
            
            var num = cleanQuery.replace(Regex("[^0-9]"), "").toIntOrNull()
            var stringVal: String? = null
            
            // Extract potential string values based on typical settings
            if (bestTopic.prefType == "string") {
                val timeRegex = Regex("([0-9]{1,2})[:.]([0-9]{2})\\\\s*(am|pm)?", RegexOption.IGNORE_CASE)
                val timeMatch = timeRegex.find(cleanQuery)
                if (timeMatch != null) {
                    val hr = timeMatch.groupValues[1].toIntOrNull() ?: 12
                    val min = timeMatch.groupValues[2]
                    val ampm = (timeMatch.groupValues[3].uppercase().takeIf { it.isNotBlank() } ?: "AM")
                    stringVal = String.format("%02d:%s %s", hr, min, ampm)
                } else if (cleanQuery.contains("dark")) stringVal = "dark"
                else if (cleanQuery.contains("light")) stringVal = "light"
                else if (cleanQuery.contains("system")) stringVal = "system"
                else if (cleanQuery.contains("banner") || cleanQuery.contains("pop")) stringVal = "Banner"
                else if (cleanQuery.contains("silent")) stringVal = "Silent"
                else if (cleanQuery.contains("both")) stringVal = "Both"
                else if (cleanQuery.contains("dashboard")) stringVal = "dashboard"
                else if (cleanQuery.contains("time") || cleanQuery.contains("analytics")) stringVal = "time"
                else if (cleanQuery.contains("account") || cleanQuery.contains("profile")) stringVal = "account"
                else if (cleanQuery.contains("schedule")) stringVal = "schedules"
            }
            
            if (num != null && bestTopic.prefKey != null && bestTopic.prefType == "int") {
                if ((cleanQuery.contains("hour") || cleanQuery.contains("hr")) && num < 24) {
                    num *= 60
                }
                val displayVal = if (num >= 60 && num % 60 == 0) "\\${num / 60} hour(s)" else if (bestTopic.id.contains("minute")) "$num minute(s)" else if (bestTopic.id.contains("hour")) "$num hour(s)" else "$num"
                
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "int")
                    put("value", num.toString())
                    put("title", bestTopic!!.title)
                    put("displayVal", displayVal)
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to change *\\${bestTopic.title}* to *\\$displayVal*?",
                    jsonPayload = pendingJson
                )
            } else if ((isBooleanTurnOn || isBooleanTurnOff) && bestTopic.prefKey != null && bestTopic.prefType == "boolean") {
                val finalBool = isBooleanTurnOn
                val actionStr = if (finalBool) "Enable" else "Disable"
                
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "boolean")
                    put("value", finalBool.toString())
                    put("title", bestTopic!!.title)
                    put("displayVal", if(finalBool) "Enabled" else "Disabled")
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to *\\$actionStr* *\\${bestTopic.title}*?",
                    jsonPayload = pendingJson
                )
            } else if (stringVal != null && bestTopic.prefKey != null && bestTopic.prefType == "string") {
                val pendingJson = JSONObject().apply {
                    put("topicId", bestTopic!!.id)
                    put("prefKey", bestTopic!!.prefKey)
                    put("prefType", "string")
                    put("value", stringVal)
                    put("title", bestTopic!!.title)
                    put("displayVal", stringVal)
                    put("status", "pending")
                }.toString()
                
                return TalkResponse(
                    formattedText = "Do you want to change *\\${bestTopic.title}* to *\\$stringVal*?",
                    jsonPayload = pendingJson
                )
            }
        }`;

code = code.replace(targetStr.replace(/\$/g, '\\$'), replacementStr);
fs.writeFileSync(path, code);
