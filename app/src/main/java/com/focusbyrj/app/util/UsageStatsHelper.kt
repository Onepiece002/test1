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

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar

data class AppUsageData(
    val appName: String,
    val packageName: String,
    val timeInForegroundMs: Long
)

object UsageStatsHelper {
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getTodayUsageStats(context: Context): List<AppUsageData> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStatsList = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val pm = context.packageManager
        val resultMap = mutableMapOf<String, AppUsageData>()

        for (usage in usageStatsList) {
            if (usage.totalTimeInForeground > 0) {
                val appName = try {
                    val appInfo = pm.getApplicationInfo(usage.packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    usage.packageName
                }
                val existing = resultMap[usage.packageName]
                val totalTime = (existing?.timeInForegroundMs ?: 0L) + usage.totalTimeInForeground
                resultMap[usage.packageName] = AppUsageData(
                    appName = appName,
                    packageName = usage.packageName,
                    timeInForegroundMs = totalTime
                )
            }
        }

        return resultMap.values.sortedByDescending { it.timeInForegroundMs }
    }

    fun getLast30DaysUsageStats(context: Context): Map<Int, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -30)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        ) ?: emptyList()

        val dayUsageMap = mutableMapOf<Int, Long>()
        val cal = Calendar.getInstance()

        for (usage in stats) {
            cal.timeInMillis = usage.firstTimeStamp
            val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
            val current = dayUsageMap[dayOfYear] ?: 0L
            dayUsageMap[dayOfYear] = current + usage.totalTimeInForeground
        }

        return dayUsageMap
    }
}
