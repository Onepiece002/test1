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

package com.focusbyrj.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.focusbyrj.app.R
import com.focusbyrj.app.data.FocusDatabase
import com.focusbyrj.app.util.FocusQuotes
import com.focusbyrj.app.util.TemporaryUnlockManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object FocusExitTracker {
    @Volatile
    var lastExitedPackage: String? = null
    @Volatile
    var exitTimestamp: Long = 0L

    fun notifyExited(packageName: String?) {
        lastExitedPackage = packageName
        exitTimestamp = System.currentTimeMillis()
    }

    fun isExitSuppressed(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName != lastExitedPackage) return false
        return (System.currentTimeMillis() - exitTimestamp) < 25000L
    }

    fun onNewForegroundAppDetected(packageName: String) {
        if (packageName != lastExitedPackage && packageName.isNotBlank() && packageName != "com.android.systemui") {
            lastExitedPackage = null
            exitTimestamp = 0L
        }
    }
}

class FocusBlockerService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var db: FocusDatabase

    private var currentForegroundPackage: String? = null
    private var monitoringJob: kotlinx.coroutines.Job? = null
    private var isScreenOn = true

    private val screenReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    monitoringJob?.cancel()
                    monitoringJob = null
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (!isScreenOn || intent?.action == Intent.ACTION_USER_PRESENT) {
                        isScreenOn = true
                        startAppMonitoringLoop()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        db = (application as com.focusbyrj.app.FocusApplication).database

        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenReceiver, filter)

        startForegroundServiceNotification()
        startRoutineMonitorLoop()
        startAppMonitoringLoop()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "focus_blocker_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Guard Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors restricted apps in background"
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Focus Guard Active")
            .setContentText("Protecting your screen time and boundaries")
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startAppMonitoringLoop() {
        if (monitoringJob?.isActive == true) return
        monitoringJob = scope.launch {
            while (isActive) {
                kotlin.runCatching {
                    val currentPackage = getForegroundPackage()
                    if (!currentPackage.isNullOrBlank()) {
                        checkAndBlockApp(currentPackage)
                    }
                }
                delay(350L)
            }
        }
    }

    private var activeRoutines = mutableMapOf<String, com.focusbyrj.app.data.FocusSchedule>()
    
    private fun startRoutineMonitorLoop() {
        scope.launch {
            delay(2000L)
            while (isActive) {
                kotlin.runCatching {
                    checkRoutinesAndNotify()
                }
                delay(60000L)
            }
        }
    }

    private suspend fun checkRoutinesAndNotify() {
        val prefs = applicationContext.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val notifyEnabled = prefs.getBoolean("routine_notifications", true)
        
        val schedules = db.scheduleDao().getAllSchedulesSync()
        val calendar = java.util.Calendar.getInstance()
        val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute
        
        val currentlyActive = mutableMapOf<String, com.focusbyrj.app.data.FocusSchedule>()
        
        for (schedule in schedules) {
            val activeDays = schedule.daysOfWeek.split(",")
            if (activeDays.contains(currentDay.toString())) {
                val startTotalMinutes = schedule.startHour * 60 + schedule.startMinute
                val endTotalMinutes = schedule.endHour * 60 + schedule.endMinute
                
                val isTimeMatch = if (startTotalMinutes <= endTotalMinutes) {
                    currentTotalMinutes in startTotalMinutes..endTotalMinutes
                } else {
                    currentTotalMinutes >= startTotalMinutes || currentTotalMinutes <= endTotalMinutes
                }
                
                if (isTimeMatch) {
                    currentlyActive[schedule.id.toString()] = schedule
                    if (!activeRoutines.containsKey(schedule.id.toString())) {
                        if (notifyEnabled) {
                            sendRoutineNotification("Routine Started", "${schedule.name} is now active.")
                        }
                    }
                }
            }
        }
        
        for (activeId in activeRoutines.keys) {
            if (!currentlyActive.containsKey(activeId)) {
                if (notifyEnabled) {
                    val scheduleName = activeRoutines[activeId]?.name ?: "Routine"
                    sendRoutineNotification("Routine Ended", "$scheduleName has ended.")
                }
            }
        }
        
        activeRoutines.clear()
        activeRoutines.putAll(currentlyActive)
    }

    private fun sendRoutineNotification(title: String, message: String) {
        val channelId = "routine_alerts"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Routine Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private val homePackages = mutableSetOf<String>()
    private var lastHomePackagesCheck = 0L

    private fun refreshHomePackages() {
        val now = System.currentTimeMillis()
        if (now - lastHomePackagesCheck < 30000L && homePackages.isNotEmpty()) return
        lastHomePackagesCheck = now
        
        kotlin.runCatching {
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val list = packageManager.queryIntentActivities(homeIntent, 0)
            for (info in list) {
                info.activityInfo?.packageName?.let { homePackages.add(it) }
            }
        }
    }

    private fun isIgnoredPackage(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == applicationContext.packageName || packageName == "com.focusbyrj.app") return true
        if (packageName == "com.android.settings" || packageName == "com.android.systemui" || packageName == "android") return true
        
        refreshHomePackages()
        if (homePackages.contains(packageName)) return true

        val lower = packageName.lowercase()
        return lower.contains("launcher") ||
                lower.contains("quickstep") ||
                lower.contains("trebuchet") ||
                lower.contains("nexuslauncher") ||
                lower.contains("miui.home") ||
                lower.contains("sec.android.app.launcher") ||
                lower.contains("huawei.android.launcher") ||
                lower.contains("oppo.launcher") ||
                lower.contains("vivo.launcher") ||
                lower.contains("transsion.home") ||
                lower.contains("motorola.launcher") ||
                lower.contains("oneplus.launcher")
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()

        val events = usm.queryEvents(now - 1000 * 10, now)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }

        if (latestPackage != null) {
            if (latestPackage == FocusExitTracker.lastExitedPackage) {
                // Ignore ghost resume events that happen exactly when the overlay is removed
                if (latestTime <= FocusExitTracker.exitTimestamp + 2000L) {
                    return null
                } else {
                    FocusExitTracker.onNewForegroundAppDetected(latestPackage)
                }
            } else {
                FocusExitTracker.onNewForegroundAppDetected(latestPackage)
            }
            currentForegroundPackage = latestPackage
            return latestPackage
        }

        if (FocusExitTracker.isExitSuppressed(currentForegroundPackage)) {
            // User just exited to home, clear the cached package so we don't get stuck
            currentForegroundPackage = null
            return null
        }
        return currentForegroundPackage
    }

    private suspend fun checkAndBlockApp(packageName: String) {
        if (isIgnoredPackage(packageName)) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                BlockOverlayManager.hideOverlay(this)
            }
            return
        }

        if (FocusExitTracker.isExitSuppressed(packageName)) {
            return
        }

        if (TemporaryUnlockManager.isUnlocked(applicationContext, packageName)) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                BlockOverlayManager.hideOverlay(this)
            }
            return
        }

        var shouldBlock = false
        var blockQuote = ""
        var blockMode = "HARD"

        val restriction = db.appRestrictionDao().getRestriction(packageName)
        if (restriction != null && restriction.isRestricted) {
            when (restriction.restrictionMode) {
                "TIME_LIMIT" -> {
                    if (restriction.timeLimitMinutes > 0) {
                        val usageMins = com.focusbyrj.app.util.UsageStatsHelper.getTodayUsageMinutesForPackage(applicationContext, packageName)
                        if (usageMins >= restriction.timeLimitMinutes) {
                            shouldBlock = true
                            blockQuote = FocusQuotes.getQuoteOrDefault(restriction.customQuote)
                            blockMode = restriction.mode
                        }
                    }
                }
                "CLICK_LIMIT" -> {
                    if (restriction.clickLimitCount > 0) {
                        val launches = com.focusbyrj.app.util.UsageStatsHelper.getTodayLaunchCountForPackage(applicationContext, packageName)
                        if (launches > restriction.clickLimitCount) {
                            shouldBlock = true
                            blockQuote = FocusQuotes.getQuoteOrDefault(restriction.customQuote)
                            blockMode = restriction.mode
                        }
                    }
                }
                else -> {
                    shouldBlock = true
                    blockQuote = FocusQuotes.getQuoteOrDefault(restriction.customQuote)
                    blockMode = restriction.mode
                }
            }
        }

        if (!shouldBlock) {
            for (schedule in activeRoutines.values) {
                if (schedule.appsToBlock.split(",").contains(packageName)) {
                    when (schedule.restrictionMode) {
                        "TIME_LIMIT" -> {
                            if (schedule.timeLimitMinutes > 0) {
                                val usageMins = com.focusbyrj.app.util.UsageStatsHelper.getTodayUsageMinutesForPackage(applicationContext, packageName)
                                if (usageMins >= schedule.timeLimitMinutes) {
                                    shouldBlock = true
                                    blockQuote = "Routine '${schedule.name}' time limit exceeded."
                                    blockMode = schedule.mode
                                    break
                                }
                            }
                        }
                        "CLICK_LIMIT" -> {
                            if (schedule.clickLimitCount > 0) {
                                val launches = com.focusbyrj.app.util.UsageStatsHelper.getTodayLaunchCountForPackage(applicationContext, packageName)
                                if (launches > schedule.clickLimitCount) {
                                    shouldBlock = true
                                    blockQuote = "Routine '${schedule.name}' open limit exceeded."
                                    blockMode = schedule.mode
                                    break
                                }
                            }
                        }
                        else -> {
                            shouldBlock = true
                            blockQuote = "Routine '${schedule.name}' is active."
                            blockMode = schedule.mode
                            break
                        }
                    }
                }
            }
        }

        val prefs = applicationContext.getSharedPreferences("focus_prefs", Context.MODE_PRIVATE)
        val isSessionActive = prefs.getBoolean("isSessionActive", false)
        if (!shouldBlock && isSessionActive && restriction != null) {
            shouldBlock = true
            blockQuote = FocusQuotes.getQuoteOrDefault(restriction.customQuote)
            blockMode = restriction.mode
        }

        if (shouldBlock) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                BlockOverlayManager.showBlockScreen(this@FocusBlockerService, packageName, blockQuote, blockMode)
            }
        } else if (BlockOverlayManager.isShowing) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                BlockOverlayManager.hideOverlay(this)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        kotlin.runCatching { unregisterReceiver(screenReceiver) }
        job.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            kotlin.runCatching {
                val intent = Intent(context, FocusBlockerService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }
    }
}
