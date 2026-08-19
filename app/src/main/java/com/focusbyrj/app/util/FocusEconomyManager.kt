package com.focusbyrj.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

data class UserProfile(
    val name: String,
    val xp: Int,
    val gold: Int,
    val level: Int,
    val maxLevel: Int,
    val maxGold: Int,
    val avatarTier: Int,
    val selectedAvatar: String,
    val purchasedAvatars: Set<String>,
    val currentStreak: Int,
    val longestStreak: Int,
    val pendingXp: Int = 0,
    val pendingGold: Int = 0,
    val lifetimeFocusMins: Int = 0,
    val lifetimeResists: Int = 0
)

object FocusEconomyManager {
    private const val PREFS_NAME = "focus_economy_prefs"
    
    private val _profileFlow = MutableStateFlow(
        UserProfile("Focus Warrior", 0, 0, 1, 1, 0, 1, "tier_1", emptySet(), 0, 0, 0, 0, 0, 0)
    )
    val profileFlow: StateFlow<UserProfile> = _profileFlow.asStateFlow()

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadProfile()
    }

    private fun loadProfile() {
        prefs?.let {
            val name = it.getString("user_name", "Focus Warrior") ?: "Focus Warrior"
            val xp = it.getInt("xp", 0)
            val gold = it.getInt("gold", 0)
            val level = calculateLevel(xp)
            val maxLevel = it.getInt("max_level", level)
            
            
            if (level > maxLevel) {
                it.edit().putInt("max_level", level).apply()
            }
            
            val finalMaxLevel = max(level, maxLevel)
            val maxGold = max(gold, it.getInt("max_gold", 0))
            if (gold > it.getInt("max_gold", 0)) {
                it.edit().putInt("max_gold", maxGold).apply()
            }
            
            val purchased = it.getStringSet("purchased_avatars", emptySet()) ?: emptySet()
            val unlockedTier = calculateUnlockedAvatarTier(maxGold)
            val selectedAvatar = it.getString("selected_avatar", "tier_$unlockedTier") ?: "tier_$unlockedTier"
            
            _profileFlow.value = UserProfile(
                name = name,
                xp = xp,
                gold = gold,
                level = level,
                maxLevel = finalMaxLevel,
                maxGold = maxGold,
                avatarTier = unlockedTier,
                selectedAvatar = selectedAvatar,
                purchasedAvatars = purchased,
                currentStreak = it.getInt("current_streak", 0),
                longestStreak = it.getInt("longest_streak", 0),
                pendingXp = it.getInt("pending_xp", 0),
                pendingGold = it.getInt("pending_gold", 0),
                lifetimeFocusMins = it.getInt("lifetime_focus_mins", 0),
                lifetimeResists = it.getInt("lifetime_resists", 0)
            )
        }
    }

    fun updateName(newName: String) {
        prefs?.edit()?.putString("user_name", newName)?.apply()
        loadProfile()
    }


    fun purchaseAvatar(avatarId: String, cost: Int): Boolean {
        prefs?.let { p ->
            val currentProfile = _profileFlow.value
            if (currentProfile.gold >= cost && !currentProfile.purchasedAvatars.contains(avatarId)) {
                val newPurchased = currentProfile.purchasedAvatars.toMutableSet().apply { add(avatarId) }
                p.edit()
                    .putInt("gold", currentProfile.gold - cost)
                    .putStringSet("purchased_avatars", newPurchased)
                    .putString("selected_avatar", avatarId)
                    .apply()
                loadProfile()
                return true
            }
        }
        return false
    }

    fun equipAvatar(avatarId: String) {
        prefs?.edit()?.putString("selected_avatar", avatarId)?.apply()
        loadProfile()
    }

    
    // Level 1: 0 XP
    // Level 2: 100 XP
    // Level 3: 250 XP
    // Formula: (Level-1) * 100 + (Level-1)^2 * 25
    fun calculateLevel(xp: Int): Int {
        var level = 1
        while (level < 200 && requiredXpForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    fun requiredXpForLevel(level: Int): Int {
        if (level <= 1) return 0
        val baseAt100 = 99 * 100 + (99 * 99) * 25 // 254,925 XP
        val constantDeltaAfter100 = (99 * 100 + 99 * 99 * 25) - (98 * 100 + 98 * 98 * 25) // 5,025 XP
        
        return if (level <= 100) {
            (level - 1) * 100 + ((level - 1) * (level - 1)) * 25
        } else {
            baseAt100 + (level - 100) * constantDeltaAfter100
        }
    }

    
    fun getGoldMultiplier(level: Int): Float {
        return when (level) {
            in 1..10 -> 1.0f
            in 11..25 -> 1.2f
            in 26..50 -> 1.5f
            in 51..75 -> 2.0f
            in 76..100 -> 2.5f
            in 101..150 -> 3.0f
            in 151..199 -> 3.5f
            else -> 4.0f
        }
    }

    
    fun applySoftUnlockPenalty() {
        prefs?.let { p ->
            val currentProfile = _profileFlow.value
            val currentLevel = currentProfile.level
            
            val penalty = when (currentLevel) {
                in 1..5 -> 25
                in 6..15 -> 100
                in 16..30 -> (currentProfile.xp * 0.05f).toInt()
                else -> (currentProfile.xp * 0.10f).toInt()
            }
            
            var newXp = max(0, currentProfile.xp - penalty)
            
            // Floor protection: Never drop more than 5 levels from maxLevel
            val minAllowedLevel = max(1, currentProfile.maxLevel - 5)
            val minAllowedXp = requiredXpForLevel(minAllowedLevel)
            
            if (newXp < minAllowedXp) {
                newXp = minAllowedXp
            }
            
            p.edit().putInt("xp", newXp).apply()
            loadProfile()
        }
    }


    fun claimPendingRewards() {
        prefs?.let { p ->
            val pXp = _profileFlow.value.pendingXp
            val pGold = _profileFlow.value.pendingGold
            
            p.edit()
                .putInt("xp", _profileFlow.value.xp + pXp)
                .putInt("gold", _profileFlow.value.gold + pGold)
                .putInt("pending_xp", 0)
                .putInt("pending_gold", 0)
                .apply()
            loadProfile()
        }
    }

    
    fun recoverXp(goldCost: Int, xpGain: Int) {
        prefs?.let { p ->
            val currentGold = _profileFlow.value.gold
            if (currentGold >= goldCost) {
                p.edit()
                    .putInt("gold", currentGold - goldCost)
                    .putInt("xp", _profileFlow.value.xp + xpGain)
                    .apply()
                loadProfile()
            }
        }
    }

    
    fun addRewards(baseXp: Int, baseGold: Int) {
        prefs?.let { p ->
            val currentLevel = _profileFlow.value.level
            val goldMultiplier = getGoldMultiplier(currentLevel)
            
            val finalGold = (baseGold * goldMultiplier).toInt()
            
            p.edit()
                .putInt("pending_xp", _profileFlow.value.pendingXp + baseXp)
                .putInt("pending_gold", _profileFlow.value.pendingGold + finalGold)
                .apply()
            loadProfile()
        }
    }

    fun addDurationBasedRewards(totalMinsFocusedSoFar: Int) {
        prefs?.let { p ->
            val currentLevel = _profileFlow.value.level
            val goldMultiplier = getGoldMultiplier(currentLevel)
            
            // 50 XP per 5 minute block
            val baseXp = 50
            
            // Base Gold scales up exponentially to reward longer focus sessions!
            val baseGold = when(totalMinsFocusedSoFar) {
                5 -> 10
                10 -> 25
                15 -> 45
                20 -> 70
                25 -> 100
                30 -> 140
                35 -> 190
                40 -> 250
                45 -> 320
                50 -> 400
                55 -> 490
                60 -> 600
                else -> 600 + ((totalMinsFocusedSoFar - 60) / 5) * 120
            }
            
            val finalGold = (baseGold * goldMultiplier).toInt()
            
            p.edit()
                .putInt("pending_xp", _profileFlow.value.pendingXp + baseXp)
                .putInt("pending_gold", _profileFlow.value.pendingGold + finalGold)
                .apply()
            loadProfile()
        }
    }


    fun addResist() {
        prefs?.let { p ->
            val current = _profileFlow.value.lifetimeResists
            p.edit().putInt("lifetime_resists", current + 1).apply()
            loadProfile()
        }
    }
    
    fun addLifetimeFocusMins(mins: Int) {
        prefs?.let { p ->
            val current = _profileFlow.value.lifetimeFocusMins
            p.edit().putInt("lifetime_focus_mins", current + mins).apply()
            loadProfile()
        }
    }

    
    
    fun syncStreaks(current: Int, longest: Int) {
        prefs?.let { p ->
            val oldGold = _profileFlow.value.gold
            var newGold = oldGold
            
            // Give achievement rewards if we hit new milestones
            val oldLongest = _profileFlow.value.longestStreak
            if (longest > oldLongest) {
                if (longest == 3 && oldLongest < 3) newGold += 100
                if (longest == 7 && oldLongest < 7) newGold += 500
                if (longest == 30 && oldLongest < 30) newGold += 5000
            }
            
            p.edit()
                .putInt("current_streak", current)
                .putInt("longest_streak", longest)
                .putInt("gold", newGold)
                .apply()
            loadProfile()
        }
    }

    fun incrementStreak() {
        prefs?.let { p ->
            val newStreak = _profileFlow.value.currentStreak + 1
            var longest = _profileFlow.value.longestStreak
            if (newStreak > longest) {
                longest = newStreak
            }
            
            var goldBonus = 0
            when (newStreak) {
                3 -> goldBonus = 100
                7 -> goldBonus = 500
                30 -> goldBonus = 5000
            }
            
            val newGold = _profileFlow.value.gold + goldBonus
            
            p.edit()
                .putInt("current_streak", newStreak)
                .putInt("longest_streak", longest)
                .putInt("gold", newGold)
                .apply()
            loadProfile()
        }
    }
    
    fun resetStreak() {
        prefs?.edit()?.putInt("current_streak", 0)?.apply()
        loadProfile()
    }

    
    fun unlockProMax() {
        prefs?.let { p ->
            // Max Level is 50, which requires ~60,000 XP
            // Max Avatar Tier requires 50,000 Gold
            // Max Streak Achievement is 30 Days
            
            p.edit()
                .putInt("xp", 260000)
                .putInt("gold", 999999)
                .putInt("current_streak", 30)
                .putInt("longest_streak", 30)
                .apply()
            
            loadProfile()
        }
    }

    fun calculateUnlockedAvatarTier(gold: Int): Int {
        return when {
            gold >= 50000 -> 5 
            gold >= 10000 -> 4 
            gold >= 2000 -> 3 
            gold >= 500 -> 2 
            else -> 1 
        }
    }
}
