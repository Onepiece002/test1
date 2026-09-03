package com.focusbyrj.app.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import java.util.Calendar
import kotlin.random.Random

object AyvaDialogueEngine {
    private const val PREFS_NAME = "ayva_dialogue_prefs"

    // --- LEVENSHTEIN FUZZY MATCHING (No-AI Engine Upgrade) ---
    private fun levenshtein(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length
        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)
        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costInsert, costDelete, costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }

    private fun isFuzzyMatch(word: String, target: String, maxDistance: Int = 1): Boolean {
        if (word == target) return true
        if (kotlin.math.abs(word.length - target.length) > maxDistance) return false
        return levenshtein(word, target) <= maxDistance
    }

    private fun containsAnyFuzzy(text: String, keywords: List<String>, maxDistance: Int = 1): Boolean {
        val words = text.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }
        for (word in words) {
            for (keyword in keywords) {
                if (isFuzzyMatch(word, keyword, maxDistance)) {
                    return true
                }
            }
        }
        return false
    }

    // --- NON-REPEATING SHUFFLE DECK LOGIC ---
    private fun getNextFromDeck(context: Context, categoryKey: String, pool: List<String>): String {
        if (pool.isEmpty()) return ""
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedJson = prefs.getString("deck_$categoryKey", null)
        val remainingIndices = mutableListOf<Int>()
        if (storedJson != null) {
            kotlin.runCatching {
                val array = JSONArray(storedJson)
                for (i in 0 until array.length()) {
                    val idx = array.getInt(i)
                    if (idx in pool.indices) {
                        remainingIndices.add(idx)
                    }
                }
            }
        }
        if (remainingIndices.isEmpty()) {
            remainingIndices.addAll(pool.indices)
            remainingIndices.shuffle()
        }
        val chosenIndex = remainingIndices.removeAt(0)
        
        // Save updated deck
        val newArray = JSONArray()
        remainingIndices.forEach { newArray.put(it) }
        prefs.edit().putString("deck_$categoryKey", newArray.toString()).apply()
        
        return pool[chosenIndex]
    }

    // =========================================================================
    // 1. TASK CREATION DIALOGUES (Upgraded with Contextual Moods & Fuzzy NLP)
    // =========================================================================
    fun getTaskAddedResponse(
        context: Context,
        title: String,
        isPriority: Boolean,
        hasDueDate: Boolean,
        dueDateStr: String?,
        attrStr: String?
    ): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val lower = title.lowercase()
        val dueSuffix = if (!dueDateStr.isNullOrBlank()) " for $dueDateStr" else ""
        val attrSuffix = if (!attrStr.isNullOrBlank()) " $attrStr" else ""
        
        // Contextual Mood: Streak-Aware Responses
        val profile = AptitudeManager.profileFlow.value
        val streak = profile.currentStreak
        
        // If the user has a high streak, Ayva gets extremely hyped and acknowledging.
        val moodPrefix = if (streak >= 7) {
            "🔥 Week-long streak energy! "
        } else if (streak >= 3) {
            "⚡ You're on a roll! "
        } else {
            ""
        }

        // Context: Time of Day
        if (hour < 6) {
            val lateNightQuips = listOf(
                "${moodPrefix}Adding tasks in the dead of night? I admire the insomnia grind, but get some sleep soon! Logged: \"$title\"$dueSuffix.",
                "${moodPrefix}Late night inspiration strikes! Locked in \"$title\"$dueSuffix. Now please get some rest! 🌙",
                "${moodPrefix}Logged \"$title\"$dueSuffix at $hour AM. Future you will either thank you or wonder what you were drinking. 🦉",
                "${moodPrefix}Got it: \"$title\"$dueSuffix. You're working while the world sleeps — mysterious and productive.",
                "${moodPrefix}Midnight warrior mode activated. Added \"$title\"$dueSuffix to the queue. ☕"
            )
            return getNextFromDeck(context, "task_late_night", lateNightQuips)
        }
        if (hour in 6..8) {
            val earlyMorningQuips = listOf(
                "${moodPrefix}Up with the sun and already locking in goals? Respect. Added$attrSuffix: \"$title\"$dueSuffix. 🌅",
                "${moodPrefix}Early bird getting the worm! \"$title\"$dueSuffix is on today's hit list.",
                "${moodPrefix}Starting strong before the rest of the world wakes up. Added: \"$title\"$dueSuffix. ⚡",
                "${moodPrefix}Morning energy detected! Logged$attrSuffix: \"$title\"$dueSuffix. Let's make today count.",
                "${moodPrefix}Coffee in hand, goals in sight. Added \"$title\"$dueSuffix. Let's roll! ☕"
            )
            return getNextFromDeck(context, "task_early_morning", earlyMorningQuips)
        }

        // Context: High Priority
        if (isPriority) {
            val priorityQuips = listOf(
                "🚨 Top priority alert! Moved \"$title\"$dueSuffix straight to the VIP lounge of your task list.",
                "Red banner engaged! \"$title\"$dueSuffix is marked high priority. All eyes on this one! 🎯",
                "Marked as critical! \"$title\"$dueSuffix is at the top of my radar. Let's conquer it.",
                "Priority stamped! \"$title\"$dueSuffix is not to be messed around with today. 🔥",
                "Locked in as urgent: \"$title\"$dueSuffix. Don't let this one linger!"
            )
            return getNextFromDeck(context, "task_priority", priorityQuips)
        }

        // Context: Category-based witty reactions with FUZZY MATCHING (Synonyms & Typos)
        val fitnessSynonyms = listOf("gym", "workout", "run", "exercise", "lift", "jog", "sprint", "cardio", "marathon", "fitness", "yoga")
        if (containsAnyFuzzy(lower, fitnessSynonyms)) {
            val fitnessQuips = listOf(
                "${moodPrefix}Gains incoming! Added \"$title\"$dueSuffix. Don't skip leg day! 🏋️",
                "${moodPrefix}Locked in: \"$title\"$dueSuffix. Sweating today means flexing tomorrow. 💪",
                "${moodPrefix}Added \"$title\"$dueSuffix. Hydrate, conquer, repeat! 🏃",
                "${moodPrefix}Fitness item logged: \"$title\"$dueSuffix. Your future self is already feeling stronger."
            )
            return getNextFromDeck(context, "task_fitness", fitnessQuips)
        }

        val studySynonyms = listOf("study", "exam", "assignment", "read", "chapter", "homework", "quiz", "test", "essay", "learn")
        if (containsAnyFuzzy(lower, studySynonyms)) {
            val studyQuips = listOf(
                "${moodPrefix}Brain gains on the schedule! Added \"$title\"$dueSuffix. Focus mode primed! 📚",
                "${moodPrefix}Knowledge is power. Locked in: \"$title\"$dueSuffix. Let's absorb that info! 🧠",
                "${moodPrefix}Academic hustle logged: \"$title\"$dueSuffix. Coffee and deep focus incoming.",
                "${moodPrefix}Added study mission: \"$title\"$dueSuffix. Lock in and conquer the material!"
            )
            return getNextFromDeck(context, "task_study", studyQuips)
        }

        val devSynonyms = listOf("code", "bug", "deploy", "build", "git", "refactor", "programming", "developer", "backend", "frontend", "api")
        if (containsAnyFuzzy(lower, devSynonyms)) {
            val devQuips = listOf(
                "${moodPrefix}Zero bugs allowed! Added \"$title\"$dueSuffix. May your builds be green! 💻",
                "${moodPrefix}Locked in: \"$title\"$dueSuffix. Semicolons placed, compiler pacified. 🚀",
                "${moodPrefix}Dev task registered: \"$title\"$dueSuffix. Time to turn caffeine into clean code.",
                "${moodPrefix}Added: \"$title\"$dueSuffix. Git commit, git push, git productive!"
            )
            return getNextFromDeck(context, "task_coding", devQuips)
        }

        val choreSynonyms = listOf("clean", "laundry", "dish", "dishes", "room", "trash", "grocer", "groceries", "sweep", "mop", "vacuum")
        if (containsAnyFuzzy(lower, choreSynonyms)) {
            val choreQuips = listOf(
                "${moodPrefix}Adulting in progress! Added \"$title\"$dueSuffix. A clean space equals a clean mind. 🧹",
                "${moodPrefix}Domestic victory logged: \"$title\"$dueSuffix. Get it done and enjoy the peace! ✨",
                "${moodPrefix}Added \"$title\"$dueSuffix. Put on some music and knock it out in 10 minutes.",
                "${moodPrefix}Chore locked: \"$title\"$dueSuffix. You'll feel so much lighter once it's checked off."
            )
            return getNextFromDeck(context, "task_chore", choreQuips)
        }

        // Generic Witty Master Pool (Huge variety)
        val masterPool = listOf(
            "${moodPrefix}Locked and loaded! Added$attrSuffix: \"$title\"$dueSuffix. Let's make it happen! 😉",
            "${moodPrefix}On it! Added$attrSuffix: \"$title\"$dueSuffix. No backing out now! 🎯",
            "${moodPrefix}Gotcha! Added$attrSuffix: \"$title\"$dueSuffix. Future you says thanks! ✨",
            "${moodPrefix}Noted and penned into reality: \"$title\"$dueSuffix. You've got this!",
            "${moodPrefix}Consider it on my radar! Added$attrSuffix: \"$title\"$dueSuffix. ⚡",
            "${moodPrefix}Added \"$title\"$dueSuffix to the queue. One small step for you, one giant leap for today's productivity!",
            "${moodPrefix}Logged: \"$title\"$dueSuffix. Clean execution is the name of the game today. 🚀",
            "${moodPrefix}Done deal! \"$title\"$dueSuffix is officially in play.",
            "${moodPrefix}Added! \"$title\"$dueSuffix is scheduled. Ready whenever you are.",
            "${moodPrefix}Boom! \"$title\"$dueSuffix is safely recorded. Now the fun part: doing it! 🔥",
            "${moodPrefix}Stamped and stored: \"$title\"$dueSuffix. Let's check this off in style later.",
            "${moodPrefix}Your wish is my command. Added$attrSuffix: \"$title\"$dueSuffix. 📋",
            "${moodPrefix}Registered: \"$title\"$dueSuffix. I'll make sure you don't forget this one.",
            "${moodPrefix}In the books! Added \"$title\"$dueSuffix. Keep this awesome momentum rolling.",
            "${moodPrefix}Locked in! Added \"$title\"$dueSuffix. Distractions don't stand a chance.",
            "${moodPrefix}Aye aye! \"$title\"$dueSuffix has entered the arena. ⚔️",
            "${moodPrefix}Secured: \"$title\"$dueSuffix. Another puzzle piece added to today's roadmap.",
            "${moodPrefix}Added! \"$title\"$dueSuffix is lined up. Ready to crush it?",
            "${moodPrefix}Pen to paper, bits to bytes: \"$title\"$dueSuffix is live! ✨",
            "${moodPrefix}Logged$attrSuffix: \"$title\"$dueSuffix. Let's turn intentions into achievements!"
        )
        return getNextFromDeck(context, "task_master_pool", masterPool)
    }

    // =========================================================================
    // 2. RESCHEDULING & POSTPONING DIALOGUES
    // =========================================================================
    fun getRescheduleSuccessResponse(context: Context, taskTitle: String, newDueFormatted: String): String {
        val reschedulePool = listOf(
            "Shifted '$taskTitle' to $newDueFormatted. Procrastination noted, but handled! 😉",
            "Moved '$taskTitle' to $newDueFormatted. Strategic tactical retreat! 🎯",
            "Pushed '$taskTitle' to $newDueFormatted. Fresh timing, fresh energy!",
            "Rescheduled '$taskTitle' to $newDueFormatted. I won't judge, but future you is keeping score. ⏳",
            "Bumped '$taskTitle' to $newDueFormatted. Reset your focus and attack it then! ⚡",
            "Adjusted! '$taskTitle' is now set for $newDueFormatted. Breathe, recharge, conquer.",
            "Shifted '$taskTitle' to $newDueFormatted. Clean schedule balance restored! 🔄",
            "Moved to $newDueFormatted for '$taskTitle'. New deadline, zero excuses! 🚀",
            "Done! '$taskTitle' is rescheduled to $newDueFormatted. Let's make sure it doesn't run away again."
        )
        return getNextFromDeck(context, "reschedule_success", reschedulePool)
    }

    fun getPostponeAllResponse(context: Context, count: Int): String {
        val postponePool = listOf(
            "Shifted $count items to tomorrow. Declaring a clean slate for tonight! 🚀",
            "Moved $count tasks to tomorrow. Reset, recharge, and come back swinging in the morning! 🌅",
            "Pushed $count tasks forward. Tactical reset complete! Tomorrow we feast. ✨",
            "All $count items moved to tomorrow's roster. Time to rest your brain for now! 🌙",
            "Waved the magic wand: $count tasks shifted to tomorrow. Don't forget, tomorrow they mean business! 😉"
        )
        return getNextFromDeck(context, "postpone_all", postponePool)
    }

    fun getRescheduleHelpResponse(): String {
        val helpVariants = listOf(
            "Usage: /reschedule <number> <time/date>\n_Example: `/reschedule 1 4pm` or `/reschedule 2 tomorrow morning`_",
            "Need to bump a task? Format is: `/reschedule <task#>` `<time>`\n_Example: `/reschedule 1 tomorrow at 10am`_",
            "Usage: `/reschedule <number> <when>`\n_Example: `/reschedule 1 5pm` or `/reschedule 3 next monday`_"
        )
        return helpVariants.random()
    }

    // =========================================================================
    // 3. BRIEFINGS & SUMMARY HEADERS / PROMPTS
    // =========================================================================
    fun getSummaryGreeting(context: Context, isAll: Boolean, hour: Int, dayOfWeek: Int): String {
        if (isAll) {
            val allBriefings = listOf(
                "📋 *__Ayva's Master Briefing__*\n_Here's the full bird's-eye view of everything on deck:_\n",
                "📋 *__The Grand Ledger with Ayva__*\n_Everything pending across all horizons:_\n",
                "📋 *__Master Task Radar__*\n_All open loops in your universe right now:_\n"
            )
            return getNextFromDeck(context, "briefing_all", allBriefings)
        }

        if (hour < 12) {
            val morningBriefings = listOf(
                "🌅 *__Morning Check-In with Ayva__*\n_Rise and shine! Here's today's playbook:_\n",
                "☀️ *__Good Morning, Champion!__*\n_Coffee brewed, goals aligned. Here's what we've got today:_\n",
                "🌅 *__Ayva's Morning Kickstart__*\n_New day, new wins. Here's what's on today's agenda:_\n",
                "☀️ *__Morning Briefing with Ayva__*\n_Let's take a look at what we're tackling today:_\n"
            )
            return getNextFromDeck(context, "briefing_morning", morningBriefings)
        }

        if (hour < 17) {
            val midDayBriefings = listOf(
                "☀️ *__Midday Check-In with Ayva__*\n_Halfway through the day! Here's the current pulse:_\n",
                "⚡ *__Ayva's Afternoon Reality Check__*\n_Quick glance at where our momentum stands:_\n",
                "☀️ *__Midday Status Report__*\n_Here's how today is shaping up so far:_\n",
                "🌤️ *__Afternoon Progress Check with Ayva__*\n_Stay focused! Here's what's still waiting for you:_\n"
            )
            return getNextFromDeck(context, "briefing_afternoon", midDayBriefings)
        }

        val eveningBriefings = listOf(
            "🌙 *__Evening Debrief with Ayva__*\n_Wrapping up the day! Here's how we finished:_\n",
            "🌙 *__Nightly Wrap-Up with Ayva__*\n_Time to review today's scorecard:_\n",
            "✨ *__Ayva's Evening Reflection__*\n_Here's what got crushed today and what carried over:_\n",
            "🌙 *__End of Day Check-In__*\n_Let's see where the day landed:_\n"
        )
        return getNextFromDeck(context, "briefing_evening", eveningBriefings)
    }

    fun getReschedulePrompt(context: Context): String {
        val prompts = listOf(
            "_Wanna reschedule any of these, or are we tackling them head-on?_",
            "_Any of these need to be shifted, or are you locked and loaded?_",
            "_Need to push anything to later, or are we executing as planned? 😉_",
            "_Want to adjust any due dates before we dive in?_",
            "_Any tasks need a rain check, or are we ready to crush them? 🎯_"
        )
        return getNextFromDeck(context, "reschedule_prompt", prompts)
    }

    fun getEmptyDayMessage(context: Context): String {
        val emptyPool = listOf(
            "🎉 *Look at you, all clear! No pending tasks in sight.*",
            "✨ *Clean slate! Zero tasks on your plate for today. Enjoy the breathing room!*",
            "🎉 *Inbox zero, task zero! You're officially caught up.*",
            "🚀 *Radar is completely clear! What a glorious sight.*",
            "🌟 *Nothing on today's hit list. Feel free to relax or add a new ambition!*"
        )
        return getNextFromDeck(context, "empty_day", emptyPool)
    }

    // =========================================================================
    // 4. PRIORITY RADAR & FOCUS COMMANDS
    // =========================================================================
    fun getPriorityEmptyResponse(context: Context): String {
        val noPriorityPool = listOf(
            "No high-priority fires to put out right now! 🔥 (Queue is clear)",
            "All quiet on the urgent front. No high-priority tasks lurking! ✨",
            "Zero emergencies! Your priority queue is completely clean.",
            "No critical tasks right now. Smooth sailing! ⛵"
        )
        return getNextFromDeck(context, "priority_empty", noPriorityPool)
    }

    fun getLockCommandResponse(context: Context, durationMin: Int): String {
        val lockPool = listOf(
            "🔒 *Focus Lock Engaged for ${durationMin}m!*\n_Distractions are locked in the vault. Go be unstoppable! 🚀_",
            "🛡️ *Shields Up! ${durationMin}m Deep Work Mode Active.*\n_No notifications, no excuses. Time to lock in! ⚡_",
            "🎯 *${durationMin} Minutes of Pure Focus Begins Now!*\n_Tune out the world and build your empire._",
            "🔒 *Ayva's Focus Guard Activated (${durationMin}m).*\n_Put your head down and show them how it's done! ✨_"
        )
        return getNextFromDeck(context, "lock_command", lockPool)
    }

    fun getClearChatIntro(context: Context): String {
        val intros = listOf(
            "💬 *__Ayva__* ✨\n_Your witty focus companion is on deck._\n_Type a task below to lock it in, or use `/` for commands._",
            "💬 *__Ayva's Command Center__* ⚡\n_Clean slate! I'm ready for your tasks, drills, or schedule adjustments._\n_Type away or type `/` for shortcuts._",
            "💬 *__Ayva__* 🎯\n_Fresh chat, clear mind. What are we conquering next?_\n_Add a task or run `/help` anytime._"
        )
        return getNextFromDeck(context, "clear_chat_intro", intros)
    }

    fun getHelloWelcomeMessage(context: Context): String {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour < 12 -> "Good morning!"
            hour < 17 -> "Good afternoon!"
            else -> "Good evening!"
        }
        val helloPool = listOf(
            "👋 *Hey there! I'm Ayva, your personal focus companion.* ✨\n\n$timeGreeting Ready to conquer your day? Drop a task below or let me know what we're tackling first!",
            "✨ *Ayva here! Fresh chat, clear mind, zero excuses.* 🚀\n\n$timeGreeting I'm locked in and ready to help you stay on track. What's on your radar today?",
            "👋 *Hello! I'm Ayva — your witty partner in anti-procrastination.* 😉\n\n$timeGreeting Clean slate mode activated. What are we making happen first?",
            "🌟 *Hey! Ayva is on deck and ready for action.* ⚡\n\n$timeGreeting Whether you've got big goals or quick errands, I've got your back. What's top of mind?",
            "👋 *Welcome! I'm Ayva.* ✨\n\n$timeGreeting Let's turn intentions into achievements today. What are we working on first? 🚀"
        )
        return getNextFromDeck(context, "hello_welcome", helloPool)
    }

    // =========================================================================
    // 5. APTITUDE & MATH DRILLS
    // =========================================================================
    fun getDrillFastCorrectPraise(context: Context): String {
        val praises = listOf(
            "⚡ Lightning speed! Big brain energy right there.",
            "🎯 Boom! Exact hit, zero hesitation.",
            "🔥 In the zone! Fast and flawless.",
            "🧠 Computational wizardry detected!",
            "✨ Pure precision! You didn't even flinch."
        )
        return getNextFromDeck(context, "drill_praise", praises)
    }

    fun getDrillMissComfort(context: Context): String {
        val comforts = listOf(
            "Oof, close one! Shake it off, next one is yours.",
            "Minor slip! Keep the rhythm going, you've got this.",
            "Almost had it! Quick breath, next question incoming.",
            "No sweat! Even calculators make rounding errors. Onward! 🚀"
        )
        return getNextFromDeck(context, "drill_comfort", comforts)
    }

    fun getDrillSummaryPraise(context: Context, scorePct: Int): String {
        return when {
            scorePct == 100 -> {
                val perfect = listOf(
                    "🏆 *PERFECT SCORE!* Flawless mental agility. You're operating at peak neural frequency! 🧠⚡",
                    "🌟 *100% ACCURACY!* Absolutely unstoppable arithmetic speed. Take a bow! 👑",
                    "🔥 *FLAWLESS DRILL!* 10 out of 10. Your cognitive sharpness is elite today."
                )
                getNextFromDeck(context, "drill_perfect", perfect)
            }
            scorePct >= 80 -> {
                val great = listOf(
                    "⚡ *Fantastic Drill Performance!* Sharp instincts and high focus. Solid mental workout!",
                    "🎯 *Great Job!* You're locked in and calculating with confidence.",
                    "✨ *Strong Session!* Fast reflexes and solid accuracy. Keep that momentum!"
                )
                getNextFromDeck(context, "drill_great", great)
            }
            else -> {
                val good = listOf(
                    "💪 *Workout Complete!* Neural gears have been warmed up. Ready to focus on big tasks!",
                    "🌱 *Solid Effort!* Brain trained and warmed up. Consistency is where the magic happens.",
                    "⚡ *Drill Done!* Mental cobwebs cleared. Let's channel that energy into today's work!"
                )
                getNextFromDeck(context, "drill_good", good)
            }
        }
    }

    // =========================================================================
    // 6. CONTEXTUAL FOCUS ADVICE & MINDFUL COACHING
    // =========================================================================
    fun getContextualFocusAdvice(context: Context, totalScreenTimeMins: Int, pendingTasksCount: Int, overdueCount: Int): String {
        return when {
            overdueCount > 0 -> {
                val overduePool = listOf(
                    "⚠️ *Triage Mode*: You have $overdueCount overdue task(s). Procrastination creates phantom mental weight. Pick just *one* and knock it out in the next 15 minutes! ⚡",
                    "🎯 *Laser Focus Strategy*: Don't look at all your overdue items at once. Focus solely on the single highest priority task right now. Single-tasking is your superpower.",
                    "⚡ *Momentum Builder*: High friction right now? Do the easiest 2-minute part of an overdue task. Action precedes motivation, always."
                )
                getNextFromDeck(context, "advice_overdue", overduePool)
            }
            totalScreenTimeMins > 180 -> {
                val screenPool = listOf(
                    "🌿 *Eye & Brain Break*: You've logged over ${totalScreenTimeMins / 60}h of screen time today. Look away at an object 20 feet away for 20 seconds, roll your shoulders, and drink a glass of water. 💧",
                    "🧠 *Neural Recharge*: Prolonged screen exposure causes cognitive fatigue. Take a 5-minute offline stroll or try the 4-7-8 breathing exercise in the chat before resuming work.",
                    "⚡ *Focus Hygiene*: Digital exhaustion is real. If you're switching apps compulsively, step away from the device for 10 minutes to reset your dopamine baseline."
                )
                getNextFromDeck(context, "advice_screentime", screenPool)
            }
            pendingTasksCount == 0 -> {
                val clearPool = listOf(
                    "🎉 *Zen State*: All clear! You've achieved task nirvana for now. Take a well-deserved breather or do a quick mental arithmetic drill to keep your mind sharp.",
                    "✨ *Recharge Time*: Clean radar! Use this open space to reflect, hydrate, or plan tomorrow with zero stress.",
                    "🚀 *Victory Lap*: No fires, no pending tasks. Enjoy your flow state or relax guilt-free!"
                )
                getNextFromDeck(context, "advice_clear", clearPool)
            }
            else -> {
                val focusPool = listOf(
                    "⚡ *The 5-Minute Rule*: Tell yourself you'll only work on your task for five minutes. 80% of the time, once inertia is broken, you'll naturally keep going! 🎯",
                    "🧘 *Single-Tab Focus*: Close background distractions. Multitasking drops cognitive output by 40%. One objective at a time.",
                    "🌊 *Find Your Flow*: Pick your top task, silence alerts, and work uninterrupted. Deep work always beats long hours.",
                    "💡 *Energy Management*: Prioritize your hardest creative task during your peak energy hours. Guard your attention like gold."
                )
                getNextFromDeck(context, "advice_general", focusPool)
            }
        }
    }

    fun getBreathingGuidance(context: Context): String {
        val breathingPool = listOf(
            "🫁 *4-7-8 Box Breathing Protocol*:\n\n1. Inhale deeply through your nose for **4 seconds** 🌬️\n2. Hold your breath gently for **7 seconds** ⏸️\n3. Exhale slowly through your mouth for **8 seconds** 💨\n\n_Repeat 3 times to downregulate your nervous system and instantly regain mental clarity._",
            "🌿 *Resonance Breathing (6 Breaths/Min)*:\n\n• Inhale smoothly for **5 seconds** 🍃\n• Exhale smoothly for **5 seconds** 🍃\n\n_Doing this for just 60 seconds lowers cortisol and centers your focus immediately._",
            "⚡ *Physiological Sigh (Rapid Reset)*:\n\n1. Take two quick sniffs in through your nose (one deep, followed immediately by a sharp top-off sniff)\n2. Long, slow sigh out through your mouth\n\n_This expands deflated alveoli in your lungs and drops heart rate within seconds._"
        )
        return getNextFromDeck(context, "breathing_pool", breathingPool)
    }

    // =========================================================================
    // 7. COMPREHENSIVE FOCUS STATUS BRIEFING
    // =========================================================================
    fun getFocusStatusBriefing(
        context: Context,
        totalScreenTimeMins: Int,
        topAppName: String?,
        topAppMins: Int,
        activeRoutineName: String?,
        routineEndsAt: String?,
        restrictedAppsCount: Int,
        isStrictMode: Boolean,
        streak: Int,
        isVacation: Boolean,
        pendingCount: Int,
        completedTodayCount: Int,
        overdueCount: Int
    ): String {
        val sb = StringBuilder()
        sb.append("⚡ *__Ayva's Focus Posture Report__*\n\n")

        // 1. Routine status
        if (activeRoutineName != null) {
            val endStr = if (routineEndsAt != null) " (Active until $routineEndsAt)" else ""
            sb.append("🛡️ **Active Routine**: **$activeRoutineName**$endStr\n")
        } else {
            sb.append("🛡️ **Routine**: No active schedule right now\n")
        }

        // 2. App Armor
        val modeStr = if (isStrictMode) "Strict Mode" else "Soft Mode"
        if (restrictedAppsCount > 0) {
            sb.append("🔒 **App Armor**: $restrictedAppsCount apps restricted [$modeStr]\n")
        } else {
            sb.append("🔓 **App Armor**: Unrestricted (No apps locked)\n")
        }

        // 3. Streak & Vacation
        if (isVacation) {
            sb.append("🏖️ **Streak**: $streak days (Frozen in Vacation Mode ❄️)\n")
        } else if (streak > 0) {
            sb.append("🔥 **Streak**: $streak days active momentum\n")
        } else {
            sb.append("🌱 **Streak**: Ready to build fresh momentum today\n")
        }

        // 4. Tasks
        val overdueWarning = if (overdueCount > 0) " (⚠️ $overdueCount overdue!)" else ""
        sb.append("📋 **Tasks**: $completedTodayCount completed today, $pendingCount pending$overdueWarning\n")

        // 5. Screen Time
        val hrs = totalScreenTimeMins / 60
        val mins = totalScreenTimeMins % 60
        val timeStr = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
        val topAppStr = if (topAppName != null && topAppMins > 0) {
            val tHrs = topAppMins / 60
            val tMins = topAppMins % 60
            val tStr = if (tHrs > 0) "${tHrs}h ${tMins}m" else "${tMins}m"
            " • Top: $topAppName ($tStr)"
        } else ""
        sb.append("📱 **Screen Time**: $timeStr$topAppStr\n\n")

        // 6. Ayva Verdict
        val verdict = when {
            overdueCount > 0 -> "💡 _Verdict: Knock out your overdue item first to eliminate subconscious cognitive drag._"
            activeRoutineName != null -> "💡 _Verdict: You're in protected focus time. Keep digital distractions at zero!_"
            restrictedAppsCount == 0 && totalScreenTimeMins > 120 -> "💡 _Verdict: High screen time with no app blocks. Consider locking your top distractor!_"
            pendingCount == 0 -> "💡 _Verdict: Pristine task radar. Great momentum today!_"
            else -> "💡 _Verdict: Pick your single highest-priority task and execute a 20-minute focus sprint._"
        }
        sb.append(verdict)
        return sb.toString()
    }

    fun getTaskCompletedPraise(context: Context, taskTitle: String, remainingCount: Int): String {
        val remainingStr = if (remainingCount == 0) {
            "Radar completely clear! 🎉"
        } else {
            "$remainingCount task${if (remainingCount > 1) "s" else ""} remaining."
        }
        val pool = listOf(
            "✅ *Crushed it!* \"$taskTitle\" is done. $remainingStr ⚡",
            "🎯 *Boom!* Marked \"$taskTitle\" as complete. $remainingStr Keep that momentum rollin'!",
            "🔥 *Task Conquered!* \"$taskTitle\" checked off. $remainingStr",
            "⚡ *Zero Friction!* Completed \"$taskTitle\". $remainingStr One step closer to total victory."
        )
        return getNextFromDeck(context, "task_complete_praise", pool)
    }
}

